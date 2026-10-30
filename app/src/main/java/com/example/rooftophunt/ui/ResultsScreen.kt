package com.example.rooftophunt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rooftophunt.gameplay.MatchSummary
import com.example.rooftophunt.gameplay.RoundResult
import com.example.ui.theme.*

@Composable
fun ResultsScreen(
    summary: MatchSummary,
    lang: String,
    onReturnHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("results_screen_root")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Victory",
                    tint = NeonAmber,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.get("match_results", lang),
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Winner Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, if (summary.isTie) NeonAmber else NeonCyan, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (summary.isTie) "TIED MATCH!" else "${LanguageManager.get("winner", lang)}: ${summary.winnerName}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (summary.isTie) NeonAmber else NeonCyan
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(summary.playerAName, color = TextSecondary, fontSize = 14.sp)
                            Text(
                                "${summary.totalScoreA} pts",
                                color = NeonCyan,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${summary.totalCapturesA} Captures", color = TextMuted, fontSize = 12.sp)
                        }

                        Text("VS", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(summary.playerBName, color = TextSecondary, fontSize = 14.sp)
                            Text(
                                "${summary.totalScoreB} pts",
                                color = NeonPurple,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${summary.totalCapturesB} Captures", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Rounds Breakdown
            Text(
                text = "ROUND-BY-ROUND BREAKDOWN",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(summary.roundResults) { round ->
                    RoundResultCard(round = round, lang = lang)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Return to Main Menu Button
            Button(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("results_return_home_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = DarkBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.get("return_menu", lang),
                    color = DarkBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun RoundResultCard(round: RoundResult, lang: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${LanguageManager.get("round", lang)} ${round.roundNumber}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (round.hunterCaptured) "Captured with ${round.captureTimeRemaining.toInt()}s left" else "Runner Survived 60s!",
                    color = if (round.hunterCaptured) HunterOrange else RunnerBlue,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${round.hunterScore} / +${round.runnerScore} pts",
                    color = NeonAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "+${round.coinsEarnedPlayerA} coins",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
