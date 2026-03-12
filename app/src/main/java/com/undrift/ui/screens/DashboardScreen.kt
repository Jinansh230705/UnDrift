package com.undrift.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.service.FocusService
import com.undrift.ui.theme.SurfaceColor
import com.undrift.ui.theme.TextSecondary
import com.undrift.utils.AppUsageInfo
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
    var appUsageList by remember { mutableStateOf(listOf<AppUsageInfo>()) }
    var weeklyScreenTime by remember { mutableStateOf(List(7) { 0L }) }
    var showDurationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit, userProfile.demoMode) {
        while (true) {
            if (userProfile.demoMode) {
                val demoWeekly = UsageStatsHelper.getDemoWeeklyScreenTime()
                weeklyScreenTime = demoWeekly
                screenTimeToday = demoWeekly.last()
                appUsageList = UsageStatsHelper.getDemoAppUsageStats(context)
            } else {
                screenTimeToday = UsageStatsHelper.getScreenTimeToday(context)
                appUsageList = UsageStatsHelper.getAppUsageStats(context)
                weeklyScreenTime = UsageStatsHelper.getWeeklyDailyScreenTime(context)
            }
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
                            text = "UNDRIFT",
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

            // Weekly Screen Time Card
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
                                text = "WEEKLY SCREEN TIME",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            val avgMillis = weeklyScreenTime.filter { it > 0 }.let {
                                if (it.isEmpty()) 0L else it.sum() / it.size
                            }
                            val avgH = avgMillis / (60 * 60 * 1000)
                            val avgM = (avgMillis % (60 * 60 * 1000)) / (60 * 1000)
                            Text(
                                text = "Avg ${avgH}h ${avgM}m / day",
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
                    
                    WeeklyScreenTimeGraph(dailyMillis = weeklyScreenTime)
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
                    
                    val hr = userProfile.focusDurationMinutes / 3600
                    val min = (userProfile.focusDurationMinutes % 3600) / 60
                    val sec = userProfile.focusDurationMinutes % 60
                    
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
                                text = String.format("%02dh %02dm %02ds", hr, min, sec),
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

            // Usage Monitor Card (Summary)
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // App Usage List
                    Text("App Breakdown", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    appUsageList.filter { it.usageTimeMillis > 0 }.forEach { app ->
                        val appHours = app.usageTimeMillis / (60 * 60 * 1000)
                        val appMinutes = (app.usageTimeMillis % (60 * 60 * 1000)) / (60 * 1000)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            app.icon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(app.appName, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                            Text("${appHours}h ${appMinutes}m", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
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
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showDurationDialog) {
        CustomTimePickerDialog(
            initialSeconds = userProfile.focusDurationMinutes,
            onDismiss = { showDurationDialog = false },
            onConfirm = { totalSeconds ->
                scope.launch {
                    userPreferences.setFocusDuration(totalSeconds)
                    showDurationDialog = false
                }
            }
        )
    }
}

@Composable
fun CustomTimePickerDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(initialSeconds / 3600) }
    var minutes by remember { mutableIntStateOf((initialSeconds % 3600) / 60) }
    var seconds by remember { mutableIntStateOf(initialSeconds % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Focus Duration") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPicker(value = hours, range = 0..23, label = "HH") { hours = it }
                Text(":", fontWeight = FontWeight.Bold)
                NumberPicker(value = minutes, range = 0..59, label = "MM") { minutes = it }
                Text(":", fontWeight = FontWeight.Bold)
                NumberPicker(value = seconds, range = 0..59, label = "SS") { seconds = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours * 3600 + minutes * 60 + seconds) }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberPicker(value: Int, range: IntRange, label: String, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
        }
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
    }
}

@Composable
fun WeeklyScreenTimeGraph(dailyMillis: List<Long>) {
    val data = if (dailyMillis.size < 7) List(7) { dailyMillis.getOrElse(it) { 0L } } else dailyMillis.takeLast(7)
    // Max is at least 1 hour so the graph always has meaningful scale
    val maxMillis = (data.maxOrNull()?.coerceAtLeast(3_600_000L) ?: 3_600_000L).toFloat()

    // Build day-of-week labels for the last 7 days
    val dayLabels = remember {
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val cal = java.util.Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val c = java.util.Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            }
            names[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                .padding(start = 36.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            // Y-axis hour labels drawn as Text overlays
            val maxHours = (maxMillis / 3_600_000f).let { kotlin.math.ceil(it.toDouble()).toInt().coerceAtLeast(1) }
            val step = if (maxHours <= 4) 1 else if (maxHours <= 10) 2 else (maxHours / 4).coerceAtLeast(1)
            val labels = (0..maxHours step step).toList()

            labels.forEach { h ->
                val fraction = h.toFloat() / maxHours
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "${h}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-32).dp)
                            .offset(y = -(fraction * 196).dp) // rough positioning within 220-24dp area
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barCount = data.size
                val barWidthPx = canvasWidth / barCount * 0.55f
                val gapPx = canvasWidth / barCount

                // Grid lines
                for (h in labels) {
                    val y = canvasHeight - (h.toFloat() / maxHours * canvasHeight)
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Bars
                data.forEachIndexed { index, millis ->
                    val barHeight = (millis / maxMillis) * canvasHeight
                    val x = index * gapPx + (gapPx - barWidthPx) / 2
                    // Bar
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(x, canvasHeight - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Hour label above bar
                    // (drawn as part of the row below instead)
                }
            }
        }

        // Day labels + hour values
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 36.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, millis ->
                val h = millis / 3_600_000
                val m = (millis % 3_600_000) / 60_000
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (h > 0) "${h}h${m}m" else "${m}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp
                    )
                    Text(
                        text = dayLabels.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
