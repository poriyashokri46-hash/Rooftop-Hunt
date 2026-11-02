package com.example.rooftophunt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    var selectedTab by remember { mutableStateOf(0) }
    var inputRoomCode by remember { mutableStateOf("") }
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
            .background(Brush.verticalGradient(listOf(Color(0xFF070914), Color(0xFF10112A), DarkBackground)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("lobby_screen_root"),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("lobby_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("MATCH SETUP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(
                        if (selectedTab == 0) LanguageManager.get("play_online", lang) else LanguageManager.get("practice_mode", lang),
                        color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(color = Color(0xFF10283A), shape = RoundedCornerShape(30.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = .35f))) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(if (netState == NetworkConnectionState.CONNECTED) NeonGreen else NeonCyan))
                        Spacer(Modifier.width(6.dp))
                        Text(if (netState == NetworkConnectionState.CONNECTED) "CONNECTED" else "ONLINE", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Cinematic rooftop scene / match identity panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF101B36), Color(0xFF241044), Color(0xFF080B18))))
                    .border(1.dp, NeonCyan.copy(alpha = .32f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text("ROOFTOP HUNT", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                    Text("CHASE THE EDGE. OWN THE NIGHT.", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        MatchPill("4 ROUNDS", NeonPurple)
                        MatchPill("60 SEC", NeonCyan)
                        MatchPill("2 PLAYERS", Color(0xFFB18CFF))
                    }
                }
                Icon(
                    Icons.Default.Apartment,
                    contentDescription = null,
                    tint = NeonPurple.copy(alpha = .55f),
                    modifier = Modifier.align(Alignment.BottomEnd).size(104.dp)
                )
                Text("01 / 04", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd))
            }

            Spacer(Modifier.height(18.dp))

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF15182B)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton("ONLINE MULTIPLAYER", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                TabButton("SOLO PRACTICE", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
            }

            Spacer(Modifier.height(20.dp))
            Text("SELECT BATTLE MAP", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("Choose the rooftop arena for your next run", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
                items(MapCatalog.maps) { map ->
                    val chosen = map.id == selectedMap.id
                    Column(
                        Modifier.width(142.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (chosen) Color(0xFF1A1D3A) else Color(0xFF111525))
                            .border(1.dp, if (chosen) NeonCyan else Color(0xFF292D48), RoundedCornerShape(16.dp))
                            .clickable { selectedMap = map }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ARENA 0${map.id}", color = if (chosen) NeonCyan else NeonPurple, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            if (chosen) Icon(Icons.Default.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(map.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Text(map.weatherType, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color(0xFF111525),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF292D48)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = NeonCyan, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(selectedMap.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Text(selectedMap.weatherType.uppercase(), color = NeonPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(selectedMap.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("${selectedMap.obstacles.size} OBSTACLES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${selectedMap.coins.size} COINS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (selectedTab == 0) {
                Text("PLAY WITH A FRIEND", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = inputRoomCode,
                    onValueChange = { if (it.length <= 6) inputRoomCode = it.uppercase() },
                    label = { Text(LanguageManager.get("room_code", lang)) },
                    placeholder = { Text("ENTER ROOM CODE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("room_code_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF343854),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val code = inputRoomCode.ifBlank { "ROOF" }
                            networkManager.createOrJoinRoom(code, "Player 1", "DEFAULT_HUNTER", asHost = true)
                        },
                        modifier = Modifier.weight(1f).height(52.dp).testTag("create_room_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(5.dp))
                        Text(LanguageManager.get("create_room", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { if (inputRoomCode.isNotBlank()) networkManager.createOrJoinRoom(inputRoomCode, "Player 2", "DEFAULT_RUNNER", asHost = false) },
                        enabled = inputRoomCode.isNotBlank(),
                        modifier = Modifier.weight(1f).height(52.dp).testTag("join_room_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                    ) {
                        Icon(Icons.Default.Login, null)
                        Spacer(Modifier.width(5.dp))
                        Text(LanguageManager.get("join_room", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { networkManager.createOrJoinRoom("MATCH", "Runner", "DEFAULT_HUNTER", asHost = false) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("quick_match_card"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = .55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.Public, null, tint = NeonCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("QUICK MATCHMAKING", fontWeight = FontWeight.ExtraBold, letterSpacing = .6.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = NeonCyan)
                }

                if (netState != NetworkConnectionState.DISCONNECTED) {
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Color(0xFF15182B), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (netState == NetworkConnectionState.CONNECTING || netState == NetworkConnectionState.WAITING_FOR_PLAYER) {
                                CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = NeonCyan)
                            } else {
                                Icon(Icons.Default.Info, null, tint = if (netState == NetworkConnectionState.ERROR) NeonPink else NeonGreen)
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(
                                when (netState) {
                                    NetworkConnectionState.CONNECTING -> "Connecting to match server..."
                                    NetworkConnectionState.WAITING_FOR_PLAYER -> "Room ${networkManager.currentRoomCode} · Waiting for player 2"
                                    NetworkConnectionState.CONNECTED -> "Opponent connected · Preparing match"
                                    NetworkConnectionState.ERROR -> "Connection failed · Check the server and retry"
                                    else -> ""
                                },
                                color = TextSecondary, fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                Text("READY FOR A SOLO RUN?", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Train your movement and survive all four rounds", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onStartPracticeGame(selectedMap) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("start_practice_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("START PRACTICE MATCH", fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = .7.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MatchPill(label: String, accent: Color) {
    Surface(color = accent.copy(alpha = .13f), shape = RoundedCornerShape(30.dp), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
    }
}

@Composable
fun TabButton(title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(11.dp))
            .background(if (isSelected) Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF7A5CFF))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
            .clickable { onClick() }.padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = if (isSelected) Color(0xFF060914) else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .2.sp, textAlign = TextAlign.Center)
    }
}
