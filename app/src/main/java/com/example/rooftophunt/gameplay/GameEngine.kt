package com.example.rooftophunt.gameplay

import com.example.rooftophunt.engine3d.CharacterOutfit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class MatchPhase {
    PRE_ROUND_COUNTDOWN,
    ROUND_ACTIVE,
    ROUND_ENDED,
    MATCH_FINISHED
}

class GameEngine(
    var currentMap: RooftopMap,
    val playerAName: String = "Player 1",
    val playerBName: String = "Player 2",
    var playerAOutfit: CharacterOutfit = CharacterOutfit.DEFAULT_HUNTER,
    var playerBOutfit: CharacterOutfit = CharacterOutfit.DEFAULT_RUNNER,
    val isMultiplayerHost: Boolean = true,
    val isPracticeMode: Boolean = false
) {
    val physics = ObstaclePhysics()

    var matchPhase: MatchPhase = MatchPhase.PRE_ROUND_COUNTDOWN
    var currentRound: Int = 1
    val totalRounds: Int = 4

    var roundTimeRemaining: Float = 60.0f
    var countdownTimer: Float = 3.0f

    val playerA = PlayerState(
        id = "player_a",
        name = playerAName,
        outfit = playerAOutfit
    )

    val playerB = PlayerState(
        id = "player_b",
        name = playerBName,
        outfit = playerBOutfit
    )

    // 3 Ability Slots for Local Player
    val abilitySlots = mutableListOf(
        AbilitySlotState(SpecialAbility.SPEED_BOOST),
        AbilitySlotState(SpecialAbility.SHIELD),
        AbilitySlotState(SpecialAbility.HEAL)
    )

    val roundResults = mutableListOf<RoundResult>()
    var matchSummary: MatchSummary? = null

    // Callbacks
    var onSoundTrigger: ((String) -> Unit)? = null
    var onNotification: ((String) -> Unit)? = null

    init {
        setupRoundRolesAndSpawns()
    }

    fun setupRoundRolesAndSpawns() {
        // Round rotation:
        // Round 1: A=Hunter, B=Runner
        // Round 2: A=Runner, B=Hunter
        // Round 3: A=Hunter, B=Runner
        // Round 4: A=Runner, B=Hunter
        when (currentRound) {
            1, 3 -> {
                playerA.role = PlayerRole.HUNTER
                playerB.role = PlayerRole.RUNNER
            }
            2, 4 -> {
                playerA.role = PlayerRole.RUNNER
                playerB.role = PlayerRole.HUNTER
            }
        }

        playerA.health = 100
        playerB.health = 100
        playerA.damageCooldown = 0f
        playerB.damageCooldown = 0f
        playerA.speedBoostTimer = 0f
        playerB.speedBoostTimer = 0f
        playerA.shieldTimer = 0f
        playerB.shieldTimer = 0f

        // Spawn locations on current map
        val b0 = currentMap.buildings[0]
        val bLast = currentMap.buildings[currentMap.buildings.size - 1]

        val hunterSpawn = if (playerA.role == PlayerRole.HUNTER) b0 else bLast
        val runnerSpawn = if (playerA.role == PlayerRole.RUNNER) b0 else bLast

        playerA.x = hunterSpawn.x
        playerA.y = hunterSpawn.y + hunterSpawn.height * 0.5f + 1f
        playerA.z = hunterSpawn.z
        playerA.vx = 0f
        playerA.vy = 0f
        playerA.vz = 0f

        playerB.x = runnerSpawn.x
        playerB.y = runnerSpawn.y + runnerSpawn.height * 0.5f + 1f
        playerB.z = runnerSpawn.z
        playerB.vx = 0f
        playerB.vy = 0f
        playerB.vz = 0f

        // Reset abilities
        abilitySlots.forEach {
            it.isCollected = false
            it.isActive = false
            it.timerRemaining = 0f
        }

        // Reset map collectibles
        currentMap.coins.forEach { it.isCollected = false }
        currentMap.powerUps.forEach { it.isCollected = false; it.respawnTimer = 0f }

        roundTimeRemaining = 60.0f
        countdownTimer = 3.0f
        matchPhase = MatchPhase.PRE_ROUND_COUNTDOWN
    }

    fun update(
        delta: Float,
        inputDx: Float,
        inputDz: Float,
        isJumpPressed: Boolean,
        isSprintPressed: Boolean
    ) {
        when (matchPhase) {
            MatchPhase.PRE_ROUND_COUNTDOWN -> {
                countdownTimer -= delta
                if (countdownTimer <= 0f) {
                    matchPhase = MatchPhase.ROUND_ACTIVE
                    onNotification?.invoke("ROUND $currentRound START!")
                    onSoundTrigger?.invoke("start_whistle")
                }
            }
            MatchPhase.ROUND_ACTIVE -> {
                roundTimeRemaining = (roundTimeRemaining - delta).coerceAtLeast(0f)

                // Update moving obstacles on map
                physics.updateMovingObstacles(currentMap.obstacles, delta)

                // Update local player (Player A)
                playerA.isSprinting = isSprintPressed
                physics.resolvePlayerMovementAndCollisions(
                    player = playerA,
                    map = currentMap,
                    inputDx = inputDx,
                    inputDz = inputDz,
                    isJumpPressed = isJumpPressed,
                    delta = delta,
                    onDamageTaken = { dmg ->
                        onSoundTrigger?.invoke("hazard_hit")
                        onNotification?.invoke("-$dmg DAMAGE!")
                    }
                )

                // Update Bot AI in Practice Mode
                if (isPracticeMode) {
                    updatePracticeBotAI(delta)
                }

                // Check coin collection
                for (coin in currentMap.coins) {
                    if (!coin.isCollected) {
                        val dx = playerA.x - coin.x
                        val dy = playerA.y - coin.y
                        val dz = playerA.z - coin.z
                        if (dx * dx + dy * dy + dz * dz < 1.4f) {
                            coin.isCollected = true
                            playerA.coinsCollected++
                            onSoundTrigger?.invoke("coin_pickup")
                        }
                    }
                }

                // Check powerup collection
                for (pw in currentMap.powerUps) {
                    if (!pw.isCollected) {
                        val dx = playerA.x - pw.x
                        val dy = playerA.y - pw.y
                        val dz = playerA.z - pw.z
                        if (dx * dx + dy * dy + dz * dz < 1.8f) {
                            pw.isCollected = true
                            pw.respawnTimer = 15.0f
                            // Grant ability to local player slot
                            val slot = abilitySlots.find { it.ability == pw.type }
                            slot?.isCollected = true
                            onSoundTrigger?.invoke("powerup_pickup")
                            onNotification?.invoke("${pw.type.title} Collected!")
                        }
                    } else if (pw.respawnTimer > 0f) {
                        pw.respawnTimer -= delta
                        if (pw.respawnTimer <= 0f) {
                            pw.isCollected = false
                        }
                    }
                }

                // Update active ability slot timers
                abilitySlots.forEach { slot ->
                    if (slot.isActive && slot.timerRemaining > 0f) {
                        slot.timerRemaining -= delta
                        if (slot.timerRemaining <= 0f) {
                            slot.isActive = false
                        }
                    }
                }

                // Check Capture condition
                val hunter = if (playerA.role == PlayerRole.HUNTER) playerA else playerB
                val runner = if (playerA.role == PlayerRole.RUNNER) playerA else playerB

                val isCaptured = physics.checkHunterRunnerCapture(hunter, runner)
                if (isCaptured) {
                    endRound(hunterWon = true, isTimeout = false)
                    return
                }

                // Check Health 0 knockout
                if (playerA.health <= 0) {
                    endRound(hunterWon = (playerB.role == PlayerRole.HUNTER), isTimeout = false)
                    return
                }
                if (playerB.health <= 0) {
                    endRound(hunterWon = (playerA.role == PlayerRole.HUNTER), isTimeout = false)
                    return
                }

                // Check 60s timeout
                if (roundTimeRemaining <= 0f) {
                    // Runner survived!
                    endRound(hunterWon = false, isTimeout = true)
                }
            }
            MatchPhase.ROUND_ENDED -> {
                // Waiting for next round or match finish
            }
            MatchPhase.MATCH_FINISHED -> {
                // Complete
            }
        }
    }

    fun activateAbility(slotIndex: Int) {
        if (slotIndex !in abilitySlots.indices) return
        val slot = abilitySlots[slotIndex]
        if (!slot.isCollected || slot.isActive) return

        slot.isActive = true
        slot.isCollected = false // Consumed upon activation
        slot.timerRemaining = slot.ability.durationSeconds

        when (slot.ability) {
            SpecialAbility.SPEED_BOOST -> {
                playerA.speedBoostTimer = slot.ability.durationSeconds
                onSoundTrigger?.invoke("speed_boost")
                onNotification?.invoke("SPEED BOOST ACTIVATED!")
            }
            SpecialAbility.SHIELD -> {
                playerA.shieldTimer = slot.ability.durationSeconds
                onSoundTrigger?.invoke("shield_activate")
                onNotification?.invoke("SHIELD ACTIVATED!")
            }
            SpecialAbility.HEAL -> {
                val healAmount = 30
                playerA.health = (playerA.health + healAmount).coerceAtMost(100)
                onSoundTrigger?.invoke("heal_chime")
                onNotification?.invoke("+$healAmount HEALTH RESTORED!")
            }
        }
    }

    private fun endRound(hunterWon: Boolean, isTimeout: Boolean) {
        matchPhase = MatchPhase.ROUND_ENDED

        val hunter = if (playerA.role == PlayerRole.HUNTER) playerA else playerB
        val runner = if (playerA.role == PlayerRole.RUNNER) playerA else playerB

        val hunterScore = if (hunterWon) 100 + (roundTimeRemaining * 1.5f).toInt() else 0
        val runnerScore = if (!hunterWon && isTimeout) 100 else 0

        val roundResult = RoundResult(
            roundNumber = currentRound,
            hunterId = hunter.id,
            runnerId = runner.id,
            hunterCaptured = hunterWon,
            captureTimeRemaining = roundTimeRemaining,
            runnerSurvived = !hunterWon && isTimeout,
            hunterScore = hunterScore,
            runnerScore = runnerScore,
            coinsEarnedPlayerA = playerA.coinsCollected,
            coinsEarnedPlayerB = playerB.coinsCollected
        )
        roundResults.add(roundResult)

        if (hunterWon) {
            onNotification?.invoke("HUNTER CAPTURED RUNNER!")
            onSoundTrigger?.invoke("capture_horn")
        } else {
            onNotification?.invoke("RUNNER SURVIVED THE ROUND!")
            onSoundTrigger?.invoke("victory_fanfare")
        }

        if (currentRound >= totalRounds) {
            finishMatch()
        }
    }

    fun proceedToNextRound() {
        if (currentRound < totalRounds) {
            currentRound++
            setupRoundRolesAndSpawns()
        } else {
            finishMatch()
        }
    }

    private fun finishMatch() {
        matchPhase = MatchPhase.MATCH_FINISHED

        var scoreA = 0
        var scoreB = 0
        var coinsA = playerA.coinsCollected
        var coinsB = playerB.coinsCollected
        var capsA = 0
        var capsB = 0

        for (r in roundResults) {
            if (playerA.role == PlayerRole.HUNTER) {
                scoreA += r.hunterScore
                scoreB += r.runnerScore
                if (r.hunterCaptured) capsA++
            } else {
                scoreA += r.runnerScore
                scoreB += r.hunterScore
                if (r.hunterCaptured) capsB++
            }
        }

        scoreA += coinsA * 10
        scoreB += coinsB * 10

        val winner = when {
            scoreA > scoreB -> playerAName
            scoreB > scoreA -> playerBName
            capsA > capsB -> playerAName
            capsB > capsA -> playerBName
            else -> "TIE"
        }

        matchSummary = MatchSummary(
            playerAName = playerAName,
            playerBName = playerBName,
            roundResults = roundResults,
            totalScoreA = scoreA,
            totalScoreB = scoreB,
            totalCoinsA = coinsA,
            totalCoinsB = coinsB,
            totalCapturesA = capsA,
            totalCapturesB = capsB,
            winnerName = winner,
            isTie = winner == "TIE"
        )
    }

    private fun updatePracticeBotAI(delta: Float) {
        val targetX: Float
        val targetZ: Float

        if (playerB.role == PlayerRole.HUNTER) {
            // Hunter Bot chases Player A
            targetX = playerA.x
            targetZ = playerA.z
        } else {
            // Runner Bot flees from Player A or heads towards coins
            val dx = playerB.x - playerA.x
            val dz = playerB.z - playerA.z
            val dist = sqrt(dx * dx + dz * dz)
            if (dist < 10.0f) {
                targetX = playerB.x + (dx / dist) * 15f
                targetZ = playerB.z + (dz / dist) * 15f
            } else {
                val nearestCoin = currentMap.coins.firstOrNull { !it.isCollected }
                if (nearestCoin != null) {
                    targetX = nearestCoin.x
                    targetZ = nearestCoin.z
                } else {
                    targetX = 0f
                    targetZ = 0f
                }
            }
        }

        val diffX = targetX - playerB.x
        val diffZ = targetZ - playerB.z
        val len = sqrt(diffX * diffX + diffZ * diffZ)

        var dirX = 0f
        var dirZ = 0f
        if (len > 0.5f) {
            dirX = diffX / len
            dirZ = diffZ / len
            playerB.yaw = Math.toDegrees(atan2(diffX.toDouble(), diffZ.toDouble())).toFloat()
        }

        // Jump over obstacles or gaps
        val shouldJump = playerB.isGrounded && (kotlin.math.abs(playerB.vx) < 0.2f && len > 2f)

        physics.resolvePlayerMovementAndCollisions(
            player = playerB,
            map = currentMap,
            inputDx = dirX,
            inputDz = dirZ,
            isJumpPressed = shouldJump,
            delta = delta,
            onDamageTaken = {}
        )
    }
}
