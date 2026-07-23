package com.undrift.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.undrift.ui.components.SquircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.ui.theme.*
import com.undrift.ui.components.premiumCard
import com.undrift.utils.MotivationHelper
import com.undrift.utils.UpdateManager
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToShop: () -> Unit,
    onColorSelect: (Long) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .premiumCard(cornerRadius = 16.dp, padding = 0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(48.dp)
                        .premiumCard(cornerRadius = 16.dp, padding = 0.dp)
                ) {
                    Icon(PhosphorIcons.Bold.ShareNetwork, contentDescription = "Share", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Profile info with Shared Avatar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = userProfile.name.ifEmpty { "User" },
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Focus Level: ${MotivationHelper.getFocusLevel(userProfile.streakCount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandSecondary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Streak Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard()
            ) {
                StreakCalendar(streakCount = userProfile.streakCount, bestStreak = userProfile.bestStreak)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Weekly Summary
            Text(
                text = "WEEKLY SUMMARY",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val totalMins = userProfile.streakHistory.sum()
                SummaryItem(modifier = Modifier.weight(1f), label = "TOTAL FOCUS", value = "${totalMins / 60}h ${totalMins % 60}m", trend = "This week")
                SummaryItem(modifier = Modifier.weight(1f), label = "BALANCE", value = "${userProfile.points}", trend = "Focus Coins")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Preferences Section
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Theme selector item
            var showThemeDialog by remember { mutableStateOf(false) }
            PreferenceItem(
                icon = PhosphorIcons.Bold.Palette,
                title = "App Theme",
                subtitle = when (userProfile.themeMode) {
                    1 -> "Light Mode"
                    2 -> "Dark Mode"
                    else -> "System Default"
                },
                onClick = { showThemeDialog = true }
            )

            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text("Select Theme", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
                    containerColor = SurfaceColor,
                    text = {
                        Column {
                            ThemeOption("System Default", userProfile.themeMode == 0) {
                                scope.launch { userPreferences.setThemeMode(0) }
                                showThemeDialog = false
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            ThemeOption("Light Mode", userProfile.themeMode == 1) {
                                scope.launch { userPreferences.setThemeMode(1) }
                                showThemeDialog = false
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            ThemeOption("Dark Mode", userProfile.themeMode == 2) {
                                scope.launch { userPreferences.setThemeMode(2) }
                                showThemeDialog = false
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }

            PreferenceItem(
                icon = PhosphorIcons.Bold.Robot, 
                title = "AI Agent Settings", 
                subtitle = "Focus on performance",
                onClick = onNavigateToAgents
            )
            
            var notificationsEnabled by remember { mutableStateOf(true) }
            PreferenceItem(
                icon = PhosphorIcons.Bold.Bell, 
                title = "Notifications", 
                subtitle = if (notificationsEnabled) "Smart reminders enabled" else "Notifications disabled", 
                showSwitch = true,
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
            
            PreferenceItem(
                icon = PhosphorIcons.Bold.Trophy, 
                title = "Rewards Program", 
                subtitle = "${userProfile.points} points available",
                onClick = onNavigateToShop
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Misc Section
            Text(
                text = "MISC",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PreferenceItem(
                icon = PhosphorIcons.Bold.Flask,
                title = "Demo Mode",
                subtitle = if (userProfile.demoMode) "Showing sample data" else "Off",
                showSwitch = true,
                checked = userProfile.demoMode,
                onCheckedChange = { enabled ->
                    scope.launch { userPreferences.setDemoMode(enabled) }
                }
            )

            val context = LocalContext.current
            PreferenceItem(
                icon = PhosphorIcons.Bold.DownloadSimple,
                title = "Check for Updates",
                subtitle = "Ensure you have the latest features",
                onClick = {
                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val url = UpdateManager.checkForUpdates(context)
                        if (url != null) {
                            Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
                            UpdateManager.downloadUpdate(context, url)
                        } else {
                            Toast.makeText(context, "Already on the latest version", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "Log Out",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFFFF453A),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ThemeOption(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape())
            .background(if (isSelected) SurfaceVariantColor else Color.Transparent)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(PhosphorIcons.Bold.Check, contentDescription = null, tint = BrandPrimaryColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun StreakCalendar(streakCount: Int, bestStreak: Int) {
    val calendar = Calendar.getInstance()
    // Determine today's index where Monday = 0, Sunday = 6
    val todayDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Week Streak", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = streakCount.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "days",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, day ->
                val isToday = index == todayDayOfWeek
                // A day is considered completed if it falls within the current streak length
                // counting backwards from today.
                val completed = streakCount > (todayDayOfWeek - index) && index <= todayDayOfWeek
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    completed -> BrandPrimaryColor
                                    isToday -> BorderColor
                                    else -> SurfaceVariantColor
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (completed) {
                            Icon(PhosphorIcons.Bold.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) TextPrimary else TextSecondary,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = MotivationHelper.getStreakMessage(streakCount, bestStreak),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SummaryItem(modifier: Modifier, label: String, value: String, trend: String) {
    Box(
        modifier = modifier.premiumCard()
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(trend, style = MaterialTheme.typography.labelMedium, color = BrandSecondary)
        }
    }
}

@Composable
fun PreferenceItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showSwitch: Boolean = false,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .premiumCard(padding = 16.dp, cornerRadius = 16.dp)
            .clickable(enabled = !showSwitch) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(SquircleShape())
                .background(SurfaceVariantColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        if (showSwitch) {
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBackground,
                    checkedTrackColor = BrandPrimary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceVariantColor
                )
            )
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
