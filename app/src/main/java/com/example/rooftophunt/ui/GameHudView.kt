package com.example.rooftophunt.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rooftophunt.engine3d.RooftopRenderer
import com.example.rooftophunt.gameplay.AbilitySlotState
import com.example.rooftophunt.gameplay.GameEngine
import com.example.rooftophunt.gameplay.MatchPhase
import com.example.rooftophunt.gameplay.PlayerRole
import com.example.rooftophunt.gameplay.SpecialAbility
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GameHudView(
    gameEngine: GameEngine,
    renderer: RooftopRenderer,
    lang: String,
    onJoystickMove: (dx: Float, dz: Float) -> Unit,
    onJumpPressed: (Boolean) -> Unit,
    onSprintToggle: (Boolean) -> Unit,
    onAbilityTapped: (slotIndex: Int) -> Unit,
    onExitGame: () -> Unit
) {
    var joystickKnobOffset by remember { mutableStateOf(Offset.Zero) }
    var isSprinting by remember { mutableStateOf(false) }

    // Notification toast
    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(gameEngine) {
        gameEngine.onNotification = { msg ->
            toastMessage = msg
            toastVisible = true
        }
    }

    LaunchedEffect(toastVisible) {
        if (toastVisible) {
            kotlinx.coroutines.delay(2200)
            toastVisible = false
        }
    }

    val playerA = gameEngine.playerA
    val currentRole = playerA.role
    val isHunter = currentRole == PlayerRole.HUNTER
    val roundTime = gameEngine.roundTimeRemaining.toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_hud_root")
    ) {
        // Transparent touch surface for camera drag (covers entire screen, buttons sit on top)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        renderer.handleDrag(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // ================= TOP HUD: Round, Role, Timer, Health, Telemetry =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit / Pause Button
                IconButton(
                    onClick = onExitGame,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                        .testTag("exit_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Match",
                        tint = TextPrimary
                    )
                }

                // Center Match Banner: Round & 60s Timer
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f)),
                    modifier = Modifier.border(1.dp, if (roundTime <= 10) NeonRed else DarkSurfaceBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${LanguageManager.get("round", lang)} ${gameEngine.currentRound}/${gameEngine.totalRounds}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(DarkSurfaceBorder)
                        )
                        Text(
                            text = "${roundTime}s",
                            color = if (roundTime <= 10) NeonRed else NeonCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Coin Counter
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = NeonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${playerA.coinsCollected}",
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-bar: Role Badge + Health Bar + Opponent Distance Radar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isHunter) HunterOrange.copy(alpha = 0.25f) else RunnerBlue.copy(alpha = 0.25f))
                        .border(1.dp, if (isHunter) HunterOrange else RunnerBlue, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isHunter) Icons.Default.GpsFixed else Icons.Default.DirectionsRun,
                        contentDescription = "Role",
                        tint = if (isHunter) HunterOrange else RunnerBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isHunter) LanguageManager.get("hunter", lang) else LanguageManager.get("runner", lang),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Health Bar (100 HP)
                Column(
                    modifier = Modifier.width(140.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = LanguageManager.get("health", lang),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${playerA.health}/100",
                            color = if (playerA.health > 40) NeonGreen else NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { (playerA.health / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (playerA.health > 40) NeonGreen else NeonRed,
                        trackColor = DarkSurfaceElevated
                    )
                }

                // Opponent Distance & 3D Radar Arrow
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Radar Bearing",
                        tint = if (isHunter) HunterOrange else RunnerBlue,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(renderer.opponentBearingDegrees)
                    )
                    Text(
                        text = "${renderer.opponentDistance.toInt()}${LanguageManager.get("meters", lang)}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ================= NOTIFICATION TOAST =================
        AnimatedVisibility(
            visible = toastVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeonPurple.copy(alpha = 0.95f)),
                modifier = Modifier.border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = toastMessage,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // ================= BOTTOM CONTROLS: Joystick, 3 Ability Slots, Jump/Sprint =================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Left: Virtual Joystick
            val joystickMaxRadius = 65f
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.BottomStart)
                    .clip(CircleShape)
                    .background(DarkSurfaceCard.copy(alpha = 0.65f))
                    .border(2.dp, DarkSurfaceBorder, CircleShape)
                    .pointerInput(renderer.cameraYaw) {
                        detectDragGestures(
                            onDragEnd = {
                                joystickKnobOffset = Offset.Zero
                                onJoystickMove(0f, 0f)
                            },
                            onDragCancel = {
                                joystickKnobOffset = Offset.Zero
                                onJoystickMove(0f, 0f)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = joystickKnobOffset + dragAmount
                                val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                                val clamped = if (dist > joystickMaxRadius) {
                                    Offset(
                                        (newOffset.x / dist) * joystickMaxRadius,
                                        (newOffset.y / dist) * joystickMaxRadius
                                    )
                                } else {
                                    newOffset
                                }
                                joystickKnobOffset = clamped

                                // Convert joystick input to world direction based on camera yaw
                                val normX = clamped.x / joystickMaxRadius
                                val normZ = -clamped.y / joystickMaxRadius // Up is forward (-y)

                                val radYaw = Math.toRadians(renderer.cameraYaw.toDouble()).toFloat()
                                val worldDx = normX * cos(radYaw) + normZ * sin(radYaw)
                                val worldDz = -normX * sin(radYaw) + normZ * cos(radYaw)

                                onJoystickMove(worldDx, worldDz)
                            }
                        )
                    }
                    .testTag("virtual_joystick_base"),
                contentAlignment = Alignment.Center
            ) {
                // Knob
                Box(
                    modifier = Modifier
                        .offset { IntOffset(joystickKnobOffset.x.toInt(), joystickKnobOffset.y.toInt()) }
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(NeonCyan, DarkSurfaceElevated)))
                        .border(1.5.dp, TextPrimary, CircleShape)
                )
            }

            // Center Bottom: 3 Special Ability Slots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                gameEngine.abilitySlots.forEachIndexed { idx, slot ->
                    AbilitySlotButton(
                        slotIndex = idx,
                        slot = slot,
                        onTap = { onAbilityTapped(idx) }
                    )
                }
            }

            // Right: Jump & Sprint Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sprint Button (Toggle or Tap)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSprinting) NeonAmber else DarkSurfaceElevated.copy(alpha = 0.85f))
                        .border(2.dp, if (isSprinting) TextPrimary else DarkSurfaceBorder, CircleShape)
                        .clickable {
                            isSprinting = !isSprinting
                            onSprintToggle(isSprinting)
                        }
                        .testTag("sprint_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Sprint",
                        tint = if (isSprinting) DarkBackground else TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Jump Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(NeonCyan, NeonPurple)))
                        .border(2.dp, TextPrimary, CircleShape)
                        .clickable {
                            onJumpPressed(true)
                        }
                        .testTag("jump_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Jump",
                            tint = DarkBackground,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = LanguageManager.get("jump", lang),
                            color = DarkBackground,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AbilitySlotButton(
    slotIndex: Int,
    slot: AbilitySlotState,
    onTap: () -> Unit
) {
    val isCollected = slot.isCollected
    val isActive = slot.isActive

    val icon = when (slot.ability) {
        SpecialAbility.SPEED_BOOST -> Icons.Default.Bolt
        SpecialAbility.SHIELD -> Icons.Default.Shield
        SpecialAbility.HEAL -> Icons.Default.Favorite
    }

    val activeColor = when (slot.ability) {
        SpecialAbility.SPEED_BOOST -> NeonCyan
        SpecialAbility.SHIELD -> NeonPurple
        SpecialAbility.HEAL -> NeonGreen
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = isCollected && !isActive) { onTap() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        isActive -> activeColor
                        isCollected -> activeColor.copy(alpha = 0.35f)
                        else -> DarkSurface.copy(alpha = 0.6f)
                    }
                )
                .border(
                    width = if (isCollected || isActive) 2.dp else 1.dp,
                    color = when {
                        isActive -> TextPrimary
                        isCollected -> activeColor
                        else -> DarkSurfaceBorder
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .testTag("ability_slot_$slotIndex"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = slot.ability.title,
                tint = when {
                    isActive -> DarkBackground
                    isCollected -> activeColor
                    else -> TextMuted
                },
                modifier = Modifier.size(26.dp)
            )

            // Timer badge if active
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${slot.timerRemaining.toInt()}s",
                        color = activeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = when (slot.ability) {
                SpecialAbility.SPEED_BOOST -> "SPEED"
                SpecialAbility.SHIELD -> "SHIELD"
                SpecialAbility.HEAL -> "HEAL"
            },
            color = if (isCollected || isActive) TextPrimary else TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
