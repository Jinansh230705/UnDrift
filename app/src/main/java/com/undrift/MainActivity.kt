package com.undrift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undrift.data.MongoRepository
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.ui.screens.*
import com.undrift.ui.theme.UnDriftTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var userPreferences: UserPreferences
    private val mongoRepository = MongoRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(this)
        
        enableEdgeToEdge()
        setContent {
            UnDriftTheme {
                val userProfile by userPreferences.userProfileFlow.collectAsStateWithLifecycle(
                    initialValue = UserProfile("", "", "", "", false)
                )
                
                var currentScreen by remember { mutableStateOf("splash") }
                val scope = rememberCoroutineScope()

                when (currentScreen) {
                    "splash" -> SplashScreen(
                        onGetStarted = { currentScreen = "onboarding" },
                        onSignInClick = { currentScreen = "auth" }
                    )
                    "auth" -> AuthScreen(
                        onAuthSuccess = { profile ->
                            scope.launch {
                                // Save locally
                                userPreferences.saveUserProfile(profile)
                                // Push to MongoDB if it's a new user (usually you'd check this)
                                mongoRepository.saveUserToMongo(profile)
                                currentScreen = "dashboard"
                            }
                        },
                        onBack = { currentScreen = "splash" },
                        mongoRepository = mongoRepository
                    )
                    "onboarding" -> OnboardingAgentsScreen(onGetStarted = { 
                        if (userProfile.isLoggedIn) currentScreen = "dashboard" 
                        else currentScreen = "auth" 
                    })
                    "dashboard" -> DashboardScreen(
                        onProfileClick = { currentScreen = "profile" },
                        onAddClick = { currentScreen = "app_block" },
                        onRewardsClick = { currentScreen = "rewards" }
                    )
                    "profile" -> ProfileScreen(
                        userName = userProfile.name,
                        onBack = { currentScreen = "dashboard" },
                        onLogout = {
                            scope.launch {
                                userPreferences.logout()
                                currentScreen = "splash"
                            }
                        }
                    )
                    "app_block" -> AppBlockScreen(onBack = { currentScreen = "dashboard" })
                    "rewards" -> RewardsShopScreen(onBack = { currentScreen = "dashboard" })
                    "nudge" -> FocusNudgeScreen(
                        onBackToFocus = { currentScreen = "dashboard" },
                        onNeedTime = { currentScreen = "rewards" }
                    )
                }
            }
        }
    }
}
