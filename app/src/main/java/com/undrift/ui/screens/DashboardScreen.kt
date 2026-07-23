package com.undrift.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.accessibilityservice.AccessibilityService
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.undrift.ui.components.SquircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.service.FocusService
import com.undrift.service.ContextAwareAgentService
import com.undrift.ui.theme.*
import com.undrift.utils.AppUsageInfo
import com.undrift.utils.MotivationHelper
import com.undrift.utils.UsageStatsHelper
import com.undrift.ui.components.premiumCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val componentName = android.content.ComponentName.unflattenFromString(componentNameString)
        if (componentName != null && componentName.packageName == context.packageName && componentName.className == service.name) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    userProfile: UserProfile,
    userPreferences: UserPreferences,
    onProfileClick: () -> Unit,
    onAddClick: () -> Unit,
    onRewardsClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFocusModeActive by remember { mutableStateOf(false) }
    var screenTimeToday by remember { mutableStateOf(0L) }
    var appUsageList by remember { mutableStateOf(listOf<AppUsageInfo>()) }
    var weeklyScreenTime by remember { mutableStateOf(List(7) { 0L }) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit, userProfile.demoMode) {
        while (true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context, ContextAwareAgentService::class.java)
            
            val (newWeekly, newToday, newApps) = withContext(Dispatchers.IO) {
                if (userProfile.demoMode) {
                    val demoWeekly = UsageStatsHelper.getDemoWeeklyScreenTime()
                    Triple(demoWeekly, demoWeekly.last(), UsageStatsHelper.getDemoAppUsageStats(context))
                } else {
                    Triple(
                        UsageStatsHelper.getWeeklyDailyScreenTime(context),
                        UsageStatsHelper.getScreenTimeToday(context),
                        UsageStatsHelper.getAppUsageStats(context)
                    )
                }
            }
            weeklyScreenTime = newWeekly
            screenTimeToday = newToday
            appUsageList = newApps
            delay(60000)
        }
    }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onProfileClick() }
                ) {
                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "profile_avatar"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceColor)
                                .border(1.dp, BorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userProfile.name.isNotEmpty()) userProfile.name.take(1).uppercase() else "U",
                                color = BrandPrimary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = MotivationHelper.getTimeBasedGreeting(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = userProfile.name.ifEmpty { "User" },
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = onRewardsClick,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, CircleShape)
                    ) {
                        Icon(PhosphorIcons.Bold.Star, contentDescription = "Rewards", tint = BrandPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            if (!isAccessibilityEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumCard(backgroundColor = Color(0xFF1E1010), borderColor = Color(0xFF3D1A1A))
                        .clickable { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(PhosphorIcons.Bold.Warning, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable AI Agent", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Required for intelligent task bypassing.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Focus Mode Session Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Focus Session",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isFocusModeActive) "Deep Work Active" else "Select duration to begin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
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
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = BrandPrimary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceVariantColor
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val hr = userProfile.focusDurationMinutes / 3600
                    val min = (userProfile.focusDurationMinutes % 3600) / 60
                    val sec = userProfile.focusDurationMinutes % 60
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleShape())
                            .background(SurfaceVariantColor)
                            .clickable { showDurationDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(PhosphorIcons.Bold.Timer, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = String.format("%02dh %02dm %02ds", hr, min, sec),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        Icon(PhosphorIcons.Regular.PencilSimple, contentDescription = "Edit Duration", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly Screen Time
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "WEEKLY AVERAGE",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            val avgMillis = weeklyScreenTime.filter { it > 0 }.let {
                                if (it.isEmpty()) 0L else it.sum() / it.size
                            }
                            val avgH = avgMillis / (60 * 60 * 1000)
                            val avgM = (avgMillis % (60 * 60 * 1000)) / (60 * 1000)
                            Text(
                                text = "${avgH}h ${avgM}m",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(PhosphorIcons.Bold.ChartLineUp, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    WeeklyScreenTimeGraph(dailyMillis = weeklyScreenTime)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Breakdown
            val dailyGoalMillis = 5 * 60 * 60 * 1000L
            val progress = (screenTimeToday.toFloat() / dailyGoalMillis).coerceAtMost(1f)
            val hours = screenTimeToday / (60 * 60 * 1000)
            val minutes = (screenTimeToday % (60 * 60 * 1000)) / (60 * 1000)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TODAY'S USAGE", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text("${(progress * 100).toInt()}% Used", style = MaterialTheme.typography.labelMedium, color = BrandPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = BrandSecondary,
                            trackColor = SurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${hours}h ${minutes}m", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    }
                    
                    if (appUsageList.any { it.usageTimeMillis > 0 }) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = BorderColor)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            appUsageList.filter { it.usageTimeMillis > 0 }.forEach { app ->
                                val appHours = app.usageTimeMillis / (60 * 60 * 1000)
                                val appMinutes = (app.usageTimeMillis % (60 * 60 * 1000)) / (60 * 1000)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    app.iconBitmap?.let { bmp ->
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(SquircleShape())
                                        )
                                    } ?: Box(modifier = Modifier.size(32.dp).background(SurfaceVariantColor, SquircleShape()))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(app.appName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Text("${appHours}h ${appMinutes}m", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "BALANCE",
                    value = "${userProfile.points}",
                    icon = PhosphorIcons.Bold.Coins,
                    iconColor = BrandPrimary,
                    onClick = onRewardsClick
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "LEVEL",
                    value = MotivationHelper.getFocusLevel(userProfile.streakCount),
                    icon = PhosphorIcons.Bold.Trophy,
                    iconColor = BrandSecondary,
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
        title = { Text("Set Focus Duration", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
        containerColor = SurfaceColor,
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPicker(value = hours, range = 0..23, label = "HH") { hours = it }
                Text(":", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                NumberPicker(value = minutes, range = 0..59, label = "MM") { minutes = it }
                Text(":", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                NumberPicker(value = seconds, range = 0..59, label = "SS") { seconds = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours * 3600 + minutes * 60 + seconds) }) {
                Text("Save", color = BrandPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun NumberPicker(value: Int, range: IntRange, label: String, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            modifier = Modifier.size(32.dp).background(SurfaceVariantColor, CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            modifier = Modifier.size(32.dp).background(SurfaceVariantColor, CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun WeeklyScreenTimeGraph(dailyMillis: List<Long>) {
    val data = if (dailyMillis.size < 7) List(7) { dailyMillis.getOrElse(it) { 0L } } else dailyMillis.takeLast(7)
    val maxMillis = (data.maxOrNull()?.coerceAtLeast(3_600_000L) ?: 3_600_000L).toFloat()

    val dayLabels = remember {
        val names = listOf("S", "M", "T", "W", "T", "F", "S")
        val cal = java.util.Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val c = java.util.Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            }
            names[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val barTrackColor = SurfaceVariantColor
        val barColor = BrandPrimary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barCount = data.size
                val barWidthPx = 16.dp.toPx()
                val gapPx = canvasWidth / barCount

                // Clean minimal bars
                data.forEachIndexed { index, millis ->
                    val barHeight = (millis / maxMillis) * canvasHeight
                    val x = index * gapPx + (gapPx - barWidthPx) / 2
                    
                    // Track (background bar)
                    drawRoundRect(
                        color = barTrackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidthPx, canvasHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidthPx/2, barWidthPx/2)
                    )
                    
                    // Value (foreground bar)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, canvasHeight - barHeight),
                        size = Size(barWidthPx, barHeight.coerceAtLeast(barWidthPx)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidthPx/2, barWidthPx/2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, _ ->
                Text(
                    text = dayLabels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
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
    Box(
        modifier = modifier
            .premiumCard()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }
    }
}
