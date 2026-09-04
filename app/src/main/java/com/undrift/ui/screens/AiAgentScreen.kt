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

    var isMonitoringEnabled by remember { mutableStateOf(userProfile.isMonitoringEnabled) }

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
                            text = "AI Context Agent",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMonitoringEnabled) "Active and protecting your focus" else "Agent is currently turned off",
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
            
            Text(
                text = "Agent Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Control whether the AI agent can monitor your app usage and help protect your focus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
