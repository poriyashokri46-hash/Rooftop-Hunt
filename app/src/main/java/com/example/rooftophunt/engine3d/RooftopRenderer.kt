package com.example.rooftophunt.engine3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.rooftophunt.gameplay.GameEngine
import com.example.rooftophunt.gameplay.ObstacleType
import com.example.rooftophunt.gameplay.PlayerRole
import com.example.rooftophunt.gameplay.SpecialAbility
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RooftopRenderer(
    var gameEngine: GameEngine?
) : GLSurfaceView.Renderer {

    private val shader = GlShader()
    private val matrixStack = MatrixStack()
    private val characterModel = Character3DModel()

    private lateinit var boxMesh: GlMesh
    private lateinit var cylinderMesh: GlMesh
    private lateinit var diamondMesh: GlMesh
    private lateinit var coinMesh: GlMesh

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Camera control
    var cameraYaw: Float = 0f
    var cameraPitch: Float = 15f
    var cameraDistance: Float = 6.0f
    var cameraSensitivity: Float = 1.0f

    // Camera smoothed position
    private var camEyeX = 0f
    private var camEyeY = 5f
    private var camEyeZ = -6f

    // Opponent telemetry for HUD
    var opponentDistance: Float = 0f
        private set
    var opponentBearingDegrees: Float = 0f
        private set

    private var animTime: Float = 0f
    private var lastFrameTime = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        shader.init()

        boxMesh = GlMesh.createBox(0.5f, 0.5f, 0.5f)
        cylinderMesh = GlMesh.createCylinder(0.5f, 1.0f, 10)
        diamondMesh = GlMesh.createDiamond(0.45f, 0.9f)
        coinMesh = GlMesh.createCoin(0.4f, 0.1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projMatrix, 0, 58f, aspect, 0.2f, 350f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameTime = now
        animTime += dt

        val engine = gameEngine ?: return
        val map = engine.currentMap
        val playerA = engine.playerA
        val playerB = engine.playerB

        // Sky & Fog setup
        val skyTop = map.skyColorTop
        val skyBot = map.skyColorBottom
        GLES30.glClearColor(skyTop[0], skyTop[1], skyTop[2], 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(shader.programId)

        // Set lighting uniforms
        GLES30.glUniform3fv(shader.uLightDirLocation, 1, map.lightDir, 0)
        GLES30.glUniform3fv(shader.uLightColorLocation, 1, map.lightColor, 0)
        GLES30.glUniform3fv(shader.uFogColorLocation, 1, map.fogColor, 0)
        GLES30.glUniform1f(shader.uFogDensityLocation, map.fogDensity)

        // Camera calculations behind Player A
        val radYaw = Math.toRadians(cameraYaw.toDouble()).toFloat()
        val radPitch = Math.toRadians(cameraPitch.toDouble()).toFloat()

        val targetEyeX = playerA.x - sin(radYaw) * cos(radPitch) * cameraDistance
        val targetEyeY = playerA.y + 1.8f + sin(radPitch) * cameraDistance
        val targetEyeZ = playerA.z - cos(radYaw) * cos(radPitch) * cameraDistance

        // Smooth camera follow
        val camSmooth = 12f * dt
        camEyeX += (targetEyeX - camEyeX) * camSmooth
        camEyeY += (targetEyeY - camEyeY) * camSmooth
        camEyeZ += (targetEyeZ - camEyeZ) * camSmooth

        val targetLookX = playerA.x
        val targetLookY = playerA.y + 1.2f
        val targetLookZ = playerA.z

        Matrix.setLookAtM(
            viewMatrix, 0,
            camEyeX, camEyeY, camEyeZ,
            targetLookX, targetLookY, targetLookZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES30.glUniform3f(shader.uCameraPosLocation, camEyeX, camEyeY, camEyeZ)

        // Telemetry calculation: Opponent distance and relative radar bearing
        val ddx = playerB.x - playerA.x
        val ddz = playerB.z - playerA.z
        opponentDistance = sqrt(ddx * ddx + ddz * ddz)

        val worldAngleDeg = Math.toDegrees(atan2(ddx.toDouble(), ddz.toDouble())).toFloat()
        var relAngle = worldAngleDeg - cameraYaw
        while (relAngle > 180f) relAngle -= 360f
        while (relAngle < -180f) relAngle += 360f
        opponentBearingDegrees = relAngle

        // 1. Draw Distant Skyline (atmospheric background buildings)
        drawDistantSkyline(map.fogColor)

        // 2. Draw Map Buildings
        for (b in map.buildings) {
            matrixStack.identity()
            matrixStack.translate(b.x, b.y, b.z)
            matrixStack.scale(b.width, b.height, b.depth)
            applyMVP()
            GLES30.glUniform4fv(shader.uColorLocation, 1, b.color, 0)
            GLES30.glUniform1f(shader.uEmissiveLocation, 0.05f)
            boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)

            // Building roof cap (slightly brighter)
            matrixStack.identity()
            matrixStack.translate(b.x, b.y + b.height * 0.5f, b.z)
            matrixStack.scale(b.width, 0.15f, b.depth)
            applyMVP()
            GLES30.glUniform4fv(shader.uColorLocation, 1, b.topColor, 0)
            GLES30.glUniform1f(shader.uEmissiveLocation, 0.12f)
            boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        }

        // 3. Draw Obstacles
        for (obs in map.obstacles) {
            matrixStack.identity()
            matrixStack.translate(obs.x, obs.y, obs.z)

            when (obs.type) {
                ObstacleType.ROTATING_BLADE -> {
                    matrixStack.rotate(obs.currentPhase, 0f, 1f, 0f)
                    matrixStack.scale(obs.width, obs.height, obs.depth)
                    applyMVP()
                    GLES30.glUniform4fv(shader.uColorLocation, 1, obs.color, 0)
                    GLES30.glUniform1f(shader.uEmissiveLocation, 0.8f)
                    boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
                }
                ObstacleType.SWINGING_BARRIER -> {
                    val swingAngle = sin(obs.currentPhase) * 60f
                    matrixStack.rotate(swingAngle, 0f, 0f, 1f)
                    matrixStack.scale(obs.width, obs.height, obs.depth)
                    applyMVP()
                    GLES30.glUniform4fv(shader.uColorLocation, 1, obs.color, 0)
                    GLES30.glUniform1f(shader.uEmissiveLocation, 0.6f)
                    boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
                }
                ObstacleType.LASER_BARRIER -> {
                    // Pulsing laser glow
                    val pulse = 0.7f + sin(animTime * 8f) * 0.3f
                    matrixStack.scale(obs.width, obs.height, obs.depth)
                    applyMVP()
                    val laserColor = floatArrayOf(obs.color[0], obs.color[1], obs.color[2], pulse)
                    GLES30.glUniform4fv(shader.uColorLocation, 1, laserColor, 0)
                    GLES30.glUniform1f(shader.uEmissiveLocation, 1.5f)
                    boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
                }
                ObstacleType.MOVING_CRANE -> {
                    matrixStack.rotate(obs.currentPhase * 0.3f, 0f, 1f, 0f)
                    matrixStack.scale(obs.width, obs.height, obs.depth)
                    applyMVP()
                    GLES30.glUniform4fv(shader.uColorLocation, 1, obs.color, 0)
                    GLES30.glUniform1f(shader.uEmissiveLocation, 0.3f)
                    boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
                }
                ObstacleType.PIPE, ObstacleType.CHIMNEY -> {
                    matrixStack.scale(obs.width, obs.height, obs.depth)
                    applyMVP()
                    GLES30.glUniform4fv(shader.uColorLocation, 1, obs.color, 0)
                    GLES30.glUniform1f(shader.uEmissiveLocation, 0.1f)
                    cylinderMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
                }
                else -> {
                    matrixStack.scale(obs.width, obs.height, obs.depth)
                    applyMVP()
                    GLES30.glUniform4fv(shader.uColorLocation, 1, obs.color, 0)
                    GLES30.glUniform1f(shader.uEmissiveLocation, if (obs.type == ObstacleType.MOVING_PLATFORM) 0.5f else 0.1f)
                    boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
                }
            }
        }

        // 4. Draw Collectible Coins (Spinning, golden glow)
        val coinColor = floatArrayOf(1.0f, 0.85f, 0.0f, 1.0f)
        for (coin in map.coins) {
            if (!coin.isCollected) {
                matrixStack.identity()
                val bob = sin(animTime * 4f + coin.id) * 0.15f
                matrixStack.translate(coin.x, coin.y + bob, coin.z)
                matrixStack.rotate(animTime * 180f, 0f, 1f, 0f)
                applyMVP()
                GLES30.glUniform4fv(shader.uColorLocation, 1, coinColor, 0)
                GLES30.glUniform1f(shader.uEmissiveLocation, 1.0f)
                coinMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
            }
        }

        // 5. Draw Collectible Power-Ups
        for (pw in map.powerUps) {
            if (!pw.isCollected) {
                matrixStack.identity()
                val bob = sin(animTime * 3f + pw.id) * 0.2f
                matrixStack.translate(pw.x, pw.y + bob, pw.z)
                matrixStack.rotate(animTime * 120f, 0.5f, 1f, 0.2f)
                applyMVP()
                val color = when (pw.type) {
                    SpecialAbility.SPEED_BOOST -> floatArrayOf(0.0f, 0.95f, 1.0f, 1.0f)
                    SpecialAbility.SHIELD -> floatArrayOf(0.85f, 0.2f, 1.0f, 1.0f)
                    SpecialAbility.HEAL -> floatArrayOf(0.1f, 1.0f, 0.4f, 1.0f)
                }
                GLES30.glUniform4fv(shader.uColorLocation, 1, color, 0)
                GLES30.glUniform1f(shader.uEmissiveLocation, 1.4f)
                diamondMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
            }
        }

        // 6. Draw Local Player (Player A)
        matrixStack.identity()
        matrixStack.translate(playerA.x, playerA.y, playerA.z)
        matrixStack.rotate(playerA.yaw, 0f, 1f, 0f)
        renderCharacter(playerA, isLocal = true)

        // 7. Draw Opponent Player (Player B)
        matrixStack.identity()
        matrixStack.translate(playerB.x, playerB.y, playerB.z)
        matrixStack.rotate(playerB.yaw, 0f, 1f, 0f)
        renderCharacter(playerB, isLocal = false)

        // 8. Draw Weather Particles
        drawWeatherParticles(map.weatherType, playerA.x, playerA.y, playerA.z)
    }

    private fun renderCharacter(player: com.example.rooftophunt.gameplay.PlayerState, isLocal: Boolean) {
        val isHunter = player.role == PlayerRole.HUNTER
        characterModel.draw(
            matrixStack = matrixStack,
            shader = shader,
            animState = player.animState,
            animTime = animTime,
            outfit = player.outfit,
            isHunter = isHunter,
            isShieldActive = player.isShieldActive,
            isSpeedActive = player.isSpeedBoostActive
        )
    }

    private fun drawDistantSkyline(fogColor: FloatArray) {
        val skyCol = floatArrayOf(fogColor[0] * 0.7f, fogColor[1] * 0.7f, fogColor[2] * 0.85f, 1.0f)
        val numBuildings = 8
        for (i in 0 until numBuildings) {
            val angle = (i.toFloat() / numBuildings) * 2f * Math.PI.toFloat()
            val dist = 140f
            val bx = cos(angle) * dist
            val bz = sin(angle) * dist
            val bh = 60f + (i % 3) * 35f

            matrixStack.identity()
            matrixStack.translate(bx, bh * 0.5f - 40f, bz)
            matrixStack.scale(25f, bh, 25f)
            applyMVP()
            GLES30.glUniform4fv(shader.uColorLocation, 1, skyCol, 0)
            GLES30.glUniform1f(shader.uEmissiveLocation, 0.02f)
            boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        }
    }

    private fun drawWeatherParticles(type: String, px: Float, py: Float, pz: Float) {
        if (type == "CLEAR") return

        val count = 40
        val pColor = when (type) {
            "RAIN" -> floatArrayOf(0.6f, 0.8f, 1.0f, 0.45f)
            "SNOW" -> floatArrayOf(0.95f, 0.95f, 1.0f, 0.7f)
            "SANDSTORM" -> floatArrayOf(0.9f, 0.7f, 0.4f, 0.5f)
            else -> floatArrayOf(0f, 0.9f, 1.0f, 0.4f)
        }

        for (i in 0 until count) {
            val seed = i * 137.5f
            val rx = ((sin(seed + animTime * 0.5f) * 18f))
            val rz = ((cos(seed + animTime * 0.5f) * 18f))
            val speedY = if (type == "RAIN") 18f else 5f
            val ry = ((seed * 10f - animTime * speedY) % 20f)

            matrixStack.identity()
            matrixStack.translate(px + rx, py + ry, pz + rz)
            if (type == "RAIN") {
                matrixStack.scale(0.04f, 0.8f, 0.04f)
            } else {
                matrixStack.scale(0.12f, 0.12f, 0.12f)
            }
            applyMVP()
            GLES30.glUniform4fv(shader.uColorLocation, 1, pColor, 0)
            GLES30.glUniform1f(shader.uEmissiveLocation, 0.5f)
            boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        }
    }

    private fun applyMVP() {
        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, matrixStack.current, 0)
        GLES30.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uMMatrixLocation, 1, false, matrixStack.current, 0)
    }

    fun handleDrag(deltaX: Float, deltaY: Float) {
        cameraYaw += deltaX * 0.22f * cameraSensitivity
        cameraPitch -= deltaY * 0.18f * cameraSensitivity
        cameraPitch = cameraPitch.coerceIn(2f, 75f)
    }
}
