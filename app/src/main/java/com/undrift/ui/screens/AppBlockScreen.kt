package com.undrift.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.undrift.ui.theme.SurfaceColor
import com.undrift.ui.theme.TextSecondary

@Composable
fun AppBlockScreen(onBack: () -> Unit) {
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
            Text("Block Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = onBack) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            placeholder = { Text("Search", color = TextSecondary) },
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
                        "We've identified 3 apps that frequently break your focus flow during deep work sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            AppSection("SUGGESTED TO BLOCK", listOf(
                AppData("Social Feed", "2h 15m daily usage", true),
                AppData("Video Stream", "1h 45m daily usage", true)
            ))

            Spacer(modifier = Modifier.height(24.dp))

            AppSection("SOCIAL NETWORKING", listOf(
                AppData("Microblog", "48m daily usage", false),
                AppData("Connect App", "32m daily usage", false)
            ))

            Spacer(modifier = Modifier.height(24.dp))

            AppSection("GAMES", listOf(
                AppData("Puzzle Quest", "1h 10m daily usage", false)
            ))
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

data class AppData(val name: String, val usage: String, val isChecked: Boolean)

@Composable
fun AppSection(title: String, apps: List<AppData>) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        apps.forEach { app ->
            AppItem(app)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AppItem(app: AppData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = app.isChecked,
            onCheckedChange = {},
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = TextSecondary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(app.usage, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text("Set Limit", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
    }
}
