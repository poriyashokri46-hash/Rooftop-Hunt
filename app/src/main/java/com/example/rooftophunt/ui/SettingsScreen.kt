package com.example.rooftophunt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rooftophunt.audio.GameAudioEngine
import com.example.rooftophunt.data.UserPreferences
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    userPrefs: UserPreferences,
    audioEngine: GameAudioEngine,
    lang: String,
    onLanguageChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(userPrefs.soundEnabled) }
    var musicEnabled by remember { mutableStateOf(userPrefs.musicEnabled) }
    var cameraSens by remember { mutableStateOf(userPrefs.cameraSensitivity) }
    var currentLang by remember { mutableStateOf(userPrefs.language) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("settings_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.get("settings", lang),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Sound FX Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(LanguageManager.get("sound_fx", lang), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                userPrefs.soundEnabled = it
                                audioEngine.soundEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Music Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(LanguageManager.get("music", lang), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = musicEnabled,
                            onCheckedChange = {
                                musicEnabled = it
                                userPrefs.musicEnabled = it
                                audioEngine.musicEnabled = it
                                if (it) audioEngine.startMusic() else audioEngine.stopMusic()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Camera Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(LanguageManager.get("camera_sensitivity", lang), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${(cameraSens * 100).toInt()}%", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = cameraSens,
                            onValueChange = {
                                cameraSens = it
                                userPrefs.cameraSensitivity = it
                            },
                            valueRange = 0.4f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Language Selection (English / Persian Farsi)
                    Text(LanguageManager.get("language", lang), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                currentLang = "en"
                                userPrefs.language = "en"
                                onLanguageChanged("en")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLang == "en") NeonCyan else DarkSurfaceElevated
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("English", color = if (currentLang == "en") DarkBackground else TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                currentLang = "fa"
                                userPrefs.language = "fa"
                                onLanguageChanged("fa")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLang == "fa") NeonCyan else DarkSurfaceElevated
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فارسی (Persian)", color = if (currentLang == "fa") DarkBackground else TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tech specs / credits card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GAME ARCHITECTURE & SPECS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Native OpenGL ES 3.0 Real-Time 3D Engine", color = TextMuted, fontSize = 12.sp)
                    Text("• 10 Unique Skyscraper Maps with Dynamic Hazards", color = TextMuted, fontSize = 12.sp)
                    Text("• Dual Hunter vs Runner 4-Round Competitive Engine", color = TextMuted, fontSize = 12.sp)
                    Text("• Real-Time Online WebSocket Room Protocol", color = TextMuted, fontSize = 12.sp)
                    Text("• Procedural AudioTrack Synthesizer", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
