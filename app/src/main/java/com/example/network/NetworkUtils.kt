package com.example.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    /**
     * Finds the most likely local IPv4 address of this device (Hotspot or Wi-Fi).
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First look for hotspot interfaces (wlan, ap, rndis)
            for (intf in interfaces) {
                if (intf.isUp && !intf.isLoopback) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress ?: ""
                            // Android hotspot typically assigns 192.168.43.1 or 192.168.44.1
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.43.1" // Fallback standard Android hotspot gateway
    }

    /**
     * Returns broadcast address for UDP announcement.
     */
    fun getBroadcastAddress(): InetAddress {
        try {
            return InetAddress.getByName("255.255.255.255")
        } catch (_: Exception) {}
        return InetAddress.getByName("192.168.43.255")
    }
}
