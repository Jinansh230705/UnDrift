package com.undrift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.undrift.ui.screens.*
import com.undrift.ui.theme.UnDriftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnDriftTheme {
                var currentScreen by remember { mutableStateOf("splash") }

                when (currentScreen) {
                    "splash" -> SplashScreen(onGetStarted = { currentScreen = "onboarding" })
                    "onboarding" -> OnboardingAgentsScreen(onGetStarted = { currentScreen = "dashboard" })
                    "dashboard" -> DashboardScreen(
                        onProfileClick = { currentScreen = "profile" },
                        onAddClick = { currentScreen = "app_block" },
                        onRewardsClick = { currentScreen = "rewards" }
                    )
                    "profile" -> ProfileScreen(onBack = { currentScreen = "dashboard" })
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
