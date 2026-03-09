package com.undrift.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.undrift.data.UserPreferences
import com.undrift.ui.theme.SurfaceColor
import com.undrift.ui.theme.TextSecondary
import com.undrift.utils.AppUsageInfo
import com.undrift.utils.UsageStatsHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppBlockScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf(listOf<AppUsageInfo>()) }
    var blockedApps by remember { mutableStateOf(setOf<String>()) }
    var appLimits by remember { mutableStateOf(mapOf<String, Long>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var selectedAppForLimit by remember { mutableStateOf<AppUsageInfo?>(null) }

    LaunchedEffect(Unit) {
        installedApps = UsageStatsHelper.getInstalledApps(context)
        val profile = userPreferences.userProfileFlow.first()
        blockedApps = profile.blockedApps
        appLimits = profile.appLimits
        isLoading = false
    }

    val filteredApps = installedApps.filter { 
        it.appName.contains(searchQuery, ignoreCase = true) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
                Text("Focus", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Text("App Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = onBack) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            placeholder = { Text("Search apps", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // AI Suggestion Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "AI Focus Agent Suggestion",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Blocked apps are strictly restricted during Focus sessions. Limits apply daily.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "INSTALLED APPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(filteredApps) { app ->
                    val isBlocked = blockedApps.contains(app.packageName)
                    val limitMillis = appLimits[app.packageName] ?: 0L
                    
                    AppItem(
                        app = app,
                        isBlocked = isBlocked,
                        limitMillis = limitMillis,
                        onToggleBlock = { blocked ->
                            scope.launch {
                                if (blocked) {
                                    userPreferences.addBlockedApp(app.packageName)
                                    blockedApps = blockedApps + app.packageName
                                } else {
                                    val newBlocked = blockedApps - app.packageName
                                    val profile = userPreferences.userProfileFlow.first()
                                    userPreferences.saveUserProfile(profile.copy(blockedApps = newBlocked))
                                    blockedApps = newBlocked
                                }
                            }
                        },
                        onSetLimit = {
                            selectedAppForLimit = app
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (selectedAppForLimit != null) {
        var limitInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedAppForLimit = null },
            title = { Text("Set Daily Limit for ${selectedAppForLimit?.appName}") },
            text = {
                Column {
                    Text("Enter limit in minutes (0 to remove limit)", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = limitInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) limitInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        label = { Text("Minutes") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = limitInput.toLongOrNull() ?: 0L
                    scope.launch {
                        userPreferences.setAppLimit(selectedAppForLimit!!.packageName, minutes * 60 * 1000)
                        val profile = userPreferences.userProfileFlow.first()
                        appLimits = profile.appLimits
                        selectedAppForLimit = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForLimit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppItem(
    app: AppUsageInfo,
    isBlocked: Boolean,
    limitMillis: Long,
    onToggleBlock: (Boolean) -> Unit,
    onSetLimit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isBlocked,
            onCheckedChange = onToggleBlock,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = TextSecondary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        app.icon?.let { icon ->
            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, style = MaterialTheme.typography.titleSmall, color = Color.White)
            if (limitMillis > 0) {
                Text(
                    "Limit: ${limitMillis / (60 * 1000)} mins", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
        IconButton(onClick = onSetLimit) {
            Icon(
                Icons.Default.Timer, 
                contentDescription = "Set Limit", 
                tint = if (limitMillis > 0) MaterialTheme.colorScheme.primary else TextSecondary
            )
        }
    }
}
