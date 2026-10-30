package com.example.rooftophunt.data

import android.content.Context
import android.content.SharedPreferences
import com.example.rooftophunt.engine3d.CharacterOutfit

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rooftop_hunt_prefs", Context.MODE_PRIVATE)

    var username: String
        get() = prefs.getString("username", "SkyRunner") ?: "SkyRunner"
        set(value) = prefs.edit().putString("username", value).apply()

    var avatarId: Int
        get() = prefs.getInt("avatar_id", 0)
        set(value) = prefs.edit().putInt("avatar_id", value).apply()

    var totalCoins: Int
        get() = prefs.getInt("total_coins", 500) // 500 welcome coins!
        set(value) = prefs.edit().putInt("total_coins", value).apply()

    var competitiveRankScore: Int
        get() = prefs.getInt("rank_score", 1250)
        set(value) = prefs.edit().putInt("rank_score", value).apply()

    var totalMatches: Int
        get() = prefs.getInt("total_matches", 0)
        set(value) = prefs.edit().putInt("total_matches", value).apply()

    var totalWins: Int
        get() = prefs.getInt("total_wins", 0)
        set(value) = prefs.edit().putInt("total_wins", value).apply()

    var totalCaptures: Int
        get() = prefs.getInt("total_captures", 0)
        set(value) = prefs.edit().putInt("total_captures", value).apply()

    var totalEscapes: Int
        get() = prefs.getInt("total_escapes", 0)
        set(value) = prefs.edit().putInt("total_escapes", value).apply()

    var equippedOutfit: CharacterOutfit
        get() {
            val name = prefs.getString("equipped_outfit", CharacterOutfit.DEFAULT_HUNTER.name)
            return try {
                CharacterOutfit.valueOf(name ?: CharacterOutfit.DEFAULT_HUNTER.name)
            } catch (_: Exception) {
                CharacterOutfit.DEFAULT_HUNTER
            }
        }
        set(value) = prefs.edit().putString("equipped_outfit", value.name).apply()

    var unlockedOutfits: Set<String>
        get() = prefs.getStringSet("unlocked_outfits", setOf(
            CharacterOutfit.DEFAULT_RUNNER.name,
            CharacterOutfit.DEFAULT_HUNTER.name
        )) ?: setOf(CharacterOutfit.DEFAULT_RUNNER.name, CharacterOutfit.DEFAULT_HUNTER.name)
        set(value) = prefs.edit().putStringSet("unlocked_outfits", value).apply()

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music_enabled", true)
        set(value) = prefs.edit().putBoolean("music_enabled", value).apply()

    var cameraSensitivity: Float
        get() = prefs.getFloat("camera_sens", 1.0f)
        set(value) = prefs.edit().putFloat("camera_sens", value).apply()

    var lastDailyClaimTime: Long
        get() = prefs.getLong("last_daily_claim", 0L)
        set(value) = prefs.edit().putLong("last_daily_claim", value).apply()

    fun unlockOutfit(outfit: CharacterOutfit): Boolean {
        if (totalCoins >= outfit.coinCost) {
            totalCoins -= outfit.coinCost
            val updated = unlockedOutfits.toMutableSet()
            updated.add(outfit.name)
            unlockedOutfits = updated
            return true
        }
        return false
    }

    fun isOutfitUnlocked(outfit: CharacterOutfit): Boolean {
        return unlockedOutfits.contains(outfit.name)
    }

    fun claimDailyReward(): Int {
        val now = System.currentTimeMillis()
        val rewardAmount = 150
        totalCoins += rewardAmount
        lastDailyClaimTime = now
        return rewardAmount
    }

    val canClaimDailyReward: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now - lastDailyClaimTime > 24 * 60 * 60 * 1000L
        }
}
