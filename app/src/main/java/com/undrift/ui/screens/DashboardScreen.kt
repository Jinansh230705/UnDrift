package com.undrift.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.service.FocusService
import com.undrift.ui.theme.SurfaceColor
import com.undrift.ui.theme.TextSecondary
import com.undrift.utils.UsageStatsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    userProfile: UserProfile,
    userPreferences: UserPreferences,
    onProfileClick: () -> Unit,
    onAddClick: () -> Unit,
    onRewardsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFocusModeActive by remember { mutableStateOf(false) }
    var screenTimeToday by remember { mutableStateOf(0L) }
    var showDurationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            screenTimeToday = UsageStatsHelper.getScreenTimeToday(context)
            delay(30000)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userProfile.name.isNotEmpty()) userProfile.name.take(1).uppercase() else "U",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ANTI-PROCRASTINATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = userProfile.name.ifEmpty { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Row {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                    }
                    IconButton(onClick = { onRewardsClick() }) {
                        Icon(Icons.Default.Stars, contentDescription = "Rewards", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly Streak Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WEEKLY STREAK",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "${userProfile.streakCount} Days Success",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    StreakGraph(history = userProfile.streakHistory)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Focus Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFocusModeActive) MaterialTheme.colorScheme.primary else SurfaceColor
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Focus Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isFocusModeActive) "Deep Work Active" else "Set duration to start",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = isFocusModeActive,
                            onCheckedChange = { active ->
                                isFocusModeActive = active
                                val intent = Intent(context, FocusService::class.java).apply {
                                    action = if (active) "START_FOCUS" else "STOP_FOCUS"
                                    if (active) putExtra("DURATION", userProfile.focusDurationMinutes)
                                }
                                if (active) {
                                    ContextCompat.startForegroundService(context, intent)
                                } else {
                                    context.startService(intent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable { showDurationDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${userProfile.focusDurationMinutes} Minutes",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Edit Duration", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Usage Monitor Card
            val dailyGoalMillis = 5 * 60 * 60 * 1000L
            val progress = (screenTimeToday.toFloat() / dailyGoalMillis).coerceAtMost(1f)
            val hours = screenTimeToday / (60 * 60 * 1000)
            val minutes = (screenTimeToday % (60 * 60 * 1000)) / (60 * 1000)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("USAGE MONITOR", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text("${(progress * 100).toInt()}% Used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Screen Time Today",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Black.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${hours}h ${minutes}m / 5h", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Balance and Level
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "BALANCE",
                    value = "${userProfile.points}",
                    icon = Icons.Default.MonetizationOn,
                    iconColor = Color(0xFFFFD700),
                    onClick = onRewardsClick
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "LEVEL",
                    value = "Pro ${userProfile.streakCount / 5 + 1}",
                    icon = Icons.Default.MilitaryTech,
                    iconColor = Color(0xFF9C27B0),
                    onClick = onProfileClick
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDurationDialog) {
        var tempDuration by remember { mutableStateOf(userProfile.focusDurationMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            title = { Text("Set Focus Duration") },
            text = {
                TextField(
                    value = tempDuration,
                    onValueChange = { if (it.all { char -> char.isDigit() }) tempDuration = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Minutes") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        userPreferences.setFocusDuration(tempDuration.toIntOrNull() ?: 60)
                        showDurationDialog = false
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun StreakGraph(history: List<Int>) {
    val data = if (history.isEmpty()) listOf(0, 0, 0, 0, 0, 0, 0) else history
    val maxValue = data.maxOrNull()?.coerceAtLeast(60) ?: 60
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = if (data.size > 1) width / (data.size - 1) else width
        
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - (value.toFloat() / maxValue * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = Color(0xFF4CAF50),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
