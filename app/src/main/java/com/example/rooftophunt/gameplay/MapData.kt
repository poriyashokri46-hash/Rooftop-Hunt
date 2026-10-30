package com.example.rooftophunt.gameplay

enum class ObstacleType {
    CONCRETE_BARRIER,
    PIPE,
    ROOFTOP_WALL,
    CHIMNEY,
    ROTATING_BLADE,
    MOVING_PLATFORM,
    SWINGING_BARRIER,
    SLIDING_WALL,
    LASER_BARRIER,
    FALLING_PLATFORM,
    MOVING_CRANE,
    STEAM_VENT
}

data class BuildingPlatform(
    val x: Float,
    val y: Float,
    val z: Float,
    val width: Float,
    val height: Float,
    val depth: Float,
    val color: FloatArray,
    val topColor: FloatArray
)

data class Obstacle(
    val id: Int,
    val type: ObstacleType,
    var x: Float,
    var y: Float,
    var z: Float,
    val width: Float,
    val height: Float,
    val depth: Float,
    val color: FloatArray,
    val damage: Int = 15,
    val isMoving: Boolean = false,
    val moveAxis: Int = 0, // 0 = X, 1 = Y, 2 = Z, 3 = Rotation
    val moveSpeed: Float = 0f,
    val moveDistance: Float = 0f,
    var currentPhase: Float = 0f,
    var isTriggered: Boolean = false,
    var fallVelocity: Float = 0f
)

data class CoinItem(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    var isCollected: Boolean = false
)

data class PowerUpItem(
    val id: Int,
    val type: SpecialAbility,
    val x: Float,
    val y: Float,
    val z: Float,
    var isCollected: Boolean = false,
    var respawnTimer: Float = 0f
)

data class RooftopMap(
    val id: Int,
    val name: String,
    val description: String,
    val skyColorTop: FloatArray,
    val skyColorBottom: FloatArray,
    val fogColor: FloatArray,
    val fogDensity: Float,
    val lightDir: FloatArray,
    val lightColor: FloatArray,
    val buildings: List<BuildingPlatform>,
    val obstacles: List<Obstacle>,
    val coins: List<CoinItem>,
    val powerUps: List<PowerUpItem>,
    val frictionModifier: Float = 1.0f, // For slippery rain / ice
    val weatherType: String = "CLEAR" // CLEAR, RAIN, SNOW, SANDSTORM, NEON_FOG
)

object MapCatalog {
    val maps: List<RooftopMap> by lazy {
        listOf(
            createFuturisticCityMap(),
            createRainyRooftopsMap(),
            createDesertCityMap(),
            createIndustrialZoneMap(),
            createGlassTowersMap(),
            createNeonNightCityMap(),
            createOldEuropeanCityMap(),
            createConstructionZoneMap(),
            createFrozenCityMap(),
            createFloatingTowersMap()
        )
    }

    private fun createFuturisticCityMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        // Cyber skyline with neon skyscrapers
        b.add(BuildingPlatform(0f, 0f, 0f, 24f, 20f, 24f, floatArrayOf(0.08f, 0.1f, 0.15f, 1f), floatArrayOf(0.12f, 0.16f, 0.22f, 1f)))
        b.add(BuildingPlatform(0f, 2f, 32f, 22f, 22f, 22f, floatArrayOf(0.1f, 0.12f, 0.18f, 1f), floatArrayOf(0.14f, 0.18f, 0.25f, 1f)))
        b.add(BuildingPlatform(28f, 4f, 32f, 20f, 24f, 20f, floatArrayOf(0.09f, 0.11f, 0.16f, 1f), floatArrayOf(0.13f, 0.17f, 0.24f, 1f)))
        b.add(BuildingPlatform(28f, -2f, 0f, 24f, 18f, 22f, floatArrayOf(0.08f, 0.1f, 0.15f, 1f), floatArrayOf(0.12f, 0.15f, 0.21f, 1f)))
        // Connecting rooftop bridges
        b.add(BuildingPlatform(0f, 1f, 14f, 6f, 2f, 8f, floatArrayOf(0.0f, 0.7f, 0.8f, 1f), floatArrayOf(0f, 0.9f, 1f, 1f)))
        b.add(BuildingPlatform(15f, 3f, 32f, 8f, 2f, 5f, floatArrayOf(0.0f, 0.7f, 0.8f, 1f), floatArrayOf(0f, 0.9f, 1f, 1f)))
        b.add(BuildingPlatform(28f, 1f, 14f, 5f, 2f, 8f, floatArrayOf(0.0f, 0.7f, 0.8f, 1f), floatArrayOf(0f, 0.9f, 1f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Laser barriers and moving platforms
        obs.add(Obstacle(1, ObstacleType.LASER_BARRIER, 0f, 11f, 5f, 12f, 2.5f, 0.5f, floatArrayOf(0f, 1f, 0.85f, 0.8f), damage = 20))
        obs.add(Obstacle(2, ObstacleType.MOVING_PLATFORM, 14f, 11f, 0f, 5f, 1f, 5f, floatArrayOf(0.2f, 0.8f, 0.9f, 1f), damage = 0, isMoving = true, moveAxis = 0, moveSpeed = 3.5f, moveDistance = 7f))
        obs.add(Obstacle(3, ObstacleType.ROTATING_BLADE, 28f, 15f, 32f, 4f, 1f, 4f, floatArrayOf(1f, 0.2f, 0.2f, 1f), damage = 25, isMoving = true, moveAxis = 3, moveSpeed = 120f))
        obs.add(Obstacle(4, ObstacleType.CONCRETE_BARRIER, -4f, 11f, -4f, 3f, 1.5f, 1f, floatArrayOf(0.2f, 0.25f, 0.35f, 1f), damage = 10))
        obs.add(Obstacle(5, ObstacleType.CONCRETE_BARRIER, 4f, 11f, -4f, 3f, 1.5f, 1f, floatArrayOf(0.2f, 0.25f, 0.35f, 1f), damage = 10))

        val coins = listOf(
            CoinItem(1, 0f, 11.5f, 14f),
            CoinItem(2, 14f, 12f, 0f),
            CoinItem(3, 28f, 13f, 14f),
            CoinItem(4, 28f, 15.5f, 32f),
            CoinItem(5, -6f, 11.5f, 32f),
            CoinItem(6, 6f, 11.5f, 32f),
            CoinItem(7, 0f, 11.5f, -6f)
        )

        val powerUps = listOf(
            PowerUpItem(1, SpecialAbility.SPEED_BOOST, 0f, 12f, 2f),
            PowerUpItem(2, SpecialAbility.SHIELD, 28f, 16f, 34f),
            PowerUpItem(3, SpecialAbility.HEAL, 28f, 11f, 0f)
        )

        return RooftopMap(
            id = 1,
            name = "Futuristic City",
            description = "Neon skyscrapers with high-tech laser barriers and shifting platforms.",
            skyColorTop = floatArrayOf(0.04f, 0.05f, 0.12f),
            skyColorBottom = floatArrayOf(0.08f, 0.15f, 0.28f),
            fogColor = floatArrayOf(0.06f, 0.12f, 0.22f),
            fogDensity = 0.015f,
            lightDir = floatArrayOf(0.4f, 0.9f, 0.3f),
            lightColor = floatArrayOf(0.8f, 0.95f, 1.0f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "NEON_FOG"
        )
    }

    private fun createRainyRooftopsMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 26f, 18f, 26f, floatArrayOf(0.06f, 0.08f, 0.1f, 1f), floatArrayOf(0.1f, 0.14f, 0.16f, 1f)))
        b.add(BuildingPlatform(0f, -2f, 34f, 24f, 16f, 22f, floatArrayOf(0.07f, 0.09f, 0.11f, 1f), floatArrayOf(0.11f, 0.15f, 0.18f, 1f)))
        b.add(BuildingPlatform(32f, 1f, 34f, 24f, 19f, 24f, floatArrayOf(0.06f, 0.08f, 0.1f, 1f), floatArrayOf(0.12f, 0.16f, 0.19f, 1f)))
        b.add(BuildingPlatform(32f, 3f, 0f, 22f, 21f, 22f, floatArrayOf(0.08f, 0.1f, 0.12f, 1f), floatArrayOf(0.13f, 0.17f, 0.2f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Slippery pipes and swinging barriers
        obs.add(Obstacle(11, ObstacleType.PIPE, 0f, 10f, 15f, 16f, 1.2f, 1.2f, floatArrayOf(0.3f, 0.4f, 0.45f, 1f), damage = 10))
        obs.add(Obstacle(12, ObstacleType.SWINGING_BARRIER, 16f, 11f, 34f, 1f, 3f, 6f, floatArrayOf(0.9f, 0.5f, 0.1f, 1f), damage = 20, isMoving = true, moveAxis = 3, moveSpeed = 90f))
        obs.add(Obstacle(13, ObstacleType.ROOFTOP_WALL, 32f, 12f, 16f, 4f, 2f, 1f, floatArrayOf(0.2f, 0.22f, 0.26f, 1f), damage = 10))

        val coins = listOf(
            CoinItem(11, 0f, 11f, 15f),
            CoinItem(12, 16f, 12f, 34f),
            CoinItem(13, 32f, 13f, 16f),
            CoinItem(14, 0f, 10f, 34f),
            CoinItem(15, 32f, 14.5f, 0f)
        )
        val powerUps = listOf(
            PowerUpItem(11, SpecialAbility.SPEED_BOOST, 32f, 14.5f, 0f),
            PowerUpItem(12, SpecialAbility.HEAL, 0f, 10f, 34f),
            PowerUpItem(13, SpecialAbility.SHIELD, 16f, 11f, 17f)
        )

        return RooftopMap(
            id = 2,
            name = "Rainy Rooftops",
            description = "Slick, wet rooftops under torrential downpours with reduced grip.",
            skyColorTop = floatArrayOf(0.05f, 0.08f, 0.12f),
            skyColorBottom = floatArrayOf(0.12f, 0.16f, 0.22f),
            fogColor = floatArrayOf(0.1f, 0.14f, 0.19f),
            fogDensity = 0.025f,
            lightDir = floatArrayOf(0.2f, 0.8f, 0.5f),
            lightColor = floatArrayOf(0.6f, 0.75f, 0.9f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            frictionModifier = 0.75f,
            weatherType = "RAIN"
        )
    }

    private fun createDesertCityMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 20f, 18f, 20f, floatArrayOf(0.25f, 0.18f, 0.12f, 1f), floatArrayOf(0.38f, 0.28f, 0.18f, 1f)))
        b.add(BuildingPlatform(0f, -4f, 38f, 22f, 14f, 22f, floatArrayOf(0.28f, 0.2f, 0.13f, 1f), floatArrayOf(0.42f, 0.31f, 0.2f, 1f)))
        b.add(BuildingPlatform(36f, 2f, 38f, 18f, 20f, 18f, floatArrayOf(0.24f, 0.17f, 0.11f, 1f), floatArrayOf(0.36f, 0.26f, 0.17f, 1f)))
        b.add(BuildingPlatform(36f, 5f, 0f, 20f, 23f, 20f, floatArrayOf(0.26f, 0.19f, 0.12f, 1f), floatArrayOf(0.39f, 0.29f, 0.19f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Long gaps with falling platforms
        obs.add(Obstacle(21, ObstacleType.FALLING_PLATFORM, 0f, 8f, 19f, 4f, 0.8f, 4f, floatArrayOf(0.45f, 0.35f, 0.25f, 1f), damage = 0))
        obs.add(Obstacle(22, ObstacleType.FALLING_PLATFORM, 18f, 9f, 38f, 4f, 0.8f, 4f, floatArrayOf(0.45f, 0.35f, 0.25f, 1f), damage = 0))
        obs.add(Obstacle(23, ObstacleType.CONCRETE_BARRIER, 36f, 17.5f, 0f, 6f, 1.5f, 1f, floatArrayOf(0.5f, 0.4f, 0.3f, 1f), damage = 10))

        val coins = listOf(
            CoinItem(21, 0f, 9.5f, 19f),
            CoinItem(22, 18f, 10.5f, 38f),
            CoinItem(23, 36f, 18.5f, 0f),
            CoinItem(24, 0f, 8f, 38f),
            CoinItem(25, 36f, 13f, 38f)
        )
        val powerUps = listOf(
            PowerUpItem(21, SpecialAbility.SPEED_BOOST, 0f, 8.5f, 38f),
            PowerUpItem(22, SpecialAbility.SHIELD, 36f, 13.5f, 38f),
            PowerUpItem(23, SpecialAbility.HEAL, 0f, 10.5f, 0f)
        )

        return RooftopMap(
            id = 3,
            name = "Desert City",
            description = "Dust-blown stone rooftops separated by huge gaps and collapsing ledges.",
            skyColorTop = floatArrayOf(0.22f, 0.14f, 0.08f),
            skyColorBottom = floatArrayOf(0.45f, 0.32f, 0.18f),
            fogColor = floatArrayOf(0.38f, 0.28f, 0.16f),
            fogDensity = 0.022f,
            lightDir = floatArrayOf(0.7f, 0.7f, 0.1f),
            lightColor = floatArrayOf(1.0f, 0.88f, 0.65f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "SANDSTORM"
        )
    }

    private fun createIndustrialZoneMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 28f, 16f, 22f, floatArrayOf(0.12f, 0.12f, 0.13f, 1f), floatArrayOf(0.2f, 0.21f, 0.22f, 1f)))
        b.add(BuildingPlatform(0f, 3f, 30f, 24f, 19f, 24f, floatArrayOf(0.11f, 0.11f, 0.12f, 1f), floatArrayOf(0.19f, 0.2f, 0.21f, 1f)))
        b.add(BuildingPlatform(32f, 0f, 30f, 24f, 16f, 24f, floatArrayOf(0.13f, 0.12f, 0.11f, 1f), floatArrayOf(0.22f, 0.2f, 0.18f, 1f)))
        b.add(BuildingPlatform(32f, 4f, 0f, 26f, 20f, 22f, floatArrayOf(0.12f, 0.13f, 0.14f, 1f), floatArrayOf(0.21f, 0.22f, 0.23f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Rotating machinery, steam vents, and moving cranes
        obs.add(Obstacle(31, ObstacleType.STEAM_VENT, 0f, 9f, 0f, 2f, 3f, 2f, floatArrayOf(0.9f, 0.9f, 0.9f, 0.6f), damage = 15))
        obs.add(Obstacle(32, ObstacleType.ROTATING_BLADE, 0f, 13f, 30f, 6f, 1f, 6f, floatArrayOf(0.9f, 0.2f, 0.1f, 1f), damage = 25, isMoving = true, moveAxis = 3, moveSpeed = 100f))
        obs.add(Obstacle(33, ObstacleType.MOVING_CRANE, 16f, 15f, 15f, 14f, 2f, 2f, floatArrayOf(1.0f, 0.7f, 0.0f, 1f), damage = 20, isMoving = true, moveAxis = 3, moveSpeed = 40f))
        obs.add(Obstacle(34, ObstacleType.PIPE, 32f, 9f, 15f, 1.5f, 1.5f, 14f, floatArrayOf(0.4f, 0.35f, 0.3f, 1f), damage = 10))

        val coins = listOf(
            CoinItem(31, 0f, 10.5f, 8f),
            CoinItem(32, 16f, 16f, 15f),
            CoinItem(33, 32f, 9.5f, 15f),
            CoinItem(34, 32f, 15f, 0f),
            CoinItem(35, 0f, 14.5f, 30f)
        )
        val powerUps = listOf(
            PowerUpItem(31, SpecialAbility.SPEED_BOOST, 32f, 15f, 0f),
            PowerUpItem(32, SpecialAbility.SHIELD, 0f, 14f, 30f),
            PowerUpItem(33, SpecialAbility.HEAL, 32f, 9.5f, 30f)
        )

        return RooftopMap(
            id = 4,
            name = "Industrial Zone",
            description = "Smokestacks, spinning turbines, pressurized steam valves and giant cranes.",
            skyColorTop = floatArrayOf(0.12f, 0.08f, 0.06f),
            skyColorBottom = floatArrayOf(0.28f, 0.18f, 0.12f),
            fogColor = floatArrayOf(0.24f, 0.16f, 0.1f),
            fogDensity = 0.02f,
            lightDir = floatArrayOf(0.5f, 0.8f, 0.2f),
            lightColor = floatArrayOf(1.0f, 0.75f, 0.5f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "CLEAR"
        )
    }

    private fun createGlassTowersMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 18f, 22f, 18f, floatArrayOf(0.1f, 0.25f, 0.35f, 0.9f), floatArrayOf(0.2f, 0.45f, 0.6f, 0.95f)))
        b.add(BuildingPlatform(0f, 4f, 30f, 16f, 26f, 16f, floatArrayOf(0.1f, 0.25f, 0.35f, 0.9f), floatArrayOf(0.2f, 0.45f, 0.6f, 0.95f)))
        b.add(BuildingPlatform(28f, 6f, 30f, 18f, 28f, 18f, floatArrayOf(0.1f, 0.25f, 0.35f, 0.9f), floatArrayOf(0.2f, 0.45f, 0.6f, 0.95f)))
        b.add(BuildingPlatform(28f, 2f, 0f, 16f, 24f, 16f, floatArrayOf(0.1f, 0.25f, 0.35f, 0.9f), floatArrayOf(0.2f, 0.45f, 0.6f, 0.95f)))
        // Fragile glass skybridges
        b.add(BuildingPlatform(0f, 2f, 15f, 4f, 0.5f, 12f, floatArrayOf(0.3f, 0.7f, 0.9f, 0.6f), floatArrayOf(0.5f, 0.9f, 1.0f, 0.7f)))
        b.add(BuildingPlatform(14f, 5f, 30f, 12f, 0.5f, 4f, floatArrayOf(0.3f, 0.7f, 0.9f, 0.6f), floatArrayOf(0.5f, 0.9f, 1.0f, 0.7f)))
        b.add(BuildingPlatform(28f, 4f, 15f, 4f, 0.5f, 12f, floatArrayOf(0.3f, 0.7f, 0.9f, 0.6f), floatArrayOf(0.5f, 0.9f, 1.0f, 0.7f)))

        val obs = mutableListOf<Obstacle>()
        obs.add(Obstacle(41, ObstacleType.SLIDING_WALL, 0f, 18f, 15f, 4f, 3f, 0.5f, floatArrayOf(0.1f, 0.9f, 1.0f, 0.8f), damage = 15, isMoving = true, moveAxis = 1, moveSpeed = 2.5f, moveDistance = 3f))
        obs.add(Obstacle(42, ObstacleType.FALLING_PLATFORM, 14f, 18f, 30f, 3f, 0.5f, 3f, floatArrayOf(0.4f, 0.8f, 1f, 0.5f), damage = 0))

        val coins = listOf(
            CoinItem(41, 0f, 13f, 15f),
            CoinItem(42, 14f, 19f, 30f),
            CoinItem(43, 28f, 15f, 15f),
            CoinItem(44, 28f, 21f, 30f),
            CoinItem(45, 0f, 18f, 30f)
        )
        val powerUps = listOf(
            PowerUpItem(41, SpecialAbility.SPEED_BOOST, 0f, 12f, 0f),
            PowerUpItem(42, SpecialAbility.SHIELD, 28f, 21f, 30f),
            PowerUpItem(43, SpecialAbility.HEAL, 28f, 15f, 0f)
        )

        return RooftopMap(
            id = 5,
            name = "Glass Towers",
            description = "Reflective mirror spires, narrow transparent bridges, and vertigo drops.",
            skyColorTop = floatArrayOf(0.02f, 0.08f, 0.16f),
            skyColorBottom = floatArrayOf(0.1f, 0.35f, 0.55f),
            fogColor = floatArrayOf(0.08f, 0.28f, 0.44f),
            fogDensity = 0.012f,
            lightDir = floatArrayOf(0.3f, 0.9f, 0.4f),
            lightColor = floatArrayOf(0.9f, 0.98f, 1.0f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "CLEAR"
        )
    }

    private fun createNeonNightCityMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 22f, 20f, 22f, floatArrayOf(0.05f, 0.05f, 0.09f, 1f), floatArrayOf(0.09f, 0.08f, 0.16f, 1f)))
        b.add(BuildingPlatform(0f, -2f, 32f, 20f, 18f, 22f, floatArrayOf(0.06f, 0.04f, 0.1f, 1f), floatArrayOf(0.11f, 0.07f, 0.18f, 1f)))
        b.add(BuildingPlatform(30f, 3f, 32f, 22f, 23f, 22f, floatArrayOf(0.05f, 0.05f, 0.09f, 1f), floatArrayOf(0.09f, 0.08f, 0.16f, 1f)))
        b.add(BuildingPlatform(30f, 1f, 0f, 24f, 21f, 22f, floatArrayOf(0.06f, 0.04f, 0.1f, 1f), floatArrayOf(0.11f, 0.07f, 0.18f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Moving billboard advertising boards & light barriers
        obs.add(Obstacle(51, ObstacleType.SLIDING_WALL, 0f, 11f, 16f, 8f, 4f, 0.6f, floatArrayOf(1.0f, 0.0f, 0.6f, 0.9f), damage = 20, isMoving = true, moveAxis = 0, moveSpeed = 4f, moveDistance = 6f))
        obs.add(Obstacle(52, ObstacleType.LASER_BARRIER, 30f, 13f, 16f, 10f, 2f, 0.5f, floatArrayOf(0.6f, 0.0f, 1.0f, 0.85f), damage = 20))
        obs.add(Obstacle(53, ObstacleType.SWINGING_BARRIER, 15f, 12f, 32f, 1f, 3f, 5f, floatArrayOf(0f, 1f, 0.8f, 1f), damage = 15, isMoving = true, moveAxis = 3, moveSpeed = 110f))

        val coins = listOf(
            CoinItem(51, 0f, 13f, 16f),
            CoinItem(52, 15f, 12.5f, 32f),
            CoinItem(53, 30f, 14f, 16f),
            CoinItem(54, 30f, 15f, 32f),
            CoinItem(55, 0f, 11.5f, 0f)
        )
        val powerUps = listOf(
            PowerUpItem(51, SpecialAbility.SPEED_BOOST, 0f, 11.5f, 0f),
            PowerUpItem(52, SpecialAbility.SHIELD, 30f, 15.5f, 32f),
            PowerUpItem(53, SpecialAbility.HEAL, 0f, 10f, 32f)
        )

        return RooftopMap(
            id = 6,
            name = "Neon Night City",
            description = "Electrified metropolis flooded with holographic ads and high-voltage lasers.",
            skyColorTop = floatArrayOf(0.04f, 0.01f, 0.08f),
            skyColorBottom = floatArrayOf(0.18f, 0.04f, 0.28f),
            fogColor = floatArrayOf(0.14f, 0.03f, 0.22f),
            fogDensity = 0.016f,
            lightDir = floatArrayOf(0.3f, 0.9f, 0.2f),
            lightColor = floatArrayOf(1.0f, 0.5f, 0.9f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "NEON_FOG"
        )
    }

    private fun createOldEuropeanCityMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 24f, 16f, 20f, floatArrayOf(0.22f, 0.12f, 0.1f, 1f), floatArrayOf(0.45f, 0.2f, 0.15f, 1f)))
        b.add(BuildingPlatform(0f, 2f, 28f, 22f, 18f, 20f, floatArrayOf(0.2f, 0.11f, 0.09f, 1f), floatArrayOf(0.42f, 0.19f, 0.14f, 1f)))
        b.add(BuildingPlatform(28f, -1f, 28f, 20f, 15f, 20f, floatArrayOf(0.21f, 0.13f, 0.1f, 1f), floatArrayOf(0.44f, 0.21f, 0.16f, 1f)))
        b.add(BuildingPlatform(28f, 3f, 0f, 22f, 19f, 22f, floatArrayOf(0.19f, 0.1f, 0.08f, 1f), floatArrayOf(0.4f, 0.18f, 0.13f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Chimneys, brick walls, and tight passages
        obs.add(Obstacle(61, ObstacleType.CHIMNEY, -4f, 9.5f, 0f, 1.5f, 3f, 1.5f, floatArrayOf(0.35f, 0.15f, 0.1f, 1f), damage = 10))
        obs.add(Obstacle(62, ObstacleType.CHIMNEY, 4f, 9.5f, 0f, 1.5f, 3f, 1.5f, floatArrayOf(0.35f, 0.15f, 0.1f, 1f), damage = 10))
        obs.add(Obstacle(63, ObstacleType.CHIMNEY, 0f, 12f, 28f, 1.5f, 3.5f, 1.5f, floatArrayOf(0.35f, 0.15f, 0.1f, 1f), damage = 10))
        obs.add(Obstacle(64, ObstacleType.ROOFTOP_WALL, 28f, 8.5f, 20f, 12f, 2f, 0.8f, floatArrayOf(0.3f, 0.18f, 0.15f, 1f), damage = 10))

        val coins = listOf(
            CoinItem(61, -4f, 12.5f, 0f),
            CoinItem(62, 4f, 12.5f, 0f),
            CoinItem(63, 0f, 15f, 28f),
            CoinItem(64, 14f, 10f, 14f),
            CoinItem(65, 28f, 14.5f, 0f)
        )
        val powerUps = listOf(
            PowerUpItem(61, SpecialAbility.SPEED_BOOST, 28f, 14f, 0f),
            PowerUpItem(62, SpecialAbility.SHIELD, 0f, 12f, 28f),
            PowerUpItem(63, SpecialAbility.HEAL, 28f, 8.5f, 28f)
        )

        return RooftopMap(
            id = 7,
            name = "Old European City",
            description = "Cobblestone alleys beneath steep clay-tile gables, dormers, and brick chimneys.",
            skyColorTop = floatArrayOf(0.08f, 0.1f, 0.18f),
            skyColorBottom = floatArrayOf(0.32f, 0.22f, 0.26f),
            fogColor = floatArrayOf(0.24f, 0.18f, 0.22f),
            fogDensity = 0.018f,
            lightDir = floatArrayOf(0.6f, 0.7f, 0.3f),
            lightColor = floatArrayOf(1.0f, 0.85f, 0.7f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "CLEAR"
        )
    }

    private fun createConstructionZoneMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 24f, 17f, 22f, floatArrayOf(0.18f, 0.18f, 0.18f, 1f), floatArrayOf(0.3f, 0.3f, 0.32f, 1f)))
        b.add(BuildingPlatform(0f, 4f, 32f, 20f, 21f, 22f, floatArrayOf(0.16f, 0.16f, 0.18f, 1f), floatArrayOf(0.28f, 0.28f, 0.3f, 1f)))
        b.add(BuildingPlatform(30f, 1f, 32f, 22f, 18f, 22f, floatArrayOf(0.17f, 0.17f, 0.19f, 1f), floatArrayOf(0.29f, 0.29f, 0.31f, 1f)))
        b.add(BuildingPlatform(30f, -2f, 0f, 24f, 15f, 22f, floatArrayOf(0.18f, 0.18f, 0.18f, 1f), floatArrayOf(0.3f, 0.3f, 0.32f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Scaffolding, steel beams, rotating crane, swinging hazard
        obs.add(Obstacle(71, ObstacleType.MOVING_CRANE, 15f, 16f, 16f, 18f, 2.5f, 2f, floatArrayOf(1f, 0.8f, 0f, 1f), damage = 25, isMoving = true, moveAxis = 3, moveSpeed = 50f))
        obs.add(Obstacle(72, ObstacleType.SWINGING_BARRIER, 0f, 13f, 16f, 2f, 4f, 2f, floatArrayOf(0.8f, 0.1f, 0.1f, 1f), damage = 20, isMoving = true, moveAxis = 3, moveSpeed = 85f))
        obs.add(Obstacle(73, ObstacleType.CONCRETE_BARRIER, 30f, 11f, 32f, 6f, 2f, 1.5f, floatArrayOf(0.4f, 0.4f, 0.42f, 1f), damage = 10))

        val coins = listOf(
            CoinItem(71, 15f, 18f, 16f),
            CoinItem(72, 0f, 14f, 16f),
            CoinItem(73, 30f, 12f, 32f),
            CoinItem(74, 30f, 9.5f, 0f),
            CoinItem(75, 0f, 13.5f, 32f)
        )
        val powerUps = listOf(
            PowerUpItem(71, SpecialAbility.SPEED_BOOST, 0f, 13.5f, 32f),
            PowerUpItem(72, SpecialAbility.SHIELD, 30f, 12f, 32f),
            PowerUpItem(73, SpecialAbility.HEAL, 0f, 9.5f, 0f)
        )

        return RooftopMap(
            id = 8,
            name = "Construction Zone",
            description = "Unfinished skyscraper summits flanked by swinging iron girders and tower cranes.",
            skyColorTop = floatArrayOf(0.06f, 0.08f, 0.12f),
            skyColorBottom = floatArrayOf(0.2f, 0.22f, 0.28f),
            fogColor = floatArrayOf(0.18f, 0.19f, 0.24f),
            fogDensity = 0.016f,
            lightDir = floatArrayOf(0.4f, 0.8f, 0.4f),
            lightColor = floatArrayOf(0.95f, 0.95f, 0.85f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "CLEAR"
        )
    }

    private fun createFrozenCityMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        b.add(BuildingPlatform(0f, 0f, 0f, 22f, 19f, 22f, floatArrayOf(0.1f, 0.16f, 0.25f, 1f), floatArrayOf(0.7f, 0.85f, 0.95f, 1f)))
        b.add(BuildingPlatform(0f, -2f, 30f, 20f, 17f, 22f, floatArrayOf(0.12f, 0.18f, 0.28f, 1f), floatArrayOf(0.75f, 0.9f, 1.0f, 1f)))
        b.add(BuildingPlatform(28f, 2f, 30f, 22f, 21f, 22f, floatArrayOf(0.09f, 0.15f, 0.24f, 1f), floatArrayOf(0.68f, 0.82f, 0.92f, 1f)))
        b.add(BuildingPlatform(28f, 0f, 0f, 22f, 19f, 22f, floatArrayOf(0.11f, 0.17f, 0.26f, 1f), floatArrayOf(0.72f, 0.86f, 0.96f, 1f)))

        val obs = mutableListOf<Obstacle>()
        // Icy barriers and swinging frost blades
        obs.add(Obstacle(81, ObstacleType.ROTATING_BLADE, 0f, 11f, 15f, 5f, 1f, 5f, floatArrayOf(0.4f, 0.85f, 1.0f, 1f), damage = 25, isMoving = true, moveAxis = 3, moveSpeed = 110f))
        obs.add(Obstacle(82, ObstacleType.CONCRETE_BARRIER, 14f, 11f, 30f, 2f, 2f, 2f, floatArrayOf(0.5f, 0.7f, 0.9f, 1f), damage = 10))
        obs.add(Obstacle(83, ObstacleType.SLIDING_WALL, 28f, 13f, 15f, 4f, 3f, 1f, floatArrayOf(0.6f, 0.9f, 1f, 0.85f), damage = 20, isMoving = true, moveAxis = 0, moveSpeed = 3f, moveDistance = 5f))

        val coins = listOf(
            CoinItem(81, 0f, 13f, 15f),
            CoinItem(82, 14f, 12f, 30f),
            CoinItem(83, 28f, 15f, 15f),
            CoinItem(84, 0f, 9.5f, 30f),
            CoinItem(85, 28f, 12f, 0f)
        )
        val powerUps = listOf(
            PowerUpItem(81, SpecialAbility.SPEED_BOOST, 28f, 12f, 0f),
            PowerUpItem(82, SpecialAbility.SHIELD, 0f, 9.5f, 30f),
            PowerUpItem(83, SpecialAbility.HEAL, 28f, 13f, 30f)
        )

        return RooftopMap(
            id = 9,
            name = "Frozen City",
            description = "Sub-zero ice crusts on sheer rooftops with blizzard gusts and frosty hazards.",
            skyColorTop = floatArrayOf(0.05f, 0.08f, 0.16f),
            skyColorBottom = floatArrayOf(0.2f, 0.35f, 0.5f),
            fogColor = floatArrayOf(0.28f, 0.42f, 0.58f),
            fogDensity = 0.024f,
            lightDir = floatArrayOf(0.2f, 0.9f, 0.3f),
            lightColor = floatArrayOf(0.85f, 0.95f, 1.0f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            frictionModifier = 0.6f, // Extra slippery!
            weatherType = "SNOW"
        )
    }

    private fun createFloatingTowersMap(): RooftopMap {
        val b = mutableListOf<BuildingPlatform>()
        // Detached floating pillars and suspended stepping blocks
        b.add(BuildingPlatform(0f, 0f, 0f, 14f, 25f, 14f, floatArrayOf(0.12f, 0.08f, 0.22f, 1f), floatArrayOf(0.35f, 0.25f, 0.65f, 1f)))
        b.add(BuildingPlatform(0f, 3f, 26f, 14f, 28f, 14f, floatArrayOf(0.14f, 0.09f, 0.25f, 1f), floatArrayOf(0.38f, 0.28f, 0.7f, 1f)))
        b.add(BuildingPlatform(26f, 5f, 26f, 14f, 30f, 14f, floatArrayOf(0.12f, 0.08f, 0.22f, 1f), floatArrayOf(0.35f, 0.25f, 0.65f, 1f)))
        b.add(BuildingPlatform(26f, 1f, 0f, 14f, 26f, 14f, floatArrayOf(0.15f, 0.1f, 0.27f, 1f), floatArrayOf(0.4f, 0.3f, 0.75f, 1f)))
        // High-wire parkour stepping pads
        b.add(BuildingPlatform(0f, 1.5f, 13f, 4f, 1f, 4f, floatArrayOf(0.8f, 0.2f, 1f, 1f), floatArrayOf(1f, 0.4f, 1f, 1f)))
        b.add(BuildingPlatform(13f, 4f, 26f, 4f, 1f, 4f, floatArrayOf(0.8f, 0.2f, 1f, 1f), floatArrayOf(1f, 0.4f, 1f, 1f)))
        b.add(BuildingPlatform(26f, 3f, 13f, 4f, 1f, 4f, floatArrayOf(0.8f, 0.2f, 1f, 1f), floatArrayOf(1f, 0.4f, 1f, 1f)))

        val obs = mutableListOf<Obstacle>()
        obs.add(Obstacle(91, ObstacleType.MOVING_PLATFORM, 13f, 14f, 0f, 5f, 1f, 5f, floatArrayOf(0.9f, 0.3f, 1f, 1f), damage = 0, isMoving = true, moveAxis = 0, moveSpeed = 4f, moveDistance = 7f))
        obs.add(Obstacle(92, ObstacleType.ROTATING_BLADE, 26f, 21f, 26f, 5f, 1f, 5f, floatArrayOf(1f, 0.1f, 0.4f, 1f), damage = 30, isMoving = true, moveAxis = 3, moveSpeed = 140f))

        val coins = listOf(
            CoinItem(91, 0f, 14.5f, 13f),
            CoinItem(92, 13f, 17f, 26f),
            CoinItem(93, 26f, 16f, 13f),
            CoinItem(94, 13f, 16f, 0f),
            CoinItem(95, 26f, 22.5f, 26f)
        )
        val powerUps = listOf(
            PowerUpItem(91, SpecialAbility.SPEED_BOOST, 0f, 14f, 0f),
            PowerUpItem(92, SpecialAbility.SHIELD, 26f, 22f, 26f),
            PowerUpItem(93, SpecialAbility.HEAL, 0f, 16.5f, 26f)
        )

        return RooftopMap(
            id = 10,
            name = "Floating Towers",
            description = "Anti-gravity pinnacles suspended thousands of feet in the air above an endless sky.",
            skyColorTop = floatArrayOf(0.08f, 0.02f, 0.18f),
            skyColorBottom = floatArrayOf(0.25f, 0.08f, 0.45f),
            fogColor = floatArrayOf(0.2f, 0.06f, 0.38f),
            fogDensity = 0.012f,
            lightDir = floatArrayOf(0.5f, 0.8f, 0.3f),
            lightColor = floatArrayOf(1.0f, 0.7f, 1.0f),
            buildings = b,
            obstacles = obs,
            coins = coins,
            powerUps = powerUps,
            weatherType = "NEON_FOG"
        )
    }
}
