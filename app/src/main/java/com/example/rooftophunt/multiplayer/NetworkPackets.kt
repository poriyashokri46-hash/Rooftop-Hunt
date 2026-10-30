package com.example.rooftophunt.multiplayer

import org.json.JSONObject

enum class PacketType {
    JOIN_ROOM,
    ROOM_READY,
    STATE_UPDATE,
    ABILITY_TRIGGER,
    CAPTURE_EVENT,
    ROUND_SYNC,
    PING
}

data class PlayerSyncPacket(
    val playerId: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val yaw: Float,
    val animState: String,
    val health: Int,
    val isShieldActive: Boolean,
    val isSpeedActive: Boolean,
    val coins: Int
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("type", PacketType.STATE_UPDATE.name)
        obj.put("pid", playerId)
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        obj.put("z", z.toDouble())
        obj.put("vx", vx.toDouble())
        obj.put("vy", vy.toDouble())
        obj.put("vz", vz.toDouble())
        obj.put("yaw", yaw.toDouble())
        obj.put("anim", animState)
        obj.put("hp", health)
        obj.put("shield", isShieldActive)
        obj.put("speed", isSpeedActive)
        obj.put("coins", coins)
        return obj.toString()
    }

    companion object {
        fun fromJson(obj: JSONObject): PlayerSyncPacket {
            return PlayerSyncPacket(
                playerId = obj.optString("pid", "player"),
                x = obj.optDouble("x", 0.0).toFloat(),
                y = obj.optDouble("y", 0.0).toFloat(),
                z = obj.optDouble("z", 0.0).toFloat(),
                vx = obj.optDouble("vx", 0.0).toFloat(),
                vy = obj.optDouble("vy", 0.0).toFloat(),
                vz = obj.optDouble("vz", 0.0).toFloat(),
                yaw = obj.optDouble("yaw", 0.0).toFloat(),
                animState = obj.optString("anim", "IDLE"),
                health = obj.optInt("hp", 100),
                isShieldActive = obj.optBoolean("shield", false),
                isSpeedActive = obj.optBoolean("speed", false),
                coins = obj.optInt("coins", 0)
            )
        }
    }
}
