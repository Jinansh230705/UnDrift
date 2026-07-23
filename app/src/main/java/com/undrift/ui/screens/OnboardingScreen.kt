package com.undrift.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.undrift.ui.components.SquircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.ui.theme.*
import com.undrift.ui.components.premiumCard

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    onSignInClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        // Top Info Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { }) {
                Icon(PhosphorIcons.Regular.X, contentDescription = "Close", tint = TextSecondary)
            }
            IconButton(onClick = { }) {
                Icon(PhosphorIcons.Regular.Question, contentDescription = "Help", tint = TextSecondary)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo Card with Shared Bounds
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "app_logo_box"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .size(160.dp)
                        .premiumCard(cornerRadius = 32.dp, backgroundColor = SurfaceColor, borderColor = BorderColor, padding = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = BrandPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "UnDrift",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Calm your mind, reclaim your\nfocus with AI assistance.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = SquircleShape(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = DarkBackground
                )
            ) {
                Text("Get Started", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onSignInClick,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Sign In", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Footer
        Text(
            text = "By joining, you agree to our Terms and Privacy Policy.\nYour focus data stays private and encrypted.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OnboardingAgentsScreen(
    onGetStarted: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        // Minimal Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            IconButton(
                onClick = { },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = TextSecondary)
            }
            Text(
                text = "ONBOARDING",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "AI Powered Focus",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Three specialized agents designed\nto crush procrastination and boost\nfocus.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Agent Items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AgentItem(
                icon = PhosphorIcons.Bold.Brain,
                title = "Context-Aware Agent",
                description = "Monitors your environment to send reminders only when they matter most."
            )

            AgentItem(
                icon = PhosphorIcons.Bold.GameController,
                title = "Reward Loop Agent",
                description = "Gamifies focus. Earn coins and unlock badges for every minute of deep work."
            )

            AgentItem(
                icon = PhosphorIcons.Bold.HandPalm,
                title = "Minimal Intervention Agent",
                description = "Quietly operates in the background with gentle haptic feedback."
            )
        }

        // Page Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == 2) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == 2) BrandPrimary else SurfaceVariantColor
                        )
                )
            }
        }

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = SquircleShape(),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = DarkBackground
            )
        ) {
            Text("Begin Journey", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AgentItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .premiumCard(cornerRadius = 24.dp, padding = 16.dp, backgroundColor = SurfaceVariantColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(SquircleShape())
                .background(SurfaceColor)
                .border(1.dp, BorderColor, SquircleShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = BrandPrimary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
