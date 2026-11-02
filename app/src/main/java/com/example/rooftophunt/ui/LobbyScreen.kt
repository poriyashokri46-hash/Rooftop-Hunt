package com.example.rooftophunt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rooftophunt.gameplay.MapCatalog
import com.example.rooftophunt.gameplay.RooftopMap
import com.example.rooftophunt.multiplayer.NetworkConnectionState
import com.example.rooftophunt.multiplayer.NetworkManager
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    lang: String,
    networkManager: NetworkManager,
    onStartPracticeGame: (map: RooftopMap) -> Unit,
    onStartOnlineGame: (map: RooftopMap, isHost: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Online / Room, 1: Practice AI
    var inputRoomCode by remember { mutableStateOf("") }
    var generatedRoomCode by remember { mutableStateOf("ROOF") }
    var selectedMap by remember { mutableStateOf(MapCatalog.maps[0]) }

    var netState by remember { mutableStateOf(networkManager.connectionState) }

    LaunchedEffect(networkManager) {
        networkManager.onConnectionStateChanged = { state ->
            netState = state
            if (state == NetworkConnectionState.CONNECTED) {
                onStartOnlineGame(selectedMap, networkManager.isHost)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("lobby_screen_root")
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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("lobby_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedTab == 0) LanguageManager.get("play_online", lang) else LanguageManager.get("practice_mode", lang),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceElevated)
                    .padding(4.dp)
            ) {
                TabButton(
                    title = "ONLINE MULTIPLAYER",
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    title = "SOLO PRACTICE AI",
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 0) {
                // ================= ONLINE ROOM / QUICK MATCH =================
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CREATE OR JOIN A PRIVATE ROOM",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Play remotely on two different phones using a room code",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Room code input
                        OutlinedTextField(
                            value = inputRoomCode,
                            onValueChange = { if (it.length <= 6) inputRoomCode = it.uppercase() },
                            label = { Text(LanguageManager.get("room_code", lang)) },
                            placeholder = { Text("e.g. ROOF, HUNT") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val code = if (inputRoomCode.isNotBlank()) inputRoomCode else generatedRoomCode
                                    networkManager.createOrJoinRoom(code, "Player 1", "DEFAULT_HUNTER", asHost = true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("create_room_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = LanguageManager.get("create_room", lang),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (inputRoomCode.isNotBlank()) {
                                        networkManager.createOrJoinRoom(inputRoomCode, "Player 2", "DEFAULT_RUNNER", asHost = false)
                                    }
                                },
                                enabled = inputRoomCode.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("join_room_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = LanguageManager.get("join_room", lang),
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Connection status
                        if (netState != NetworkConnectionState.DISCONNECTED) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (netState) {
                                        NetworkConnectionState.CONNECTING -> "Connecting to server..."
                                        NetworkConnectionState.WAITING_FOR_PLAYER -> "Room Code [${networkManager.currentRoomCode}] Waiting for Player 2..."
                                        NetworkConnectionState.CONNECTED -> "Player connected! Starting match..."
                                        NetworkConnectionState.ERROR -> "Connection failed. Check server and try again."
                                        else -> ""
                                    },
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Match Option
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            networkManager.createOrJoinRoom("MATCH", "Runner", "DEFAULT_HUNTER", asHost = false)
                        }
                        .testTag("quick_match_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("QUICK MATCHMAKING", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Find any available online opponent instantly", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                }
            } else {
                // ================= PRACTICE SOLO AI =================
                Text(
                    text = "SELECT MAP FOR PRACTICE (ALL 10 AVAILABLE)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MapCatalog.maps) { map ->
                        val isSelected = map.id == selectedMap.id
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) DarkSurfaceCard else DarkSurfaceElevated),
                            modifier = Modifier
                                .width(150.dp)
                                .border(1.5.dp, if (isSelected) NeonCyan else DarkSurfaceBorder, RoundedCornerShape(14.dp))
                                .clickable { selectedMap = map }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Map ${map.id}",
                                    color = NeonAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = map.name,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = map.weatherType,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = selectedMap.name,
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedMap.description,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Obstacles: ${selectedMap.obstacles.size}", color = TextMuted, fontSize = 12.sp)
                            Text("Coins: ${selectedMap.coins.size}", color = TextMuted, fontSize = 12.sp)
                            Text("Weather: ${selectedMap.weatherType}", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onStartPracticeGame(selectedMap) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_practice_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DarkBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAUNCH 4-ROUND MATCH VS AI",
                        color = DarkBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) DarkBackground else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
