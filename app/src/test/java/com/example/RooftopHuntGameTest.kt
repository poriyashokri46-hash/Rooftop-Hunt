package com.example

import com.example.rooftophunt.engine3d.CharacterOutfit
import com.example.rooftophunt.gameplay.*
import org.junit.Assert.*
import org.junit.Test

class RooftopHuntGameTest {

    @Test
    fun testAllTenMapsExistWithDistinctFeatures() {
        val maps = MapCatalog.maps
        assertEquals("Must contain exactly 10 rooftop maps", 10, maps.size)

        val names = maps.map { it.name }.toSet()
        assertEquals("Each map must have a unique name", 10, names.size)

        for (map in maps) {
            assertTrue("Map ${map.name} must have buildings", map.buildings.isNotEmpty())
            assertTrue("Map ${map.name} must have obstacles", map.obstacles.isNotEmpty())
            assertTrue("Map ${map.name} must have collectible coins", map.coins.isNotEmpty())
            assertTrue("Map ${map.name} must have powerup spawns", map.powerUps.isNotEmpty())
        }
    }

    @Test
    fun testFourRoundRoleAlternation() {
        val map = MapCatalog.maps[0]
        val engine = GameEngine(
            currentMap = map,
            playerAName = "Alice",
            playerBName = "Bob"
        )

        // Round 1: A=Hunter, B=Runner
        assertEquals(1, engine.currentRound)
        assertEquals(PlayerRole.HUNTER, engine.playerA.role)
        assertEquals(PlayerRole.RUNNER, engine.playerB.role)

        // Round 2: A=Runner, B=Hunter
        engine.proceedToNextRound()
        assertEquals(2, engine.currentRound)
        assertEquals(PlayerRole.RUNNER, engine.playerA.role)
        assertEquals(PlayerRole.HUNTER, engine.playerB.role)

        // Round 3: A=Hunter, B=Runner
        engine.proceedToNextRound()
        assertEquals(3, engine.currentRound)
        assertEquals(PlayerRole.HUNTER, engine.playerA.role)
        assertEquals(PlayerRole.RUNNER, engine.playerB.role)

        // Round 4: A=Runner, B=Hunter
        engine.proceedToNextRound()
        assertEquals(4, engine.currentRound)
        assertEquals(PlayerRole.RUNNER, engine.playerA.role)
        assertEquals(PlayerRole.HUNTER, engine.playerB.role)
    }

    @Test
    fun testHunterRunnerCaptureAndShieldBlock() {
        val physics = ObstaclePhysics()

        val hunter = PlayerState(id = "hunter", role = PlayerRole.HUNTER, x = 0f, y = 10f, z = 0f)
        val runner = PlayerState(id = "runner", role = PlayerRole.RUNNER, x = 0.5f, y = 10f, z = 0.5f)

        // Distance is ~0.7m (< 1.5m) and shield is off -> Capture succeeds!
        val captured = physics.checkHunterRunnerCapture(hunter, runner)
        assertTrue("Hunter should capture Runner when in proximity without shield", captured)

        // Activate Shield on Runner -> Capture must be deflected!
        runner.shieldTimer = 3.0f
        val capturedWithShield = physics.checkHunterRunnerCapture(hunter, runner)
        assertFalse("Runner shield should deflect capture", capturedWithShield)
    }

    @Test
    fun testAbilityActivations() {
        val map = MapCatalog.maps[0]
        val engine = GameEngine(currentMap = map)

        // Speed Boost activation
        val speedSlot = engine.abilitySlots[0]
        speedSlot.isCollected = true
        engine.activateAbility(0)
        assertTrue("Speed boost timer should be 5s", engine.playerA.speedBoostTimer > 4.5f)
        assertFalse("Slot item should be consumed upon activation", speedSlot.isCollected)

        // Shield activation
        val shieldSlot = engine.abilitySlots[1]
        shieldSlot.isCollected = true
        engine.activateAbility(1)
        assertTrue("Shield timer should be 3s", engine.playerA.shieldTimer > 2.5f)
        assertTrue("Shield active check should return true", engine.playerA.isShieldActive)

        // Heal activation
        engine.playerA.health = 50
        val healSlot = engine.abilitySlots[2]
        healSlot.isCollected = true
        engine.activateAbility(2)
        assertEquals("Health should restore +30 HP", 80, engine.playerA.health)
    }
}
