package com.example.rooftophunt.engine3d

import android.opengl.GLES30
import kotlin.math.sin

enum class CharacterAnimState {
    IDLE, RUN, SPRINT, JUMP, FALL, LAND, HURT
}

enum class CharacterOutfit(
    val displayName: String,
    val primaryColor: FloatArray,
    val secondaryColor: FloatArray,
    val accentColor: FloatArray,
    val coinCost: Int
) {
    DEFAULT_RUNNER("Neon Striker", floatArrayOf(0.0f, 0.9f, 0.85f, 1f), floatArrayOf(0.08f, 0.1f, 0.18f, 1f), floatArrayOf(0f, 1f, 0.8f, 1f), 0),
    DEFAULT_HUNTER("Apex Hunter", floatArrayOf(1.0f, 0.35f, 0.1f, 1f), floatArrayOf(0.12f, 0.08f, 0.08f, 1f), floatArrayOf(1f, 0.6f, 0f, 1f), 0),
    CYBER_NINJA("Cyber Ninja", floatArrayOf(0.1f, 0.12f, 0.18f, 1f), floatArrayOf(0.2f, 0.6f, 1f, 1f), floatArrayOf(0f, 0.9f, 1f, 1f), 300),
    NEON_PUNK("Neon Punk", floatArrayOf(1.0f, 0.05f, 0.5f, 1f), floatArrayOf(0.15f, 0.05f, 0.2f, 1f), floatArrayOf(1f, 0.8f, 0f, 1f), 500),
    STEALTH_RUNNER("Stealth Ops", floatArrayOf(0.05f, 0.05f, 0.07f, 1f), floatArrayOf(0.25f, 0.27f, 0.3f, 1f), floatArrayOf(0.7f, 0.1f, 0.1f, 1f), 750),
    GOLD_CHAMPION("Gold Champion", floatArrayOf(1.0f, 0.82f, 0.1f, 1f), floatArrayOf(0.95f, 0.95f, 0.98f, 1f), floatArrayOf(1f, 0.95f, 0.4f, 1f), 1200),
    HAZARD_TECH("Hazard Tech", floatArrayOf(1.0f, 0.55f, 0.0f, 1f), floatArrayOf(0.12f, 0.12f, 0.14f, 1f), floatArrayOf(1f, 0.9f, 0.0f, 1f), 450),
    FROST_GHOST("Frost Ghost", floatArrayOf(0.6f, 0.88f, 1.0f, 1f), floatArrayOf(0.1f, 0.2f, 0.35f, 1f), floatArrayOf(0.85f, 0.95f, 1.0f, 1f), 900)
}

class Character3DModel {
    private val boxMesh = GlMesh.createBox(0.5f, 0.5f, 0.5f)
    private val headMesh = GlMesh.createBox(0.5f, 0.5f, 0.5f)
    private val limbMesh = GlMesh.createCylinder(radius = 0.12f, height = 0.7f, segments = 8)

    fun draw(
        matrixStack: MatrixStack,
        shader: GlShader,
        animState: CharacterAnimState,
        animTime: Float,
        outfit: CharacterOutfit,
        isHunter: Boolean,
        isShieldActive: Boolean = false,
        isSpeedActive: Boolean = false
    ) {
        val pCol = outfit.primaryColor
        val sCol = outfit.secondaryColor
        val aCol = outfit.accentColor

        var legSwingL = 0f
        var legSwingR = 0f
        var armSwingL = 0f
        var armSwingR = 0f
        var torsoLean = 0f
        var torsoBob = 0f

        when (animState) {
            CharacterAnimState.IDLE -> {
                torsoBob = sin(animTime * 3f) * 0.03f
                armSwingL = sin(animTime * 2f) * 5f
                armSwingR = -armSwingL
            }
            CharacterAnimState.RUN -> {
                val cycle = animTime * 12f
                legSwingL = sin(cycle) * 35f
                legSwingR = -legSwingL
                armSwingL = -sin(cycle) * 40f
                armSwingR = -armSwingL
                torsoLean = 10f
                torsoBob = kotlin.math.abs(sin(cycle)) * 0.08f
            }
            CharacterAnimState.SPRINT -> {
                val cycle = animTime * 18f
                legSwingL = sin(cycle) * 55f
                legSwingR = -legSwingL
                armSwingL = -sin(cycle) * 60f
                armSwingR = -armSwingL
                torsoLean = 22f
                torsoBob = kotlin.math.abs(sin(cycle)) * 0.12f
            }
            CharacterAnimState.JUMP -> {
                torsoLean = 15f
                legSwingL = -30f
                legSwingR = -20f
                armSwingL = 50f
                armSwingR = 50f
            }
            CharacterAnimState.FALL -> {
                torsoLean = 5f
                legSwingL = 15f
                legSwingR = -10f
                armSwingL = 30f
                armSwingR = 30f
            }
            CharacterAnimState.LAND -> {
                torsoBob = -0.2f
                torsoLean = 25f
                legSwingL = -45f
                legSwingR = -45f
                armSwingL = 20f
                armSwingR = 20f
            }
            CharacterAnimState.HURT -> {
                torsoLean = -20f
                torsoBob = 0.1f
                legSwingL = 20f
                legSwingR = 20f
                armSwingL = -40f
                armSwingR = -40f
            }
        }

        matrixStack.push()
        matrixStack.translate(0f, torsoBob, 0f)
        matrixStack.rotate(torsoLean, 1f, 0f, 0f)

        // 1. Torso
        matrixStack.push()
        matrixStack.translate(0f, 1.15f, 0f)
        matrixStack.scale(0.55f, 0.65f, 0.32f)
        uploadMatrices(matrixStack, shader)
        GLES30.glUniform4fv(shader.uColorLocation, 1, pCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, if (isSpeedActive) 0.6f else 0.1f)
        boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        matrixStack.pop()

        // 2. Chest Accent / Hunter/Runner Crest
        matrixStack.push()
        matrixStack.translate(0f, 1.25f, 0.18f)
        matrixStack.scale(0.3f, 0.25f, 0.05f)
        uploadMatrices(matrixStack, shader)
        val roleCol = if (isHunter) floatArrayOf(1f, 0.3f, 0.1f, 1f) else floatArrayOf(0f, 0.9f, 1f, 1f)
        GLES30.glUniform4fv(shader.uColorLocation, 1, roleCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, 0.9f)
        boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        matrixStack.pop()

        // 3. Head & Visor
        matrixStack.push()
        matrixStack.translate(0f, 1.68f, 0f)
        matrixStack.scale(0.38f, 0.38f, 0.38f)
        uploadMatrices(matrixStack, shader)
        GLES30.glUniform4fv(shader.uColorLocation, 1, sCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, 0.05f)
        headMesh.draw(shader.aPositionLocation, shader.aNormalLocation)

        // Visor glow
        matrixStack.translate(0f, 0.05f, 0.52f)
        matrixStack.scale(0.85f, 0.3f, 0.1f)
        uploadMatrices(matrixStack, shader)
        GLES30.glUniform4fv(shader.uColorLocation, 1, aCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, 1.0f)
        headMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        matrixStack.pop()

        // 4. Left Arm
        matrixStack.push()
        matrixStack.translate(-0.38f, 1.35f, 0f)
        matrixStack.rotate(armSwingL, 1f, 0f, 0f)
        matrixStack.translate(0f, -0.32f, 0f)
        uploadMatrices(matrixStack, shader)
        GLES30.glUniform4fv(shader.uColorLocation, 1, sCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, 0.05f)
        limbMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        matrixStack.pop()

        // 5. Right Arm
        matrixStack.push()
        matrixStack.translate(0.38f, 1.35f, 0f)
        matrixStack.rotate(armSwingR, 1f, 0f, 0f)
        matrixStack.translate(0f, -0.32f, 0f)
        uploadMatrices(matrixStack, shader)
        GLES30.glUniform4fv(shader.uColorLocation, 1, sCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, 0.05f)
        limbMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        matrixStack.pop()

        // 6. Left Leg
        matrixStack.push()
        matrixStack.translate(-0.16f, 0.78f, 0f)
        matrixStack.rotate(legSwingL, 1f, 0f, 0f)
        matrixStack.translate(0f, -0.35f, 0f)
        uploadMatrices(matrixStack, shader)
        GLES30.glUniform4fv(shader.uColorLocation, 1, pCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, 0.05f)
        limbMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        matrixStack.pop()

        // 7. Right Leg
        matrixStack.push()
        matrixStack.translate(0.16f, 0.78f, 0f)
        matrixStack.rotate(legSwingR, 1f, 0f, 0f)
        matrixStack.translate(0f, -0.35f, 0f)
        uploadMatrices(matrixStack, shader)
        GLES30.glUniform4fv(shader.uColorLocation, 1, pCol, 0)
        GLES30.glUniform1f(shader.uEmissiveLocation, 0.05f)
        limbMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
        matrixStack.pop()

        // 8. Shield Aura
        if (isShieldActive) {
            matrixStack.push()
            matrixStack.translate(0f, 1.0f, 0f)
            matrixStack.scale(1.4f, 1.6f, 1.4f)
            uploadMatrices(matrixStack, shader)
            val shieldCol = floatArrayOf(0.7f, 0.2f, 1.0f, 0.45f)
            GLES30.glUniform4fv(shader.uColorLocation, 1, shieldCol, 0)
            GLES30.glUniform1f(shader.uEmissiveLocation, 1.2f)
            boxMesh.draw(shader.aPositionLocation, shader.aNormalLocation)
            matrixStack.pop()
        }

        matrixStack.pop()
    }

    private fun uploadMatrices(matrixStack: MatrixStack, shader: GlShader) {
        GLES30.glUniformMatrix4fv(shader.uMMatrixLocation, 1, false, matrixStack.current, 0)
        // Note: uMVPMatrix is updated in renderer using ViewProjection * Model
    }
}
