package com.example.rooftophunt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rooftophunt.data.UserPreferences
import com.example.rooftophunt.engine3d.CharacterOutfit
import com.example.ui.theme.*

@Composable
fun CustomizationScreen(
    userPrefs: UserPreferences,
    lang: String,
    onBack: () -> Unit
) {
    var equippedOutfit by remember { mutableStateOf(userPrefs.equippedOutfit) }
    var userCoins by remember { mutableStateOf(userPrefs.totalCoins) }
    var unlockedSet by remember { mutableStateOf(userPrefs.unlockedOutfits) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("customization_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("customization_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = LanguageManager.get("customization", lang),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Coin Counter
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$userCoins", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Currently Equipped Hero Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Color(
                                    equippedOutfit.primaryColor[0],
                                    equippedOutfit.primaryColor[1],
                                    equippedOutfit.primaryColor[2]
                                )
                            )
                            .border(2.dp, TextPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏃", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "EQUIPPED OUTFIT",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = equippedOutfit.displayName,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Ready for the 4-round chase",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "AVAILABLE OUTFITS & SKINS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of Outfits
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(CharacterOutfit.values()) { outfit ->
                    val isUnlocked = unlockedSet.contains(outfit.name)
                    val isEquipped = equippedOutfit == outfit

                    OutfitCard(
                        outfit = outfit,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        canAfford = userCoins >= outfit.coinCost,
                        lang = lang,
                        onAction = {
                            if (isUnlocked) {
                                userPrefs.equippedOutfit = outfit
                                equippedOutfit = outfit
                            } else {
                                if (userPrefs.unlockOutfit(outfit)) {
                                    userCoins = userPrefs.totalCoins
                                    unlockedSet = userPrefs.unlockedOutfits
                                    userPrefs.equippedOutfit = outfit
                                    equippedOutfit = outfit
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OutfitCard(
    outfit: CharacterOutfit,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    lang: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isEquipped) DarkSurfaceCard else DarkSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                color = if (isEquipped) NeonCyan else DarkSurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color Swatch preview
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(outfit.primaryColor[0], outfit.primaryColor[1], outfit.primaryColor[2]))
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(outfit.secondaryColor[0], outfit.secondaryColor[1], outfit.secondaryColor[2]))
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(outfit.accentColor[0], outfit.accentColor[1], outfit.accentColor[2]))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = outfit.displayName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isEquipped -> DarkSurfaceBorder
                        isUnlocked -> NeonCyan
                        canAfford -> NeonAmber
                        else -> DarkSurfaceBorder
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                if (isEquipped) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LanguageManager.get("equipped", lang), color = TextPrimary, fontSize = 11.sp)
                } else if (isUnlocked) {
                    Text(LanguageManager.get("equip", lang), color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                } else {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${outfit.coinCost}", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
