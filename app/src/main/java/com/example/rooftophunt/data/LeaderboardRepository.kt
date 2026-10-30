package com.example.rooftophunt.data

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val avatarEmoji: String,
    val score: Int,
    val tierTitle: String,
    val wins: Int,
    val isLocalPlayer: Boolean = false
)

object LeaderboardRepository {

    fun getRankTier(score: Int): Pair<String, String> {
        return when {
            score >= 3500 -> "Apex Hunter" to "👑"
            score >= 3000 -> "Diamond III" to "💎"
            score >= 2600 -> "Diamond II" to "💎"
            score >= 2200 -> "Platinum III" to "⚡"
            score >= 1800 -> "Platinum I" to "⚡"
            score >= 1400 -> "Gold III" to "🥇"
            score >= 1100 -> "Gold I" to "🥇"
            score >= 800 -> "Silver II" to "🥈"
            score >= 500 -> "Silver I" to "🥈"
            else -> "Bronze I" to "🥉"
        }
    }

    fun getLeaderboard(category: String, userPrefs: UserPreferences): List<LeaderboardEntry> {
        val userScore = when (category) {
            "Total Victories" -> userPrefs.totalWins
            "Most Captures" -> userPrefs.totalCaptures
            "Most Escapes" -> userPrefs.totalEscapes
            "Most Coins" -> userPrefs.totalCoins
            else -> userPrefs.competitiveRankScore
        }

        val entries = mutableListOf(
            LeaderboardEntry(1, "ShadowVolt", "🥷", 4280, "Apex Hunter", 142),
            LeaderboardEntry(2, "ViperRun", "⚡", 3910, "Apex Hunter", 128),
            LeaderboardEntry(3, "NeonGhost", "👻", 3640, "Apex Hunter", 115),
            LeaderboardEntry(4, "CyberPulse", "🤖", 3210, "Diamond III", 98),
            LeaderboardEntry(5, "RooftopKing", "👑", 2950, "Diamond II", 89),
            LeaderboardEntry(6, "StormLeaper", "🌪️", 2720, "Diamond I", 76),
            LeaderboardEntry(7, "MirageEdge", "✨", 2430, "Platinum III", 64),
            LeaderboardEntry(8, "ZenithHunter", "🎯", 2180, "Platinum II", 58),
            LeaderboardEntry(9, "ApexFalcon", "🦅", 1950, "Platinum I", 52),
            LeaderboardEntry(10, "VoltSkater", "🛹", 1720, "Gold III", 45)
        )

        // Insert local player into appropriate rank position
        val userTier = getRankTier(userScore).first
        val userEntry = LeaderboardEntry(
            rank = 11,
            username = userPrefs.username,
            avatarEmoji = "🏃",
            score = userScore,
            tierTitle = userTier,
            wins = userPrefs.totalWins,
            isLocalPlayer = true
        )

        val combined = (entries + userEntry).sortedByDescending { it.score }
        return combined.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }
}
