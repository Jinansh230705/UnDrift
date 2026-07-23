package com.undrift.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.CaretLeft
import com.adamglin.phosphoricons.bold.Gear
import com.adamglin.phosphoricons.bold.Link
import com.adamglin.phosphoricons.bold.ShieldCheck
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
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

    LaunchedEffect(apiUrl, apiKey) {
        if (apiUrl != userProfile.aiApiUrl || apiKey != userProfile.aiApiKey) {
            hasUnsavedChanges = true
        } else {
            hasUnsavedChanges = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Context Agent", fontWeight = FontWeight.Bold) },
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

            // Monitoring Toggle Card
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), SquircleShape()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(PhosphorIcons.Bold.ShieldCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Usage Monitoring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMonitoringEnabled) "Running and protecting focus" else "Monitoring is currently paused",
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
                                val serviceIntent = Intent(context, FocusService::class.java).apply {
                                    action = "START_MONITORING"
                                }
                                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                            } else {
                                context.stopService(Intent(context, FocusService::class.java))
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Context AI Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect an external LLM to analyze your on-screen context and detect Doom Scrolling automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // API URL Input
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("API URL (e.g. OpenAI / Gemini endpoint)") },
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
                leadingIcon = { Icon(PhosphorIcons.Bold.Gear, contentDescription = null) },
                shape = SquircleShape(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(100.dp)) // Extra padding for bottom bar
        }
    }
}
