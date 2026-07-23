package com.undrift.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.undrift.ui.components.SquircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import com.undrift.data.MongoRepository
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.ui.theme.*
import com.undrift.ui.components.premiumCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (UserProfile) -> Unit,
    mongoRepository: MongoRepository,
    userPreferences: UserPreferences,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // App Logo with Shared Bounds Transition from Splash
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "app_logo_box"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .size(80.dp)
                        .premiumCard(cornerRadius = 20.dp, backgroundColor = SurfaceColor, borderColor = BorderColor, padding = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = BrandPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = if (isSignUp) "Create Account" else "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isSignUp) "Start your journey to better focus" else "Login to continue your progress",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Form inputs wrapper
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isSignUp) {
                    AuthTextField(value = name, onValueChange = { name = it }, label = "Full Name", icon = PhosphorIcons.Regular.User)
                }

                AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = PhosphorIcons.Regular.EnvelopeSimple)

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = TextSecondary) },
                    leadingIcon = { Icon(PhosphorIcons.Regular.Lock, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        val image = if (passwordVisible) PhosphorIcons.Regular.Eye else PhosphorIcons.Regular.EyeSlash
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = TextSecondary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = SquircleShape(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = SurfaceColor,
                        unfocusedContainerColor = SurfaceColor,
                        unfocusedTextColor = TextPrimary,
                        focusedTextColor = TextPrimary,
                        focusedLeadingIconColor = BrandPrimary,
                        unfocusedLeadingIconColor = TextSecondary,
                        focusedTrailingIconColor = BrandPrimary,
                        unfocusedTrailingIconColor = TextSecondary
                    ),
                    singleLine = true
                )
                
                if (isSignUp) {
                    AuthTextField(value = age, onValueChange = { age = it }, label = "Age", icon = PhosphorIcons.Regular.Hash)
                    AuthTextField(value = goal, onValueChange = { goal = it }, label = "Your Primary Goal", icon = PhosphorIcons.Regular.Flag)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandPrimary, strokeWidth = 3.dp)
                }
            } else {
                Button(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            scope.launch {
                                if (isSignUp) {
                                    if (name.isEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    val profile = UserProfile(name, email, password, age, goal, true)
                                    
                                    try {
                                        val success = withContext(Dispatchers.IO) { mongoRepository.saveUserToMongo(profile) }
                                        withContext(Dispatchers.Main) {
                                            onAuthSuccess(profile)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            onAuthSuccess(profile)
                                        }
                                    }
                                } else {
                                    val localProfile = withContext(Dispatchers.IO) { userPreferences.userProfileFlow.first() }

                                    try {
                                        val userDoc = withContext(Dispatchers.IO) { mongoRepository.findUserByEmail(email) }
                                        if (userDoc != null) {
                                            val dbPassword = userDoc.getString("password")
                                            if (dbPassword == password) {
                                                @Suppress("UNCHECKED_CAST")
                                                val profile = UserProfile(
                                                    name = userDoc.getString("name") ?: "",
                                                    email = userDoc.getString("email") ?: "",
                                                    password = dbPassword ?: "",
                                                    age = userDoc.getString("age") ?: "",
                                                    goal = userDoc.getString("goal") ?: "",
                                                    points = userDoc.getInteger("points") ?: 0,
                                                    streakCount = userDoc.getInteger("streakCount") ?: 0,
                                                    bestStreak = userDoc.getInteger("bestStreak") ?: 0,
                                                    streakHistory = (userDoc.get("streakHistory") as? List<Int>) ?: emptyList(),
                                                    lastStreakDate = userDoc.getLong("lastStreakDate") ?: 0L,
                                                    focusDurationMinutes = userDoc.getInteger("focusDurationMinutes") ?: 60,
                                                    lastExtraTimePurchaseDate = userDoc.getLong("lastExtraTimePurchaseDate") ?: 0L,
                                                    blockedApps = (userDoc.get("blockedApps") as? List<String>)?.toSet() ?: emptySet(),
                                                    isLoggedIn = true,
                                                    isFirstLaunch = false
                                                )
                                                withContext(Dispatchers.Main) {
                                                    onAuthSuccess(profile)
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                    Toast.makeText(context, "Incorrect password.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                Toast.makeText(context, "User not found.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (localProfile.email == email && localProfile.password == password) {
                                            withContext(Dispatchers.Main) {
                                                onAuthSuccess(localProfile.copy(isLoggedIn = true))
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = SquircleShape(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(if (isSignUp) "Sign Up" else "Login", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { isSignUp = !isSignUp },
                colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = SquircleShape(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = SurfaceColor,
            unfocusedContainerColor = SurfaceColor,
            unfocusedTextColor = TextPrimary,
            focusedTextColor = TextPrimary,
            focusedLeadingIconColor = BrandPrimary,
            unfocusedLeadingIconColor = TextSecondary
        ),
        singleLine = true
    )
}
