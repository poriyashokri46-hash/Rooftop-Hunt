package com.example.rooftophunt.multiplayer

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class NetworkConnectionState {
    DISCONNECTED,
    CONNECTING,
    WAITING_FOR_PLAYER,
    CONNECTED,
    ERROR
}

class NetworkManager {
    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val netScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    var connectionState: NetworkConnectionState = NetworkConnectionState.DISCONNECTED
        private set

    var currentRoomCode: String = ""
        private set
    var localPlayerId: String = "p_" + System.currentTimeMillis().toString().takeLast(5)
    var localPlayerName: String = "Hunter"
    var opponentPlayerName: String = "Opponent"
    var isHost: Boolean = false

    var onConnectionStateChanged: ((NetworkConnectionState) -> Unit)? = null
    var onOpponentJoined: ((String, String) -> Unit)? = null
    var onOpponentState: ((PlayerSyncPacket) -> Unit)? = null
    var onCaptureEvent: ((String, String) -> Unit)? = null
    var onRoundSync: ((Int, Float) -> Unit)? = null

    // Multiple fallback public echo/relay endpoints
    private val relayEndpoints = listOf(
        "wss://echo.websocket.events",
        "wss://ws.postman-echo.com/raw"
    )

    fun createOrJoinRoom(roomCode: String, playerName: String, outfit: String, asHost: Boolean) {
        disconnect()
        currentRoomCode = roomCode.uppercase().trim()
        localPlayerName = playerName
        isHost = asHost

        updateState(NetworkConnectionState.CONNECTING)

        val endpoint = relayEndpoints[0]
        val request = Request.Builder().url(endpoint).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                updateState(if (asHost) NetworkConnectionState.WAITING_FOR_PLAYER else NetworkConnectionState.CONNECTED)

                // Announce join room
                val joinMsg = JSONObject().apply {
                    put("type", PacketType.JOIN_ROOM.name)
                    put("room", currentRoomCode)
                    put("pid", localPlayerId)
                    put("name", localPlayerName)
                    put("outfit", outfit)
                    put("isHost", asHost)
                }
                ws.send(joinMsg.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("NetworkManager", "WebSocket failed: ${t.message}")
                updateState(NetworkConnectionState.ERROR)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                updateState(NetworkConnectionState.DISCONNECTED)
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val msgRoom = json.optString("room", "")

            // Only process messages for this room
            if (msgRoom.isNotEmpty() && msgRoom != currentRoomCode) {
                return
            }

            val senderId = json.optString("pid", "")
            if (senderId == localPlayerId) {
                // Ignore self echo from echo server
                return
            }

            val type = json.optString("type", "")
            when (type) {
                PacketType.JOIN_ROOM.name -> {
                    opponentPlayerName = json.optString("name", "Rival")
                    val oppOutfit = json.optString("outfit", "DEFAULT_RUNNER")
                    updateState(NetworkConnectionState.CONNECTED)
                    onOpponentJoined?.invoke(opponentPlayerName, oppOutfit)

                    // If we are host, respond with room ready
                    if (isHost) {
                        val readyMsg = JSONObject().apply {
                            put("type", PacketType.ROOM_READY.name)
                            put("room", currentRoomCode)
                            put("pid", localPlayerId)
                            put("name", localPlayerName)
                        }
                        webSocket?.send(readyMsg.toString())
                    }
                }
                PacketType.ROOM_READY.name -> {
                    opponentPlayerName = json.optString("name", "Host")
                    updateState(NetworkConnectionState.CONNECTED)
                    onOpponentJoined?.invoke(opponentPlayerName, "DEFAULT_HUNTER")
                }
                PacketType.STATE_UPDATE.name -> {
                    val packet = PlayerSyncPacket.fromJson(json)
                    onOpponentState?.invoke(packet)
                }
                PacketType.CAPTURE_EVENT.name -> {
                    val hunterId = json.optString("hunter", "")
                    val runnerId = json.optString("runner", "")
                    onCaptureEvent?.invoke(hunterId, runnerId)
                }
                PacketType.ROUND_SYNC.name -> {
                    val round = json.optInt("round", 1)
                    val time = json.optDouble("time", 60.0).toFloat()
                    onRoundSync?.invoke(round, time)
                }
            }
        } catch (_: Exception) {}
    }

    fun sendPlayerState(packet: PlayerSyncPacket) {
        if (connectionState != NetworkConnectionState.CONNECTED) return
        try {
            val json = JSONObject(packet.toJson())
            json.put("room", currentRoomCode)
            webSocket?.send(json.toString())
        } catch (_: Exception) {}
    }

    fun sendCapture(hunterId: String, runnerId: String) {
        try {
            val msg = JSONObject().apply {
                put("type", PacketType.CAPTURE_EVENT.name)
                put("room", currentRoomCode)
                put("pid", localPlayerId)
                put("hunter", hunterId)
                put("runner", runnerId)
            }
            webSocket?.send(msg.toString())
        } catch (_: Exception) {}
    }

    private fun updateState(newState: NetworkConnectionState) {
        connectionState = newState
        onConnectionStateChanged?.invoke(newState)
    }

    fun disconnect() {
        syncJob?.cancel()
        syncJob = null
        try {
            webSocket?.close(1000, "User disconnect")
        } catch (_: Exception) {}
        webSocket = null
        updateState(NetworkConnectionState.DISCONNECTED)
    }
}
