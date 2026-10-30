package com.example.rooftophunt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rooftophunt.data.LeaderboardRepository
import com.example.rooftophunt.data.UserPreferences
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    userPrefs: UserPreferences,
    lang: String,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf(userPrefs.username) }
    var userCoins by remember { mutableStateOf(userPrefs.totalCoins) }
    var selectedAvatarIdx by remember { mutableStateOf(userPrefs.avatarId) }
    var canClaimDaily by remember { mutableStateOf(userPrefs.canClaimDailyReward) }

    val avatars = listOf("🏃", "🥷", "⚡", "🤖", "👑", "🐺", "🔥", "🦅")
    val (tierName, tierBadge) = LeaderboardRepository.getRankTier(userPrefs.competitiveRankScore)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("profile_screen_root")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.get("profile", lang),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar & Name Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(2.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = avatars[selectedAvatarIdx.coerceIn(0, avatars.size - 1)], fontSize = 38.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            userPrefs.username = it
                        },
                        label = { Text("Player Codename") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .width(240.dp)
                            .testTag("profile_name_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("SELECT AVATAR", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(avatars) { idx, em ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == selectedAvatarIdx) NeonCyan else DarkSurfaceElevated)
                                    .clickable {
                                        selectedAvatarIdx = idx
                                        userPrefs.avatarId = idx
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(em, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rank & Daily Rewards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Competitive Rank Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(LanguageManager.get("rank", lang), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tierBadge, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tierName, color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${userPrefs.competitiveRankScore} MMR", color = TextMuted, fontSize = 12.sp)
                    }
                }

                // Daily Reward Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(LanguageManager.get("daily_rewards", lang), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (canClaimDaily) {
                                    userPrefs.claimDailyReward()
                                    userCoins = userPrefs.totalCoins
                                    canClaimDaily = false
                                }
                            },
                            enabled = canClaimDaily,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("claim_daily_button")
                        ) {
                            Text(
                                text = if (canClaimDaily) "+150 🪙 CLAIM" else "CLAIMED",
                                color = DarkBackground,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lifetime Stats
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LIFETIME CAREER STATS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem(label = "Matches", value = "${userPrefs.totalMatches}")
                        StatItem(label = "Victories", value = "${userPrefs.totalWins}")
                        StatItem(label = "Captures", value = "${userPrefs.totalCaptures}")
                        StatItem(label = "Escapes", value = "${userPrefs.totalEscapes}")
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}
