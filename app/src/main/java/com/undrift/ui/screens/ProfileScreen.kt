package com.undrift.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
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
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.ui.theme.Lavender
import com.undrift.ui.theme.Orange
import com.undrift.ui.theme.OrangeColor
import com.undrift.ui.theme.SurfaceColor
import com.undrift.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToShop: () -> Unit,
    onColorSelect: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userProfile.name.isNotEmpty()) userProfile.name.take(1).uppercase() else "U",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = userProfile.name.ifEmpty { "User" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Focus Level: Pro ${userProfile.streakCount / 5 + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Streak & Calendar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Orange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${userProfile.streakCount} Day Streak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                StreakCalendar(streakCount = userProfile.streakCount)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Weekly Summary
        Text("Weekly Summary", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val totalMins = userProfile.streakHistory.sum()
            SummaryItem(modifier = Modifier.weight(1f), label = "TOTAL FOCUS", value = "${totalMins / 60}h ${totalMins % 60}m", trend = "This week")
            SummaryItem(modifier = Modifier.weight(1f), label = "BALANCE", value = "${userProfile.points}", trend = "Focus Coins")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Preferences
        Text("Preferences", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Color Selection
        var showColorDialog by remember { mutableStateOf(false) }
        PreferenceItem(
            icon = Icons.Default.ColorLens,
            title = "Theme Color",
            subtitle = if (userProfile.themeColor == 0xFFCE705D) "Orange" else "Lavender",
            onClick = { showColorDialog = true }
        )

        if (showColorDialog) {
            AlertDialog(
                onDismissRequest = { showColorDialog = false },
                title = { Text("Select Theme Color", color = Color.White) },
                containerColor = SurfaceColor,
                text = {
                    Column {
                        ColorOption("Orange", OrangeColor, userProfile.themeColor == 0xFFCE705D) {
                            onColorSelect(0xFFCE705D)
                            showColorDialog = false
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ColorOption("Lavender", Lavender, userProfile.themeColor == 0xFF685DCE) {
                            onColorSelect(0xFF685DCE)
                            showColorDialog = false
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        PreferenceItem(
            icon = Icons.Default.SmartToy, 
            title = "AI Agent Settings", 
            subtitle = "Focus on performance",
            onClick = onNavigateToAgents
        )
        
        var notificationsEnabled by remember { mutableStateOf(true) }
        PreferenceItem(
            icon = Icons.Default.Notifications, 
            title = "Notifications", 
            subtitle = if (notificationsEnabled) "Smart reminders enabled" else "Notifications disabled", 
            showSwitch = true,
            checked = notificationsEnabled,
            onCheckedChange = { notificationsEnabled = it }
        )
        
        PreferenceItem(
            icon = Icons.Default.EmojiEvents, 
            title = "Rewards Program", 
            subtitle = "${userProfile.points} points available",
            onClick = onNavigateToShop
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Misc
        Text("Misc", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        PreferenceItem(
            icon = Icons.Default.Science,
            title = "Demo Mode",
            subtitle = if (userProfile.demoMode) "Showing sample data" else "Off",
            showSwitch = true,
            checked = userProfile.demoMode,
            onCheckedChange = { enabled ->
                scope.launch { userPreferences.setDemoMode(enabled) }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Log Out",
            modifier = Modifier.fillMaxWidth().clickable { onLogout() },
            textAlign = TextAlign.Center,
            color = Color.Red.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ColorOption(name: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, color = Color.White, modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun StreakCalendar(streakCount: Int) {
    val calendar = Calendar.getInstance()
    val currentMonthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
    val currentYear = calendar.get(Calendar.YEAR)
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    
    // Calculate first day of week and days in month
    val firstDayOfMonth = calendar.clone() as Calendar
    firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonthName ?: "Month",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentYear.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Day Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Grid of Days
        val rows = (daysInMonth + firstDayOfWeek + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - firstDayOfWeek + 1
                    
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val isToday = dayNum == today
                            // Logic: If day is today or within the last 'streakCount' days, mark it as success
                            val isStreakDay = dayNum <= today && dayNum > (today - streakCount)
                            
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isStreakDay -> MaterialTheme.colorScheme.primary
                                            isToday -> Color.White.copy(alpha = 0.1f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 1.dp else 0.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isStreakDay) Color.White else if (isToday) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                    fontWeight = if (isToday || isStreakDay) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryItem(modifier: Modifier, label: String, value: String, trend: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(trend, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable(enabled = !showSwitch) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        if (showSwitch) {
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
