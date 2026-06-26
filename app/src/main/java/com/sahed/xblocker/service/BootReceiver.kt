package com.sahed.xblocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.sahed.xblocker.data.datastore.AppPreferences
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        CoroutineScope(Dispatchers.IO).launch {
            val autoStart = preferences.isAutoStartBoot.first()
            val wasActive = preferences.isVpnActive.first()
            if (autoStart && wasActive) {
                Log.i("BootReceiver", "Restarting VPN after boot")
                val vpnIntent = Intent(context, XBlockerVpnService::class.java).apply {
                    action = XBlockerVpnService.ACTION_START
                }
                context.startForegroundService(vpnIntent)
            }
        }
    }
}
