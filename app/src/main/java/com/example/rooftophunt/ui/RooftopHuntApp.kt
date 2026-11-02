package com.example.rooftophunt.ui

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rooftophunt.audio.GameAudioEngine
import com.example.rooftophunt.data.LeaderboardRepository
import com.example.rooftophunt.data.UserPreferences
import com.example.rooftophunt.engine3d.CharacterOutfit
import com.example.rooftophunt.engine3d.RooftopRenderer
import com.example.rooftophunt.gameplay.*
import com.example.rooftophunt.multiplayer.NetworkManager
import com.example.rooftophunt.multiplayer.PlayerSyncPacket
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class AppScreen {
    MAIN_MENU,
    LOBBY,
    GAME_PLAY,
    RESULTS,
    CUSTOMIZATION,
    MAPS,
    LEADERBOARDS,
    PROFILE,
    SETTINGS
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RooftopHuntApp() {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val audioEngine = remember { GameAudioEngine() }
    val networkManager = remember { NetworkManager() }

    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }
    var language by remember { mutableStateOf(userPrefs.language) }

    // Game state references
    var activeEngine by remember { mutableStateOf<GameEngine?>(null) }
    val activeRenderer = remember { RooftopRenderer(null) }

    // Touch controls state
    var inputDx by remember { mutableFloatStateOf(0f) }
    var inputDz by remember { mutableFloatStateOf(0f) }
    var isJumpRequested by remember { mutableStateOf(false) }
    var isSprintActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        audioEngine.soundEnabled = userPrefs.soundEnabled
        audioEngine.musicEnabled = userPrefs.musicEnabled
        activeRenderer.cameraSensitivity = userPrefs.cameraSensitivity
        if (userPrefs.musicEnabled) {
            audioEngine.startMusic()
        }
    }

    // Handle Android system back button
    BackHandler(enabled = currentScreen != AppScreen.MAIN_MENU) {
        if (currentScreen == AppScreen.GAME_PLAY) {
            activeEngine = null
            networkManager.disconnect()
            currentScreen = AppScreen.MAIN_MENU
        } else {
            currentScreen = AppScreen.MAIN_MENU
        }
    }

    // Game Loop updater when on GAME_PLAY screen
    LaunchedEffect(currentScreen, activeEngine) {
        if (currentScreen == AppScreen.GAME_PLAY) {
            var lastTime = System.nanoTime()

            // Setup network callbacks
            networkManager.onOpponentState = { packet ->
                activeEngine?.playerB?.let { opp ->
                    opp.x = packet.x
                    opp.y = packet.y
                    opp.z = packet.z
                    opp.vx = packet.vx
                    opp.vy = packet.vy
                    opp.vz = packet.vz
                    opp.yaw = packet.yaw
                    opp.health = packet.health
                }
            }

            networkManager.onCaptureEvent = { hunterId, runnerId ->
                activeEngine?.onNotification?.invoke("Capture confirmed by server!")
            }
            networkManager.onRoundSync = { round, time ->
                activeEngine?.let { engine ->
                    if (engine.currentRound == round) {
                        engine.roundTimeRemaining = time
                    }
                }
            }
            networkManager.onMatchFinished = {
                activeEngine?.onNotification?.invoke("Match finished on server")
            }
            networkManager.onOpponentLeft = {
                activeEngine?.onNotification?.invoke("Opponent disconnected")
            }

            var netSyncTimer = 0f

            while (isActive && currentScreen == AppScreen.GAME_PLAY) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                activeEngine?.let { engine ->
                    engine.update(
                        delta = dt,
                        inputDx = inputDx,
                        inputDz = inputDz,
                        isJumpPressed = isJumpRequested,
                        isSprintPressed = isSprintActive
                    )
                    isJumpRequested = false

                    // Sync state over network
                    netSyncTimer += dt
                    if (netSyncTimer >= 0.05f) { // 20 Hz sync
                        netSyncTimer = 0f
                        val sync = PlayerSyncPacket(
                            playerId = networkManager.localPlayerId,
                            x = engine.playerA.x,
                            y = engine.playerA.y,
                            z = engine.playerA.z,
                            vx = engine.playerA.vx,
                            vy = engine.playerA.vy,
                            vz = engine.playerA.vz,
                            yaw = engine.playerA.yaw,
                            animState = engine.playerA.animState.name,
                            health = engine.playerA.health,
                            isShieldActive = engine.playerA.isShieldActive,
                            isSpeedActive = engine.playerA.isSpeedBoostActive,
                            coins = engine.playerA.coinsCollected
                        )
                        networkManager.sendPlayerState(sync)
                    }

                    // Check for match completion
                    if (engine.matchPhase == MatchPhase.MATCH_FINISHED) {
                        // Persist coins & stats
                        userPrefs.totalCoins += engine.playerA.coinsCollected
                        userPrefs.totalMatches++
                        val won = engine.matchSummary?.winnerName == engine.playerAName
                        if (won) userPrefs.totalWins++
                        userPrefs.competitiveRankScore += if (won) 35 else -15

                        currentScreen = AppScreen.RESULTS
                    } else if (engine.matchPhase == MatchPhase.ROUND_ENDED) {
                        // Delay 3s then start next round
                        delay(2500)
                        engine.proceedToNextRound()
                    }
                }
                delay(16) // ~60 FPS
            }
        }
    }

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
                if (language == "fa") androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            when (currentScreen) {
                AppScreen.MAIN_MENU -> {
                    MainMenuScreen(
                        userPrefs = userPrefs,
                        lang = language,
                        onNavigate = { screen -> currentScreen = screen }
                    )
                }

                AppScreen.LOBBY -> {
                    LobbyScreen(
                        lang = language,
                        networkManager = networkManager,
                        onStartPracticeGame = { map ->
                            val engine = GameEngine(
                                currentMap = map,
                                playerAName = userPrefs.username,
                                playerBName = "CyberBot",
                                playerAOutfit = userPrefs.equippedOutfit,
                                playerBOutfit = CharacterOutfit.DEFAULT_RUNNER,
                                isMultiplayerHost = true,
                                isPracticeMode = true
                            )
                            engine.onSoundTrigger = { sfx -> audioEngine.playSound(sfx) }
                            activeRenderer.gameEngine = engine
                            activeEngine = engine
                            currentScreen = AppScreen.GAME_PLAY
                        },
                        onStartOnlineGame = { map, isHost ->
                            val engine = GameEngine(
                                currentMap = map,
                                playerAName = userPrefs.username,
                                playerBName = networkManager.opponentPlayerName,
                                playerAOutfit = userPrefs.equippedOutfit,
                                playerBOutfit = CharacterOutfit.DEFAULT_RUNNER,
                                isMultiplayerHost = isHost,
                                isPracticeMode = false
                            )
                            engine.onSoundTrigger = { sfx -> audioEngine.playSound(sfx) }
                            activeRenderer.gameEngine = engine
                            activeEngine = engine
                            currentScreen = AppScreen.GAME_PLAY
                        },
                        onBack = { currentScreen = AppScreen.MAIN_MENU }
                    )
                }

                AppScreen.GAME_PLAY -> {
                    activeEngine?.let { engine ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 3D OpenGL View
                            AndroidView(
                                factory = { ctx ->
                                    GLSurfaceView(ctx).apply {
                                        setEGLContextClientVersion(3)
                                        setRenderer(activeRenderer)
                                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Interactive Mobile Game HUD overlay
                            GameHudView(
                                gameEngine = engine,
                                renderer = activeRenderer,
                                lang = language,
                                onJoystickMove = { dx, dz ->
                                    inputDx = dx
                                    inputDz = dz
                                },
                                onJumpPressed = { isJumpRequested = true },
                                onSprintToggle = { isSprintActive = it },
                                onAbilityTapped = { slotIdx -> engine.activateAbility(slotIdx) },
                                onExitGame = {
                                    activeEngine = null
                                    networkManager.disconnect()
                                    currentScreen = AppScreen.MAIN_MENU
                                }
                            )
                        }
                    }
                }

                AppScreen.RESULTS -> {
                    activeEngine?.matchSummary?.let { summary ->
                        ResultsScreen(
                            summary = summary,
                            lang = language,
                            onReturnHome = {
                                activeEngine = null
                                currentScreen = AppScreen.MAIN_MENU
                            }
                        )
                    } ?: run {
                        currentScreen = AppScreen.MAIN_MENU
                    }
                }

                AppScreen.CUSTOMIZATION -> {
                    CustomizationScreen(
                        userPrefs = userPrefs,
                        lang = language,
                        onBack = { currentScreen = AppScreen.MAIN_MENU }
                    )
                }

                AppScreen.MAPS -> {
                    MapsScreen(
                        lang = language,
                        onBack = { currentScreen = AppScreen.MAIN_MENU }
                    )
                }

                AppScreen.LEADERBOARDS -> {
                    LeaderboardScreen(
                        userPrefs = userPrefs,
                        lang = language,
                        onBack = { currentScreen = AppScreen.MAIN_MENU }
                    )
                }

                AppScreen.PROFILE -> {
                    ProfileScreen(
                        userPrefs = userPrefs,
                        lang = language,
                        onBack = { currentScreen = AppScreen.MAIN_MENU }
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        userPrefs = userPrefs,
                        audioEngine = audioEngine,
                        lang = language,
                        onLanguageChanged = { newLang -> language = newLang },
                        onBack = { currentScreen = AppScreen.MAIN_MENU }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    userPrefs: UserPreferences,
    lang: String,
    onNavigate: (AppScreen) -> Unit
) {
    val (tierName, tierBadge) = LeaderboardRepository.getRankTier(userPrefs.competitiveRankScore)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050711), Color(0xFF10142A), Color(0xFF090B17))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .testTag("main_menu_root")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Player profile preview + Coins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated)
                        .clickable { onNavigate(AppScreen.PROFILE) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tierBadge, fontSize = 16.sp)
                    Column {
                        Text(userPrefs.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(tierName, color = NeonCyan, fontSize = 10.sp)
                    }
                }

                // Coins pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated)
                        .clickable { onNavigate(AppScreen.CUSTOMIZATION) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                    Text("${userPrefs.totalCoins}", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            CinematicHeroBanner(lang = lang)

            Spacer(modifier = Modifier.height(22.dp))

            // Primary Play Action Button
            Button(
                onClick = { onNavigate(AppScreen.LOBBY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("main_play_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = LanguageManager.get("play_online", lang),
                    color = DarkBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Menu Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuGridItem(
                    title = LanguageManager.get("customization", lang),
                    icon = Icons.Default.Checkroom,
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.CUSTOMIZATION) }
                )
                MenuGridItem(
                    title = LanguageManager.get("map_info", lang),
                    icon = Icons.Default.Apartment,
                    accentColor = NeonPink,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.MAPS) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuGridItem(
                    title = LanguageManager.get("leaderboards", lang),
                    icon = Icons.Default.Leaderboard,
                    accentColor = NeonAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.LEADERBOARDS) }
                )
                MenuGridItem(
                    title = LanguageManager.get("settings", lang),
                    icon = Icons.Default.Settings,
                    accentColor = TextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.SETTINGS) }
                )
            }
        }
    }
}


@Composable
private fun CinematicHeroBanner(lang: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF111C38), Color(0xFF21113F), Color(0xFF080B18))))
            .border(1.dp, NeonCyan.copy(alpha = 0.42f), RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Column(Modifier.align(Alignment.TopStart)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(NeonCyan))
                Spacer(Modifier.width(7.dp))
                Text("ONLINE ROOFTOP SURVIVAL", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = LanguageManager.get("app_title", lang).uppercase(),
                color = TextPrimary,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                maxLines = 1
            )
            Text(
                text = LanguageManager.get("app_tagline", lang),
                color = Color(0xFFB7C3E4),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .8.sp,
                maxLines = 2
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomEnd).height(66.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(30, 47, 38, 62, 44, 55, 34).forEachIndexed { index, height ->
                Box(
                    Modifier.width(if (index == 3) 24.dp else 17.dp)
                        .height(height.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (index % 2 == 0) NeonCyan.copy(alpha = .55f) else NeonPurple.copy(alpha = .72f),
                                    Color(0xFF10162B)
                                )
                            )
                        )
                        .border(.5.dp, NeonCyan.copy(alpha = .18f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
            }
        }
        Icon(
            imageVector = Icons.Default.FlashOn,
            contentDescription = null,
            tint = NeonCyan.copy(alpha = .18f),
            modifier = Modifier.align(Alignment.BottomStart).size(54.dp)
        )
        Surface(
            color = Color(0xFF0A1022).copy(alpha = .88f),
            shape = RoundedCornerShape(30.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = .55f)),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                "HUNTER  VS  RUNNER",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = Color(0xFFD8CBFF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .8.sp
            )
        }
    }
}

@Composable
fun MenuGridItem(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        modifier = modifier
            .height(78.dp)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}
