package com.undrift.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.undrift.ui.components.SquircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.data.MongoRepository
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.ui.theme.*
import com.undrift.ui.components.premiumCard
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RewardsShopScreen(
    onBack: () -> Unit,
    userProfile: UserProfile,
    userPreferences: UserPreferences,
    mongoRepository: MongoRepository,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val canPurchaseExtraTime = remember(userProfile.lastExtraTimePurchaseDate) {
        val threeDaysInMillis = TimeUnit.DAYS.toMillis(3)
        System.currentTimeMillis() - userProfile.lastExtraTimePurchaseDate >= threeDaysInMillis
    }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .premiumCard(cornerRadius = 16.dp, padding = 0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text("Focus Shop", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(48.dp)
                        .premiumCard(cornerRadius = 16.dp, padding = 0.dp)
                ) {
                    Icon(PhosphorIcons.Bold.ClockCounterClockwise, contentDescription = "History", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Balance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(PhosphorIcons.Bold.Coins, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AVAILABLE BALANCE", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${userProfile.points}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FOCUS COINS",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Special Power-ups
                Column {
                    Text("Special Power-ups", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Extra Time Pass Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumCard()
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(
                                    text = "LIMITED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandSecondary,
                                    modifier = Modifier
                                        .background(SurfaceVariantColor, SquircleShape())
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(PhosphorIcons.Bold.HourglassMedium, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextSecondary)
                            }
                            Text("Extra Time Pass", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unlock a blocked app for 15 minutes. Use wisely to maintain your productivity flow.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(PhosphorIcons.Bold.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (canPurchaseExtraTime) TextSecondary else Color(0xFFFF453A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (canPurchaseExtraTime) "Available once every 3 days" else "Available again in ${getRemainingTime(userProfile.lastExtraTimePurchaseDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canPurchaseExtraTime) TextSecondary else Color(0xFFFF453A)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(PhosphorIcons.Bold.Coins, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("600", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                }
                                Button(
                                    onClick = {
                                        if (userProfile.points >= 600 && canPurchaseExtraTime) {
                                            scope.launch {
                                                userPreferences.recordExtraTimePurchase()
                                                mongoRepository.saveUserToMongo(userProfile.copy(
                                                    points = userProfile.points - 600,
                                                    lastExtraTimePurchaseDate = System.currentTimeMillis()
                                                ))
                                                Toast.makeText(context, "Purchase successful!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else if (userProfile.points < 600) {
                                            Toast.makeText(context, "Insufficient points!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Wait 3 days between purchases!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = canPurchaseExtraTime && userProfile.points >= 600,
                                    shape = SquircleShape(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandPrimary,
                                        contentColor = DarkBackground,
                                        disabledContainerColor = SurfaceVariantColor,
                                        disabledContentColor = TextSecondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Text("PURCHASE", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Agent Skins
                Column {
                    Text("Agent Skins", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SkinItem(modifier = Modifier.weight(1f), name = "Neon Guardian", price = "750", isLocked = false)
                        SkinItem(modifier = Modifier.weight(1f), name = "Chrome Zen", price = "1,200", isLocked = true)
                    }
                }

                // Badges
                Column {
                    Text("Badges", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BadgeItem("Focus Master", "200", PhosphorIcons.Bold.Trophy)
                        BadgeItem("Fast Starter", "150", PhosphorIcons.Bold.Lightning)
                        BadgeItem("Deep Work", "300", PhosphorIcons.Bold.Sparkle)
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

fun getRemainingTime(lastPurchaseDate: Long): String {
    val threeDaysInMillis = TimeUnit.DAYS.toMillis(3)
    val remainingMillis = threeDaysInMillis - (System.currentTimeMillis() - lastPurchaseDate)
    if (remainingMillis <= 0) return "now"
    
    val days = TimeUnit.MILLISECONDS.toDays(remainingMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis) % 24
    return "${days}d ${hours}h"
}

@Composable
fun SkinItem(modifier: Modifier, name: String, price: String, isLocked: Boolean) {
    Box(
        modifier = modifier.premiumCard(padding = 16.dp, cornerRadius = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(SquircleShape())
                    .background(SurfaceVariantColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) PhosphorIcons.Bold.Lock else PhosphorIcons.Bold.Robot,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isLocked) TextSecondary else BrandPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            if (isLocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PhosphorIcons.Bold.Coins, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(price, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOCKED", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(PhosphorIcons.Bold.Coins, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(price, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = SquircleShape(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = DarkBackground)
                    ) {
                        Text("BUY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeItem(name: String, price: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(SquircleShape())
                .background(SurfaceVariantColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(price, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(PhosphorIcons.Bold.Coins, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(12.dp))
        }
    }
}
