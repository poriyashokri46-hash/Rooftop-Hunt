package com.example.rooftophunt.gameplay

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ObstaclePhysics {

    fun updateMovingObstacles(obstacles: List<Obstacle>, delta: Float) {
        for (obs in obstacles) {
            if (obs.isMoving) {
                obs.currentPhase += obs.moveSpeed * delta * 0.05f
                when (obs.moveAxis) {
                    0 -> { // X axis oscillation
                        obs.x += cos(obs.currentPhase) * obs.moveDistance * delta * 0.5f
                    }
                    1 -> { // Y axis oscillation
                        obs.y += sin(obs.currentPhase) * obs.moveDistance * delta * 0.5f
                    }
                    2 -> { // Z axis oscillation
                        obs.z += sin(obs.currentPhase) * obs.moveDistance * delta * 0.5f
                    }
                    3 -> { // Rotation (phase is angle)
                        // Phase continuously increments
                    }
                }
            }

            if (obs.type == ObstacleType.FALLING_PLATFORM && obs.isTriggered) {
                obs.fallVelocity += 9.8f * delta
                obs.y -= obs.fallVelocity * delta
            }
        }
    }

    fun resolvePlayerMovementAndCollisions(
        player: PlayerState,
        map: RooftopMap,
        inputDx: Float,
        inputDz: Float,
        isJumpPressed: Boolean,
        delta: Float,
        onDamageTaken: (Int) -> Unit
    ) {
        val gravity = -24.0f
        val baseSpeed = if (player.isSprinting) 11.0f else 7.0f
        val speedMult = if (player.isSpeedBoostActive) 1.6f else 1.0f
        val friction = map.frictionModifier

        // Update timers
        if (player.damageCooldown > 0f) {
            player.damageCooldown = (player.damageCooldown - delta).coerceAtLeast(0f)
        }
        if (player.speedBoostTimer > 0f) {
            player.speedBoostTimer = (player.speedBoostTimer - delta).coerceAtLeast(0f)
        }
        if (player.shieldTimer > 0f) {
            player.shieldTimer = (player.shieldTimer - delta).coerceAtLeast(0f)
        }

        // Horizontal velocity
        val targetVx = inputDx * baseSpeed * speedMult * friction
        val targetVz = inputDz * baseSpeed * speedMult * friction

        val accel = if (player.isGrounded) 14.0f else 6.0f
        player.vx += (targetVx - player.vx) * accel * delta
        player.vz += (targetVz - player.vz) * accel * delta

        // Jump
        if (isJumpPressed && player.isGrounded) {
            val jumpPower = if (player.isSpeedBoostActive) 11.5f else 9.5f
            player.vy = jumpPower
            player.isGrounded = false
            player.animState = com.example.rooftophunt.engine3d.CharacterAnimState.JUMP
        }

        // Apply gravity
        player.vy += gravity * delta
        if (player.vy < -30f) player.vy = -30f

        // Move horizontally
        player.x += player.vx * delta
        player.z += player.vz * delta
        // Move vertically
        player.y += player.vy * delta

        // Building rooftop collision
        val pRadius = 0.4f
        val pHeight = 1.8f
        var onRoof = false

        for (b in map.buildings) {
            val halfW = b.width * 0.5f
            val halfD = b.depth * 0.5f
            val roofTop = b.y + b.height * 0.5f
            val roofBottom = b.y - b.height * 0.5f

            val inX = (player.x + pRadius >= b.x - halfW) && (player.x - pRadius <= b.x + halfW)
            val inZ = (player.z + pRadius >= b.z - halfD) && (player.z - pRadius <= b.z + halfD)

            if (inX && inZ) {
                // Check if standing on or landing on top
                if (player.y >= roofTop - 0.6f && player.y <= roofTop + 0.8f && player.vy <= 0.1f) {
                    player.y = roofTop
                    player.vy = 0f
                    player.isGrounded = true
                    onRoof = true
                } else if (player.y < roofTop - 0.6f && player.y > roofBottom) {
                    // Push out horizontally
                    val dxLeft = kotlin.math.abs(player.x - (b.x - halfW))
                    val dxRight = kotlin.math.abs(player.x - (b.x + halfW))
                    val dzFront = kotlin.math.abs(player.z - (b.z - halfD))
                    val dzBack = kotlin.math.abs(player.z - (b.z + halfD))
                    val minPen = minOf(dxLeft, dxRight, dzFront, dzBack)
                    when (minPen) {
                        dxLeft -> player.x = b.x - halfW - pRadius
                        dxRight -> player.x = b.x + halfW + pRadius
                        dzFront -> player.z = b.z - halfD - pRadius
                        dzBack -> player.z = b.z + halfD + pRadius
                    }
                }
            }
        }

        // Moving platforms / obstacles vertical collision
        for (obs in map.obstacles) {
            if (obs.type == ObstacleType.MOVING_PLATFORM || obs.type == ObstacleType.FALLING_PLATFORM) {
                val halfW = obs.width * 0.5f
                val halfD = obs.depth * 0.5f
                val topY = obs.y + obs.height * 0.5f
                val inX = (player.x + pRadius >= obs.x - halfW) && (player.x - pRadius <= obs.x + halfW)
                val inZ = (player.z + pRadius >= obs.z - halfD) && (player.z - pRadius <= obs.z + halfD)

                if (inX && inZ && player.y >= topY - 0.5f && player.y <= topY + 0.7f && player.vy <= 0.1f) {
                    player.y = topY
                    player.vy = 0f
                    player.isGrounded = true
                    onRoof = true
                    if (obs.type == ObstacleType.FALLING_PLATFORM) {
                        obs.isTriggered = true
                    }
                }
            }
        }

        if (!onRoof && player.y > -20f) {
            player.isGrounded = false
        }

        // Hazard collision detection
        for (obs in map.obstacles) {
            if (obs.damage > 0 && player.damageCooldown <= 0f) {
                val halfW = obs.width * 0.5f
                val halfH = obs.height * 0.5f
                val halfD = obs.depth * 0.5f

                val inX = (player.x + pRadius >= obs.x - halfW) && (player.x - pRadius <= obs.x + halfW)
                val inY = (player.y + pHeight >= obs.y - halfH) && (player.y <= obs.y + halfH)
                val inZ = (player.z + pRadius >= obs.z - halfD) && (player.z - pRadius <= obs.z + halfD)

                if (inX && inY && inZ) {
                    // Collision with hazard!
                    player.health = (player.health - obs.damage).coerceAtLeast(0)
                    player.damageCooldown = 1.2f
                    // Knockback
                    val knockDirX = if (player.x < obs.x) -3.5f else 3.5f
                    val knockDirZ = if (player.z < obs.z) -3.5f else 3.5f
                    player.vx = knockDirX
                    player.vz = knockDirZ
                    player.vy = 3.0f
                    player.animState = com.example.rooftophunt.engine3d.CharacterAnimState.HURT
                    onDamageTaken(obs.damage)
                }
            }
        }

        // Fell off roof into void check
        if (player.y < -12.0f) {
            // Severe fall damage
            player.health = (player.health - 30).coerceAtLeast(0)
            player.damageCooldown = 1.5f
            onDamageTaken(30)
            // Respawn on first building
            val respawnBuilding = map.buildings[0]
            player.x = respawnBuilding.x
            player.y = respawnBuilding.y + respawnBuilding.height * 0.5f + 1f
            player.z = respawnBuilding.z
            player.vx = 0f
            player.vy = 0f
            player.vz = 0f
        }

        // Update animation state
        val horizSpeed = sqrt(player.vx * player.vx + player.vz * player.vz)
        if (player.damageCooldown > 0.8f) {
            player.animState = com.example.rooftophunt.engine3d.CharacterAnimState.HURT
        } else if (!player.isGrounded) {
            player.animState = if (player.vy > 0f) com.example.rooftophunt.engine3d.CharacterAnimState.JUMP else com.example.rooftophunt.engine3d.CharacterAnimState.FALL
        } else if (horizSpeed > 8.0f) {
            player.animState = com.example.rooftophunt.engine3d.CharacterAnimState.SPRINT
        } else if (horizSpeed > 0.4f) {
            player.animState = com.example.rooftophunt.engine3d.CharacterAnimState.RUN
        } else {
            player.animState = com.example.rooftophunt.engine3d.CharacterAnimState.IDLE
        }
    }

    fun checkHunterRunnerCapture(
        hunter: PlayerState,
        runner: PlayerState
    ): Boolean {
        val dx = hunter.x - runner.x
        val dy = hunter.y - runner.y
        val dz = hunter.z - runner.z
        val distSq = dx * dx + dy * dy + dz * dz

        // Capture distance threshold = 1.5 meters (1.5^2 = 2.25)
        if (distSq <= 2.25f) {
            if (runner.isShieldActive) {
                // Shield deflects capture!
                hunter.vx = -hunter.vx * 1.5f
                hunter.vz = -hunter.vz * 1.5f
                return false
            }
            return true
        }
        return false
    }
}
