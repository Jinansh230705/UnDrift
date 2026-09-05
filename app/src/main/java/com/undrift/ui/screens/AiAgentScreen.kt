package com.undrift.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.agent.*
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.network.ProxyAiClient
import com.undrift.service.FocusService
import com.undrift.ui.components.SquircleShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AiAgentScreen(
    userProfile: UserProfile,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var apiUrl by remember { mutableStateOf(userProfile.aiApiUrl) }
    var apiKey by remember { mutableStateOf(userProfile.aiApiKey) }
    var isMonitoringEnabled by remember { mutableStateOf(userProfile.isMonitoringEnabled) }
    
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    val rewardAgent: RewardLoopAgent = remember {
        ProxyRewardLoopAgent(ProxyAiClient(), LocalRewardLoopAgent.instance)
    }

    LaunchedEffect(apiUrl, apiKey) {
        hasUnsavedChanges = apiUrl != userProfile.aiApiUrl || apiKey != userProfile.aiApiKey
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PhosphorIcons.Bold.CaretLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (hasUnsavedChanges) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PaddingValues(16.dp).let {
                        Button(
                            onClick = {
                                scope.launch {
                                    userPreferences.updateAiSettings(apiUrl, apiKey)
                                    hasUnsavedChanges = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(it)
                                .height(56.dp),
                            shape = SquircleShape(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Configuration", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Agent Persona Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.Robot,
                        contentDescription = "AI Agent",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "UnDrift Focus Agent",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by AI Proxy & Local Rules. I continuously track your focus usage to reward you with coins and nudge you away from distractions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats / Rewards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Points Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = SquircleShape(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(PhosphorIcons.Bold.Coins, contentDescription = "Points", tint = Color(0xFFFFC107), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${userProfile.points}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Focus Coins", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Streak Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = SquircleShape(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(PhosphorIcons.Bold.Fire, contentDescription = "Streak", tint = Color(0xFFFF5722), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${userProfile.streakCount}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Day Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Monitoring Toggle Card (Status)
            Text(
                text = "Agent Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = SquircleShape(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isMonitoringEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.1f), 
                                SquircleShape()
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMonitoringEnabled) PhosphorIcons.Bold.ShieldCheck else PhosphorIcons.Bold.ShieldSlash, 
                            contentDescription = null, 
                            tint = if (isMonitoringEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Context & Focus Monitoring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMonitoringEnabled) "Active • Tracking focus usage & protecting workflow" else "Paused • App limits are ignored",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    
                    Switch(
                        checked = isMonitoringEnabled,
                        onCheckedChange = { isEnabled ->
                            isMonitoringEnabled = isEnabled
                            scope.launch {
                                userPreferences.setMonitoringEnabled(isEnabled)
                            }
                            
                            if (isEnabled) {
                                try {
                                    val serviceIntent = Intent(context, FocusService::class.java).apply {
                                        action = "START_MONITORING"
                                    }
                                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                                } catch (e: Exception) {
                                    android.util.Log.e("AiAgentScreen", "Failed to start monitoring service", e)
                                }
                            } else {
                                context.stopService(Intent(context, FocusService::class.java))
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Interactive Agent Simulator
            Text(
                text = "Test Agent Triggers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Simulate behavioral events to test how the AI Reward Loop Agent evaluates progress and awards coins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val evaluations by LocalRewardLoopAgent.evaluationsFlow.collectAsState()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val input = RewardEventInput(
                                    event = "FOCUS_USAGE_PROGRESS",
                                    actualFocusDurationMinutes = 10
                                )
                                val output = rewardAgent.evaluate(input)
                                if (output.type != RewardType.NONE) {
                                    val pts = when (output.magnitude) {
                                        RewardMagnitude.HIGH -> 30
                                        RewardMagnitude.MEDIUM -> 20
                                        RewardMagnitude.LOW -> 10
                                    }
                                    userPreferences.updatePoints(pts)
                                    android.widget.Toast.makeText(context, "Agent: +$pts Coins! (${output.message ?: "10m Focus Progress"})", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                    ) {
                        Text("10m Usage (+10)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val input = RewardEventInput(
                                    event = "FOCUS_SESSION_COMPLETED",
                                    plannedDurationMinutes = 25,
                                    actualFocusDurationMinutes = 25
                                )
                                val output = rewardAgent.evaluate(input)
                                if (output.type != RewardType.NONE) {
                                    val pts = when (output.magnitude) {
                                        RewardMagnitude.HIGH -> 500
                                        RewardMagnitude.MEDIUM -> 300
                                        RewardMagnitude.LOW -> 100
                                    }
                                    userPreferences.updatePoints(pts)
                                    android.widget.Toast.makeText(context, "Agent: +$pts Coins! (${output.message ?: "25m Session Completed"})", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("25m Session", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val input = RewardEventInput(
                                    event = "DISTRACTION_RECOVERED"
                                )
                                val output = rewardAgent.evaluate(input)
                                if (output.type != RewardType.NONE) {
                                    val pts = when (output.magnitude) {
                                        RewardMagnitude.HIGH -> 50
                                        RewardMagnitude.MEDIUM -> 25
                                        RewardMagnitude.LOW -> 10
                                    }
                                    userPreferences.updatePoints(pts)
                                    android.widget.Toast.makeText(context, "Agent: +$pts Coins! (${output.message ?: "Recovery rewarded"})", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Recovery (+25)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val input = RewardEventInput(
                                    event = "FOCUS_SESSION_COMPLETED",
                                    plannedDurationMinutes = 120,
                                    actualFocusDurationMinutes = 120,
                                    dailyFocusMinutes = 120
                                )
                                val output = rewardAgent.evaluate(input)
                                if (output.type != RewardType.NONE) {
                                    val pts = when (output.magnitude) {
                                        RewardMagnitude.HIGH -> 500
                                        RewardMagnitude.MEDIUM -> 300
                                        RewardMagnitude.LOW -> 100
                                    }
                                    userPreferences.updatePoints(pts)
                                    android.widget.Toast.makeText(context, "Agent: +$pts Coins! (${output.message ?: "2h Milestone Reached"})", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                    ) {
                        Text("2h Milestone", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent Evaluation History Log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agent Activity Log (${evaluations.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (evaluations.isNotEmpty()) {
                    TextButton(onClick = { rewardAgent.clearEvaluations() }) {
                        Text("Clear Log", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (evaluations.isEmpty()) {
                Card(
                    shape = SquircleShape(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No evaluations recorded yet. Complete focus sessions or tap a trigger button above to test the agent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    evaluations.take(10).forEach { record ->
                        EvaluationRecordCard(record)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // How it works / Triggers
            Text(
                text = "How The Agent Rewards You",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            TriggerCard(
                icon = PhosphorIcons.Bold.Timer,
                title = "Focus Usage Tracking",
                description = "Earn continuous small coin bonuses (10-30 coins) every 5 minutes while staying active in focus mode.",
                iconTint = Color(0xFF009688)
            )
            TriggerCard(
                icon = PhosphorIcons.Bold.CheckCircle,
                title = "Session Completion",
                description = "Earn 300+ coins when you successfully complete a full timed focus session.",
                iconTint = Color(0xFF4CAF50)
            )
            TriggerCard(
                icon = PhosphorIcons.Bold.ArrowUUpLeft,
                title = "Distraction Recovery",
                description = "Earn 25-50 coins when the agent blocks a distracting app and you return to work.",
                iconTint = Color(0xFF2196F3)
            )
            TriggerCard(
                icon = PhosphorIcons.Bold.Medal,
                title = "Milestones & Consistency",
                description = "Get massive bonuses (500 coins) for hitting 2 hours of daily focus or keeping multi-day streaks.",
                iconTint = Color(0xFF9C27B0)
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Context AI Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect an external LLM endpoint to analyze on-screen context and intelligently filter distractions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API URL Input
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("API URL Endpoint") },
                placeholder = { Text("https://api.openai.com/v1/chat/completions") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(PhosphorIcons.Bold.Link, contentDescription = null) },
                shape = SquircleShape(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API Key Input
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(PhosphorIcons.Bold.Key, contentDescription = null) },
                shape = SquircleShape(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun EvaluationRecordCard(record: RewardEvaluationRecord) {
    val badgeColor = when (record.output.type) {
        RewardType.SESSION_COMPLETION -> Color(0xFF4CAF50)
        RewardType.RECOVERY -> Color(0xFF2196F3)
        RewardType.MILESTONE -> Color(0xFF9C27B0)
        RewardType.CONSISTENCY -> Color(0xFFFF9800)
        RewardType.PROGRESS -> Color(0xFF009688)
        RewardType.NONE -> Color.Gray
    }

    Card(
        shape = SquircleShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = SquircleShape()
                ) {
                    Text(
                        text = record.output.type.name,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Mag: ${record.output.magnitude.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.output.message != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${record.output.message}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Trigger Event: ${record.input.event}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun TriggerCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    iconTint: Color
) {
    Card(
        shape = SquircleShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
