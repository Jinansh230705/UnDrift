package com.undrift.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.undrift.ui.components.SquircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.ui.theme.*
import com.undrift.ui.components.premiumCard

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FocusNudgeScreen(
    packageName: String?,
    reason: String?,
    onBackToFocus: () -> Unit,
    onNeedTime: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    val appName = try {
        packageName?.let { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() } ?: "App"
    } catch (e: Exception) {
        "App"
    }
    
    val appIcon = try {
        packageName?.let { pm.getApplicationIcon(it).toBitmap().asImageBitmap() }
    } catch (e: Exception) {
        null
    }
    
    // Block back button - user must click a button to leave
    BackHandler {
        // Do nothing - prevents back button dismissal
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.96f))
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .premiumCard(padding = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Icon or Agent Icon in premium bubble
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .premiumCard(
                            cornerRadius = 24.dp,
                            backgroundColor = SurfaceVariantColor,
                            padding = 0.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .clip(SquircleShape())
                        )
                    } else {
                        Icon(
                            imageVector = PhosphorIcons.Bold.Lightning,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = BrandPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .premiumCard(
                            cornerRadius = 12.dp,
                            backgroundColor = SurfaceVariantColor,
                            padding = 0.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (reason == "LIMIT_EXCEEDED") Color(0xFFFF453A) else BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "FOCUS AGENT BLOCKING",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (reason == "LIMIT_EXCEEDED") "Time's Up for $appName!" else "Focus Alert!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (reason == "LIMIT_EXCEEDED") 
                        "You've reached your daily limit for $appName. Stay focused on your goals instead."
                        else "This app is currently blocked because you're in Deep Work mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onBackToFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = SquircleShape(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reason == "LIMIT_EXCEEDED") Color(0xFFFF453A) else BrandPrimary,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Back to Focus", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNeedTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = SquircleShape(),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary,
                        containerColor = SurfaceVariantColor
                    )
                ) {
                    Text("Unlock with 50 Points", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(28.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.Star,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Resisting distractions builds your streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
