package com.sahed.xblocker.service

/**
 * Lightweight custom DNS UDP packet parser.
 * Parses only the question section (domain name) from a DNS query packet.
 * Follows RFC 1035 section 4.1.
 */
object DnsPacketParser {

    private const val DNS_HEADER_SIZE = 12
    private const val DNS_QR_MASK = 0x80.toByte()

    /**
     * Extracts the queried domain name from a raw DNS UDP payload.
     * Returns null if the packet is invalid or is a response (not a query).
     */
    fun extractDomain(payload: ByteArray): String? {
        if (payload.size < DNS_HEADER_SIZE + 1) return null

        // Flags at bytes 2-3: QR bit (bit 15) == 0 means query
        val flags = payload[2]
        if (flags.toInt() and 0x80 != 0) return null // It's a response, skip

        // QDCOUNT at bytes 4-5: number of questions
        val qdCount = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
        if (qdCount == 0) return null

        // Parse first question starting at byte 12
        return try {
            parseDomainName(payload, DNS_HEADER_SIZE)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDomainName(data: ByteArray, startOffset: Int): String {
        val labels = mutableListOf<String>()
        var offset = startOffset

        while (offset < data.size) {
            val labelLength = data[offset].toInt() and 0xFF
            if (labelLength == 0) break // Root label — end of domain name

            // Check for pointer (compression — starts with 0xC0)
            if (labelLength and 0xC0 == 0xC0) {
                if (offset + 1 >= data.size) break
                val pointer = ((labelLength and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
                labels.add(parseDomainName(data, pointer))
                break
            }

            offset++
            if (offset + labelLength > data.size) break
            labels.add(String(data, offset, labelLength, Charsets.US_ASCII))
            offset += labelLength
        }

        return labels.joinToString(".")
    }

    /**
     * Builds a DNS NXDOMAIN (no such domain) response for the given query packet.
     * Returns 0.0.0.0 style block response.
     */
    fun buildBlockResponse(queryPacket: ByteArray): ByteArray {
        val response = queryPacket.copyOf()
        // Set QR bit (response), AA bit, RCODE=3 (NXDOMAIN)
        response[2] = (response[2].toInt() or 0x81).toByte() // QR + AA
        response[3] = (response[3].toInt() or 0x83).toByte() // RA + NXDOMAIN
        return response
    }

    /**
     * Builds a DNS A record response returning 0.0.0.0 for the queried domain.
     */
    fun buildNullIpResponse(queryPacket: ByteArray): ByteArray {
        val response = queryPacket.copyOf()
        // Set QR=1 (response), AA=1, ANCOUNT=1
        response[2] = (response[2].toInt() or 0x81).toByte()
        response[3] = (response[3].toInt() and 0x02.inv()).toByte() // Clear TC
        // ANCOUNT = 1
        response[6] = 0x00
        response[7] = 0x01

        // Append answer: pointer to question name + A type + IN class + TTL + 4-byte 0.0.0.0
        val answer = byteArrayOf(
            0xC0.toByte(), 0x0C.toByte(), // name pointer to offset 12 (question)
            0x00, 0x01,                    // TYPE A
            0x00, 0x01,                    // CLASS IN
            0x00, 0x00, 0x00, 0x01,       // TTL = 1 second
            0x00, 0x04,                    // RDLENGTH = 4
            0x00, 0x00, 0x00, 0x00         // RDATA = 0.0.0.0
        )
        return response + answer
    }
}
