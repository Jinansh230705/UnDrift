package com.undrift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.undrift.ui.screens.OnboardingAgentsScreen
import com.undrift.ui.screens.SplashScreen
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
                    "onboarding" -> OnboardingAgentsScreen(onGetStarted = { /* TODO: Navigate to Home */ })
                }
            }
        }
    }
}
