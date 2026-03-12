package com.undrift.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.unit.sp
import com.undrift.data.MongoRepository
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    onAuthSuccess: (UserProfile) -> Unit,
    mongoRepository: MongoRepository,
    userPreferences: UserPreferences
) {
    var isSignUp by remember { mutableStateOf(false) } // Default to login for faster entry
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = if (isSignUp) "Start your journey to better focus" else "Login to continue your progress",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isSignUp) {
            AuthTextField(value = name, onValueChange = { name = it }, label = "Full Name", icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
        }

        AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            ),
            singleLine = true
        )
        
        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = age, onValueChange = { age = it }, label = "Age", icon = Icons.Default.Numbers)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = goal, onValueChange = { goal = it }, label = "Your Primary Goal", icon = Icons.Default.Flag)
        }

        Spacer(modifier = Modifier.height(32.dp))

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
                            // FAST LOGIN: Check local first
                            val localProfile = withContext(Dispatchers.IO) { userPreferences.userProfileFlow.first() }
                            if (localProfile.email == email && localProfile.password == password) {
                                withContext(Dispatchers.Main) {
                                    onAuthSuccess(localProfile.copy(isLoggedIn = true))
                                }
                                return@launch
                            }

                            // If not found locally or password mismatch, try Mongo
                            try {
                                val userDoc = withContext(Dispatchers.IO) { mongoRepository.findUserByEmail(email) }
                                if (userDoc != null) {
                                    val dbPassword = userDoc.getString("password")
                                    if (dbPassword == password) {
                                        val profile = UserProfile(
                                            name = userDoc.getString("name") ?: "",
                                            email = userDoc.getString("email") ?: "",
                                            password = dbPassword ?: "",
                                            age = userDoc.getString("age") ?: "",
                                            goal = userDoc.getString("goal") ?: "",
                                            isLoggedIn = true
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
                                Log.e("AuthScreen", "Mongo Login error", e)
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    Toast.makeText(context, "Login failed. Check connection.", Toast.LENGTH_SHORT).show()
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
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(if (isSignUp) "Sign Up" else "Login", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(
                text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                color = MaterialTheme.colorScheme.primary
            )
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
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White
        ),
        singleLine = true
    )
}
