package com.example.rooftophunt.gameplay

import com.example.rooftophunt.engine3d.CharacterAnimState
import com.example.rooftophunt.engine3d.CharacterOutfit

enum class PlayerRole {
    HUNTER,
    RUNNER
}

enum class SpecialAbility(
    val title: String,
    val durationSeconds: Float,
    val description: String
) {
    SPEED_BOOST("Speed Boost", 5.0f, "Supercharge movement speed and jump distance"),
    SHIELD("Protective Shield", 3.0f, "Invulnerability to Hunter capture"),
    HEAL("Health Recovery", 0.0f, "Restore up to +30 Health Points")
}

data class AbilitySlotState(
    val ability: SpecialAbility,
    var isCollected: Boolean = false,
    var isActive: Boolean = false,
    var timerRemaining: Float = 0.0f
)

data class PlayerState(
    var id: String = "local",
    var name: String = "Player",
    var x: Float = 0f,
    var y: Float = 1f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var yaw: Float = 0f,
    var isGrounded: Boolean = true,
    var health: Int = 100,
    var role: PlayerRole = PlayerRole.HUNTER,
    var animState: CharacterAnimState = CharacterAnimState.IDLE,
    var outfit: CharacterOutfit = CharacterOutfit.DEFAULT_HUNTER,
    var coinsCollected: Int = 0,
    var damageCooldown: Float = 0f,
    var speedBoostTimer: Float = 0f,
    var shieldTimer: Float = 0f,
    var isSprinting: Boolean = false
) {
    val isShieldActive: Boolean get() = shieldTimer > 0f
    val isSpeedBoostActive: Boolean get() = speedBoostTimer > 0f
}

data class RoundResult(
    val roundNumber: Int,
    val hunterId: String,
    val runnerId: String,
    val hunterCaptured: Boolean,
    val captureTimeRemaining: Float,
    val runnerSurvived: Boolean,
    val hunterScore: Int,
    val runnerScore: Int,
    val coinsEarnedPlayerA: Int,
    val coinsEarnedPlayerB: Int
)

data class MatchSummary(
    val playerAName: String,
    val playerBName: String,
    val roundResults: List<RoundResult>,
    val totalScoreA: Int,
    val totalScoreB: Int,
    val totalCoinsA: Int,
    val totalCoinsB: Int,
    val totalCapturesA: Int,
    val totalCapturesB: Int,
    val winnerName: String,
    val isTie: Boolean
)
