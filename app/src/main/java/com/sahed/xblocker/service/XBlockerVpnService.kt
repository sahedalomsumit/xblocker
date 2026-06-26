package com.sahed.xblocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sahed.xblocker.R
import com.sahed.xblocker.data.datastore.AppPreferences
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class XBlockerVpnService : VpnService() {

    @Inject lateinit var blocklistRepo: BlocklistRepository
    @Inject lateinit var preferences: AppPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val blockEngine = DnsBlockEngine()

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null

    companion object {
        private const val TAG = "XBlockerVpn"
        private const val CHANNEL_ID = "xblocker_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.sahed.xblocker.START_VPN"
        const val ACTION_STOP = "com.sahed.xblocker.STOP_VPN"
        private const val VIRTUAL_DNS = "10.0.0.1"
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val MTU = 1500
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startVpn()
                START_STICKY
            }
        }
    }

    private fun startVpn() {
        vpnJob?.cancel()
        vpnJob = serviceScope.launch {
            try {
                // Load blocklist
                loadBlocklist()

                // Build VPN interface
                vpnInterface?.close()
                val upstreamDns = preferences.upstreamDns.first()
                vpnInterface = Builder()
                    .setSession("XBlocker")
                    .addAddress(VPN_ADDRESS, 32)
                    .addDnsServer(VIRTUAL_DNS)
                    .addRoute("0.0.0.0", 0)
                    .setMtu(MTU)
                    .setBlocking(true)
                    .establish()

                preferences.setVpnActive(true)

                val fd = vpnInterface ?: return@launch
                val inputStream = FileInputStream(fd.fileDescriptor)
                val outputStream = FileOutputStream(fd.fileDescriptor)

                val buffer = ByteBuffer.allocate(MTU)

                // Upstream DNS socket
                val upstreamSocket = DatagramSocket()
                protect(upstreamSocket)

                Log.i(TAG, "VPN tunnel started — blocklist: ${blockEngine.size()} entries")

                while (!Thread.currentThread().isInterrupted) {
                    buffer.clear()
                    val length = inputStream.read(buffer.array())
                    if (length <= 0) continue

                    val packet = buffer.array().copyOf(length)
                    val response = processPacket(packet, upstreamSocket, upstreamDns)
                    if (response != null) {
                        outputStream.write(response)
                    }
                }

                upstreamSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "VPN error: ${e.message}", e)
                preferences.setVpnActive(false)
            }
        }
    }

    private suspend fun loadBlocklist() {
        val customDomains = blocklistRepo.getAllActiveDomains()
        val isDefaultEnabled = preferences.isDefaultListEnabled.first()

        val allDomains = mutableListOf<String>()
        allDomains.addAll(customDomains)

        if (isDefaultEnabled) {
            try {
                assets.open("blocklist.txt").bufferedReader().use { reader ->
                    allDomains.addAll(reader.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load default blocklist: ${e.message}")
            }
        }

        blockEngine.loadDomains(allDomains)
        Log.i(TAG, "Loaded ${blockEngine.size()} domains into block engine")
    }

    /**
     * Processes an IP packet. If it contains a DNS query for a blocked domain,
     * returns a block response. Otherwise forwards to upstream DNS and relays the answer.
     */
    private fun processPacket(packet: ByteArray, socket: DatagramSocket, upstreamDns: String): ByteArray? {
        // Check for UDP (protocol byte at IP offset 9 = 17)
        if (packet.size < 28) return null
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // Not UDP

        // Source/destination ports at IP header end (20 bytes) + UDP header (8 bytes)
        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        if (packet.size < ipHeaderLength + 8) return null

        val destPort = ((packet[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                (packet[ipHeaderLength + 3].toInt() and 0xFF)

        // Only handle DNS (port 53)
        if (destPort != 53) return null

        val udpPayloadOffset = ipHeaderLength + 8
        if (packet.size <= udpPayloadOffset) return null
        val dnsPayload = packet.copyOfRange(udpPayloadOffset, packet.size)

        val domain = DnsPacketParser.extractDomain(dnsPayload) ?: return null

        return if (blockEngine.shouldBlock(domain)) {
            Log.d(TAG, "BLOCKED: $domain")
            serviceScope.launch { blocklistRepo.incrementBlockedCount(domain); preferences.incrementBlockedToday() }
            val blockResp = DnsPacketParser.buildNullIpResponse(dnsPayload)
            buildIpUdpPacket(packet, blockResp, ipHeaderLength)
        } else {
            // Forward to upstream DNS
            forwardToUpstream(packet, dnsPayload, socket, upstreamDns, ipHeaderLength)
        }
    }

    private fun forwardToUpstream(
        originalPacket: ByteArray,
        dnsPayload: ByteArray,
        socket: DatagramSocket,
        upstreamDns: String,
        ipHeaderLength: Int
    ): ByteArray? {
        return try {
            val upstream = InetAddress.getByName(upstreamDns)
            val request = DatagramPacket(dnsPayload, dnsPayload.size, upstream, 53)
            socket.soTimeout = 3000
            socket.send(request)
            val responseBuffer = ByteArray(4096)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            val responseData = responseBuffer.copyOf(responsePacket.length)
            buildIpUdpPacket(originalPacket, responseData, ipHeaderLength)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Wraps a DNS payload back into an IP+UDP packet addressed back to the original sender.
     */
    private fun buildIpUdpPacket(originalPacket: ByteArray, dnsResponse: ByteArray, ipHeaderLength: Int): ByteArray {
        val udpLength = 8 + dnsResponse.size
        val totalLength = ipHeaderLength + udpLength
        val result = ByteArray(totalLength)

        // Copy IP header, swap src/dst
        System.arraycopy(originalPacket, 0, result, 0, ipHeaderLength)
        // Swap source and destination IP
        System.arraycopy(originalPacket, 12, result, 16, 4)
        System.arraycopy(originalPacket, 16, result, 12, 4)
        // Update total length
        result[2] = (totalLength shr 8).toByte()
        result[3] = (totalLength and 0xFF).toByte()
        // Protocol = UDP
        result[9] = 17

        // UDP header: swap src/dst port
        result[ipHeaderLength] = originalPacket[ipHeaderLength + 2]
        result[ipHeaderLength + 1] = originalPacket[ipHeaderLength + 3]
        result[ipHeaderLength + 2] = originalPacket[ipHeaderLength]
        result[ipHeaderLength + 3] = originalPacket[ipHeaderLength + 1]
        result[ipHeaderLength + 4] = (udpLength shr 8).toByte()
        result[ipHeaderLength + 5] = (udpLength and 0xFF).toByte()
        result[ipHeaderLength + 6] = 0 // Checksum — zero (skipped)
        result[ipHeaderLength + 7] = 0

        // DNS payload
        System.arraycopy(dnsResponse, 0, result, ipHeaderLength + 8, dnsResponse.size)

        // Recalculate IP checksum
        result[10] = 0
        result[11] = 0
        val checksum = ipChecksum(result, ipHeaderLength)
        result[10] = (checksum shr 8).toByte()
        result[11] = (checksum and 0xFF).toByte()

        return result
    }

    private fun ipChecksum(data: ByteArray, headerLength: Int): Int {
        var sum = 0
        var i = 0
        while (i < headerLength - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum shr 16 > 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    fun reloadBlocklist() {
        serviceScope.launch { loadBlocklist() }
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        serviceScope.launch { preferences.setVpnActive(false) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        vpnInterface?.close()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "XBlocker VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows VPN active status"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, XBlockerVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("XBlocker Active")
            .setContentText("Adult content is being blocked")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .build()
    }
}
