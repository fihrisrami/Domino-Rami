package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

const val NSD_SERVICE_TYPE = "_dominorami._tcp."

/**
 * Handles automatic room advertising by Host over UDP broadcast & Android NSD.
 */
class RoomBroadcaster(
    private val scope: CoroutineScope,
    private val context: Context? = null,
    private val roomId: String,
    private val roomName: String,
    private val hostName: String,
    private val hostIp: String,
    private val getPlayerCount: () -> Int
) {
    private var job: Job? = null
    private var socket: DatagramSocket? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun start() {
        stop()
        startUdpBroadcast()
        startNsdRegistration()
    }

    private fun startUdpBroadcast() {
        job = scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                }
                while (isActive) {
                    val payload = JSONObject().apply {
                        put("magic", "RAMI_DOMINO")
                        put("roomId", roomId)
                        put("roomName", roomName)
                        put("hostName", hostName)
                        put("hostIp", hostIp)
                        put("port", DEFAULT_PORT)
                        put("playerCount", getPlayerCount())
                        put("maxPlayers", 4)
                    }.toString().toByteArray(Charsets.UTF_8)

                    val packet = DatagramPacket(
                        payload,
                        payload.size,
                        NetworkUtils.getBroadcastAddress(),
                        DISCOVERY_PORT
                    )
                    socket?.send(packet)
                    delay(1500)
                }
            } catch (_: Exception) {
                // Ignore network closing/interface change exceptions
            } finally {
                socket?.close()
            }
        }
    }

    private fun startNsdRegistration() {
        context?.let { ctx ->
            try {
                nsdManager = ctx.getSystemService(Context.NSD_SERVICE) as? NsdManager
                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = "DominoRami_${roomId}"
                    serviceType = NSD_SERVICE_TYPE
                    port = DEFAULT_PORT
                    setAttribute("roomId", roomId)
                    setAttribute("roomName", roomName)
                    setAttribute("hostName", hostName)
                    setAttribute("hostIp", hostIp)
                }

                registrationListener = object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
                    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
                    override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                }

                nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null

        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (_: Exception) {}
        registrationListener = null
        nsdManager = null
    }
}

/**
 * Listens for UDP room beacons and Android NSD to discover games on local Hotspot or Wi-Fi.
 */
class RoomScanner(
    private val scope: CoroutineScope,
    private val context: Context? = null
) {
    private var job: Job? = null
    private var socket: DatagramSocket? = null
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    fun startScanning() {
        stopScanning()
        startUdpScanner()
        startNsdDiscovery()
    }

    private fun startUdpScanner() {
        job = scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                    soTimeout = 2000
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val json = JSONObject(text)
                        if (json.optString("magic") == "RAMI_DOMINO") {
                            val room = DiscoveredRoom(
                                roomId = json.getString("roomId"),
                                roomName = json.getString("roomName"),
                                hostName = json.getString("hostName"),
                                hostIp = json.getString("hostIp"),
                                port = json.optInt("port", DEFAULT_PORT),
                                currentPlayers = json.getInt("playerCount"),
                                maxPlayers = json.optInt("maxPlayers", 4),
                                lastSeenTimestamp = System.currentTimeMillis()
                            )
                            updateRoom(room)
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        cleanStaleRooms()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {
            } finally {
                socket?.close()
            }
        }
    }

    private fun startNsdDiscovery() {
        context?.let { ctx ->
            try {
                nsdManager = ctx.getSystemService(Context.NSD_SERVICE) as? NsdManager
                discoveryListener = object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(regType: String) {}
                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        if (serviceInfo.serviceType.contains("dominorami")) {
                            resolveNsdService(serviceInfo)
                        }
                    }
                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                    override fun onDiscoveryStopped(serviceType: String) {}
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                }
                nsdManager?.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            } catch (_: Exception) {}
        }
    }

    private fun resolveNsdService(serviceInfo: NsdServiceInfo) {
        try {
            nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val host = resolved.host?.hostAddress ?: return
                    val port = resolved.port
                    val rId = resolved.attributes["roomId"]?.let { String(it) } ?: resolved.serviceName.removePrefix("DominoRami_")
                    val rName = resolved.attributes["roomName"]?.let { String(it) } ?: "غرفة دومينو رامي"
                    val hName = resolved.attributes["hostName"]?.let { String(it) } ?: "المضيف"

                    val room = DiscoveredRoom(
                        roomId = rId,
                        roomName = rName,
                        hostName = hName,
                        hostIp = host,
                        port = port,
                        currentPlayers = 1,
                        maxPlayers = 4,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                    updateRoom(room)
                }
            })
        } catch (_: Exception) {}
    }

    private fun updateRoom(room: DiscoveredRoom) {
        val current = _discoveredRooms.value.toMutableList()
        val index = current.indexOfFirst { it.roomId == room.roomId }
        if (index >= 0) {
            current[index] = room
        } else {
            current.add(room)
        }
        _discoveredRooms.value = current
    }

    private fun cleanStaleRooms() {
        val now = System.currentTimeMillis()
        val fresh = _discoveredRooms.value.filter { now - it.lastSeenTimestamp < 6000 }
        if (fresh.size != _discoveredRooms.value.size) {
            _discoveredRooms.value = fresh
        }
    }

    fun stopScanning() {
        job?.cancel()
        job = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null

        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        discoveryListener = null
        nsdManager = null
    }
}
