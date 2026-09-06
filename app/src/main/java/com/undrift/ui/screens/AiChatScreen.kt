package com.undrift.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.undrift.network.ChatMessage
import com.undrift.network.ConduitClient
import com.undrift.ui.theme.BrandPrimary
import com.undrift.ui.theme.DarkBackground
import com.undrift.ui.theme.SurfaceVariantColor
import com.undrift.ui.theme.TextPrimary
import com.undrift.ui.theme.TextSecondary
import com.undrift.ui.components.premiumCard
import com.undrift.data.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AiChatScreen(
    userProfile: UserProfile,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val scope = rememberCoroutineScope()
    val aiClient = remember { ConduitClient() }
    
    var messages by remember { 
        mutableStateOf(
            listOf(
                ChatMessage("system", "You are the UnDrift AI assistant. You help the user reflect on their focus sessions, explain how the minimal intervention agent operates, and offer supportive guidance for managing digital distractions. The user's current dashboard data: Streak=${userProfile.streakCount}, Points=${userProfile.points}."),
                ChatMessage("assistant", "Hello! I'm the UnDrift AI. Ask me about your focus session, your dashboard stats, or how I determine when to intervene.")
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .premiumCard(cornerRadius = 16.dp, padding = 0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "AI Agent Chat",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your focus...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantColor,
                        unfocusedContainerColor = SurfaceVariantColor,
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isTyping) {
                            val userMsg = ChatMessage("user", inputText)
                            messages = messages + userMsg
                            inputText = ""
                            isTyping = true
                            
                            scope.launch {
                                try {
                                    val response = aiClient.chatCompletion(messages)
                                    messages = messages + ChatMessage("assistant", response)
                                } catch (e: Exception) {
                                    val err = e.message ?: e.toString()
                                    val regex = Regex("Please retry in [0-9.]+s")
                                    val match = regex.find(err)
                                    val displayMsg = if (match != null) {
                                        "You have exceeded your API quota. ${match.value}."
                                    } else {
                                        "Sorry, I encountered an error: $err"
                                    }
                                    messages = messages + ChatMessage("assistant", displayMsg)
                                } finally {
                                    isTyping = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BrandPrimary),
                    enabled = !isTyping && inputText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val displayMessages = messages.filter { it.role != "system" }
            items(displayMessages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (isUser) 20.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 20.dp
                                )
                            )
                            .background(if (isUser) BrandPrimary else SurfaceVariantColor)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = msg.content,
                            color = if (isUser) Color.White else TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            if (isTyping) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                                .background(SurfaceVariantColor)
                                .padding(16.dp)
                        ) {
                            Text("Typing...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
