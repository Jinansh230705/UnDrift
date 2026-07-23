package com.undrift.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.undrift.ui.components.SquircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.data.UserPreferences
import com.undrift.ui.theme.*
import com.undrift.utils.AppUsageInfo
import com.undrift.utils.UsageStatsHelper
import com.undrift.ui.components.premiumCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppBlockScreen(
    onBack: () -> Unit,
    userPreferences: UserPreferences,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
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
        installedApps = withContext(Dispatchers.IO) {
            UsageStatsHelper.getInstalledApps(context)
        }
        val profile = userPreferences.userProfileFlow.first()
        blockedApps = profile.blockedApps
        appLimits = profile.appLimits
        isLoading = false
    }

    val filteredApps = installedApps.filter { 
        it.appName.contains(searchQuery, ignoreCase = true) 
    }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .animateContentSize()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .premiumCard(cornerRadius = 16.dp, padding = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("App Controls", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                TextButton(onClick = onBack) {
                    Text("Done", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps", color = TextSecondary) },
                leadingIcon = { Icon(PhosphorIcons.Regular.MagnifyingGlass, contentDescription = null, tint = TextSecondary) },
                shape = SquircleShape(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    unfocusedTextColor = TextPrimary,
                    focusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandPrimary, strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        // Info Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .premiumCard()
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(SquircleShape())
                                        .background(SurfaceVariantColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        PhosphorIcons.Bold.Sparkle,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Focus Agent Rule",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Blocked apps are strictly restricted during sessions. Limits apply daily outside sessions.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            "INSTALLED APPS",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    items(filteredApps, key = { it.packageName }) { app ->
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
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    if (selectedAppForLimit != null) {
        var limitInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedAppForLimit = null },
            containerColor = SurfaceColor,
            title = { Text("Daily Limit", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("Enter limit in minutes (0 to remove limit) for ${selectedAppForLimit?.appName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) limitInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        label = { Text("Minutes", color = TextSecondary) },
                        shape = SquircleShape(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
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
                    Text("Save", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForLimit = null }) {
                    Text("Cancel", color = TextSecondary)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .premiumCard(padding = 16.dp, backgroundColor = SurfaceVariantColor, cornerRadius = 16.dp)
            .clickable { onToggleBlock(!isBlocked) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            app.iconBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SquircleShape())
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                if (limitMillis > 0) {
                    Text(
                        "Limit: ${limitMillis / (60 * 1000)} mins", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = BrandSecondary
                    )
                } else {
                    Text(app.packageName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1)
                }
            }
            IconButton(
                onClick = onSetLimit,
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape())
                    .background(SurfaceColor)
            ) {
                Icon(
                    PhosphorIcons.Regular.Timer, 
                    contentDescription = "Set Limit", 
                    tint = if (limitMillis > 0) BrandSecondary else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isBlocked,
                onCheckedChange = onToggleBlock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBackground,
                    checkedTrackColor = BrandPrimary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceColor
                )
            )
        }
    }
}
