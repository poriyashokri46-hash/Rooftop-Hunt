package com.example.rooftophunt.multiplayer

import android.util.Log
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

/**
 * WebSocket client for the Rooftop Hunt room server.
 * The URL can be overridden when constructing NetworkManager for staging builds.
 */
class NetworkManager(
    private val serverUrl: String = DEFAULT_SERVER_URL
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    var connectionState: NetworkConnectionState = NetworkConnectionState.DISCONNECTED
        private set
    var currentRoomCode: String = ""
        private set
    var localPlayerId: String = "p_" + System.currentTimeMillis().toString().takeLast(5)
    var localPlayerName: String = "Hunter"
    var opponentPlayerName: String = "Opponent"
    var isHost: Boolean = false
        private set
    var localSeat: Int = -1
        private set

    var onConnectionStateChanged: ((NetworkConnectionState) -> Unit)? = null
    var onOpponentJoined: ((String, String) -> Unit)? = null
    var onOpponentState: ((PlayerSyncPacket) -> Unit)? = null
    var onCaptureEvent: ((String, String) -> Unit)? = null
    var onRoundSync: ((Int, Float) -> Unit)? = null
    var onMatchFinished: (() -> Unit)? = null
    var onOpponentLeft: (() -> Unit)? = null

    fun createOrJoinRoom(roomCode: String, playerName: String, outfit: String, asHost: Boolean) {
        disconnect()
        currentRoomCode = roomCode.uppercase().trim()
        localPlayerName = playerName
        isHost = asHost
        localSeat = -1
        updateState(NetworkConnectionState.CONNECTING)

        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
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
                Log.e("NetworkManager", "WebSocket failed: ${t.message}", t)
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
            if (msgRoom.isNotEmpty() && msgRoom != currentRoomCode) return

            when (json.optString("type", "")) {
                "WAITING_FOR_PLAYER" -> {
                    currentRoomCode = json.optString("room", currentRoomCode)
                    updateState(NetworkConnectionState.WAITING_FOR_PLAYER)
                }
                "ROOM_READY" -> {
                    currentRoomCode = json.optString("room", currentRoomCode)
                    localSeat = json.optInt("seat", -1)
                    opponentPlayerName = json.optString("name", "Opponent")
                    updateState(NetworkConnectionState.CONNECTED)
                    onOpponentJoined?.invoke(opponentPlayerName, "DEFAULT_RUNNER")
                    val round = json.optInt("round", 1)
                    val time = json.optDouble("time", 60.0).toFloat()
                    onRoundSync?.invoke(round, time)
                }
                "ROOM_ERROR" -> {
                    Log.w("NetworkManager", "Room rejected: ${json.optString("reason")}")
                    updateState(NetworkConnectionState.ERROR)
                }
                PacketType.STATE_UPDATE.name -> {
                    if (json.optString("pid") == localPlayerId) return
                    onOpponentState?.invoke(PlayerSyncPacket.fromJson(json))
                }
                PacketType.CAPTURE_EVENT.name -> {
                    onCaptureEvent?.invoke(
                        json.optString("hunter", ""),
                        json.optString("runner", "")
                    )
                }
                PacketType.ROUND_SYNC.name -> {
                    onRoundSync?.invoke(
                        json.optInt("round", 1),
                        json.optDouble("time", 60.0).toFloat()
                    )
                }
                "MATCH_FINISHED" -> onMatchFinished?.invoke()
                "OPPONENT_LEFT" -> onOpponentLeft?.invoke()
                "PONG" -> Unit
            }
        } catch (e: Exception) {
            Log.w("NetworkManager", "Invalid server message", e)
        }
    }

    fun sendPlayerState(packet: PlayerSyncPacket) {
        if (connectionState != NetworkConnectionState.CONNECTED) return
        try {
            val json = JSONObject(packet.toJson())
            json.put("room", currentRoomCode)
            webSocket?.send(json.toString())
        } catch (e: Exception) {
            Log.w("NetworkManager", "Could not send player state", e)
        }
    }

    fun sendCapture(hunterId: String, runnerId: String) {
        // Capture is intentionally server-validated from the two latest player positions.
        // Client-provided capture claims are never authoritative.
    }

    private fun updateState(newState: NetworkConnectionState) {
        connectionState = newState
        onConnectionStateChanged?.invoke(newState)
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User disconnect")
        } catch (_: Exception) {
        }
        webSocket = null
        updateState(NetworkConnectionState.DISCONNECTED)
    }

    companion object {
        const val DEFAULT_SERVER_URL = "wss://rooftop-hunt-server.onrender.com/ws"
    }
}
