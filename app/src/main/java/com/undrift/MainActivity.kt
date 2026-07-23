package com.undrift

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.regular.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.undrift.data.MongoRepository
import com.undrift.data.UserPreferences
import com.undrift.data.UserProfile
import com.undrift.service.FocusService
import com.undrift.ui.screens.*
import com.undrift.ui.theme.UnDriftTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeDefaults

class MainActivity : ComponentActivity() {
    private lateinit var userPreferences: UserPreferences
    private val mongoRepository by lazy { MongoRepository() }
    
    // State to track the latest intent for navigation
    private var intentState = mutableStateOf<Intent?>(null)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Temporary receiver to restore user's streak
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                lifecycleScope.launch {
                    val prefs = UserPreferences(this@MainActivity)
                    prefs.forceRestoreStreak(2)
                    android.widget.Toast.makeText(this@MainActivity, "Streak Restored to 2!", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, android.content.IntentFilter("com.undrift.RESTORE_STREAK"), android.content.Context.RECEIVER_EXPORTED)
        } else {
            androidx.core.content.ContextCompat.registerReceiver(this, receiver, android.content.IntentFilter("com.undrift.RESTORE_STREAK"), androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
        }

        enableEdgeToEdge()
        userPreferences = UserPreferences(this)
        intentState.value = intent
        
        checkPermissions()
        
        lifecycleScope.launch {
            val profile = userPreferences.userProfileFlow.first()
            if (profile.isMonitoringEnabled) {
                startMonitoringService()
            }
        }
        
        enableEdgeToEdge()
        setContent {
            val userProfile by userPreferences.userProfileFlow.collectAsStateWithLifecycle(
                initialValue = UserProfile("", "", "", "", "", false, true)
            )
            
            UnDriftTheme(themeMode = userProfile.themeMode) {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val hazeState = remember { HazeState() }

                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination
                
                val showBottomBar = currentDestination?.route?.startsWith("nudge") != true && 
                                    currentDestination?.route in listOf("dashboard", "ai_agent", "rewards", "profile")

                // Handle incoming Intent for navigation using State
                val currentIntent by intentState
                LaunchedEffect(currentIntent) {
                    currentIntent?.let {
                        processIntent(it, navController)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(
                                navController = navController,
                                hazeState = hazeState,
                                onAddClick = { navController.navigate("app_block") }
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    SharedTransitionLayout {
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = when {
                                    userProfile.isLoggedIn -> "dashboard"
                                    userProfile.isFirstLaunch -> "splash"
                                    else -> "auth"
                                },
                                modifier = Modifier.fillMaxSize().haze(state = hazeState),
                                enterTransition = {
                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + 
                                androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(300))
                            },
                            exitTransition = {
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + 
                                androidx.compose.animation.scaleOut(targetScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(300))
                            },
                            popEnterTransition = {
                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + 
                                androidx.compose.animation.scaleIn(initialScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(300))
                            },
                            popExitTransition = {
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + 
                                androidx.compose.animation.scaleOut(targetScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(300))
                            }
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    onGetStarted = { navController.navigate("onboarding") },
                                    onSignInClick = { navController.navigate("auth") },
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("auth") {
                                AuthScreen(
                                    onAuthSuccess = { profile ->
                                        scope.launch {
                                            userPreferences.saveUserProfile(profile)
                                            navController.navigate("dashboard") {
                                                popUpTo("auth") { inclusive = true }
                                            }
                                        }
                                    },
                                    mongoRepository = mongoRepository,
                                    userPreferences = userPreferences,
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("onboarding") {
                                OnboardingAgentsScreen(
                                    onGetStarted = { 
                                        scope.launch {
                                            userPreferences.setFirstLaunchCompleted()
                                            navController.navigate("auth")
                                        }
                                    },
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("dashboard") {
                                DashboardScreen(
                                    userProfile = userProfile,
                                    userPreferences = userPreferences,
                                    onProfileClick = { navController.navigate("profile") },
                                    onAddClick = { navController.navigate("app_block") },
                                    onRewardsClick = { navController.navigate("rewards") },
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("profile") {
                                ProfileScreen(
                                    userProfile = userProfile,
                                    userPreferences = userPreferences,
                                    onBack = { navController.popBackStack() },
                                    onLogout = {
                                        scope.launch {
                                            userPreferences.logout()
                                            navController.navigate("auth") {
                                                popUpTo(0)
                                            }
                                        }
                                    },
                                    onNavigateToAgents = { navController.navigate("ai_agent") },
                                    onNavigateToShop = { navController.navigate("rewards") },
                                    onColorSelect = { color ->
                                        scope.launch {
                                            userPreferences.setThemeColor(color)
                                        }
                                    },
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("app_block") {
                                AppBlockScreen(
                                    onBack = { navController.popBackStack() },
                                    userPreferences = userPreferences,
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("rewards") {
                                RewardsShopScreen(
                                    onBack = { navController.popBackStack() },
                                    userProfile = userProfile,
                                    userPreferences = userPreferences,
                                    mongoRepository = mongoRepository,
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable(
                                route = "nudge?package={package}&reason={reason}",
                                arguments = listOf(
                                    navArgument("package") { type = NavType.StringType; nullable = true },
                                    navArgument("reason") { type = NavType.StringType; nullable = true }
                                )
                            ) { backStackEntry ->
                                val pkg = backStackEntry.arguments?.getString("package")
                                val reason = backStackEntry.arguments?.getString("reason")
                                FocusNudgeScreen(
                                    packageName = pkg,
                                    reason = reason,
                                    onBackToFocus = { 
                                        val startMain = Intent(Intent.ACTION_MAIN)
                                        startMain.addCategory(Intent.CATEGORY_HOME)
                                        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(startMain)
                                    },
                                    onNeedTime = { navController.navigate("rewards") },
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("ai_agent") {
                                AiAgentScreen(
                                    userProfile = userProfile,
                                    userPreferences = userPreferences,
                                    onBack = { navController.popBackStack() },
                                    animatedVisibilityScope = this@composable,
                                    sharedTransitionScope = this@SharedTransitionLayout
                                )
                            }
                            composable("badges") {
                                ComingSoonScreen("Badges & Achievements")
                            }
                        }
                        
                        // Top progressive blur overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 48.dp)
                                .align(Alignment.TopCenter)
                                .hazeChild(state = hazeState)
                        )
                    }
                }
            }
        }
    }
}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }

    private fun processIntent(intent: Intent, navController: NavHostController) {
        val screen = intent.getStringExtra("SCREEN")
        if (screen == "nudge") {
            val pkg = intent.getStringExtra("PACKAGE") ?: ""
            val reason = intent.getStringExtra("REASON") ?: ""
            val forceOverlay = intent.getBooleanExtra("FORCE_OVERLAY", false)
            
            Log.d("MainActivity", "Processing nudge for package: $pkg, forceOverlay: $forceOverlay")
            
            // Navigate to nudge screen with animations disabled to prevent flickering
            navController.navigate("nudge?package=$pkg&reason=$reason") {
                // Clear the backstack to prevent overlay bypass
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    private fun checkPermissions() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun startMonitoringService() {
        val serviceIntent = Intent(this, FocusService::class.java).apply {
            action = "START_MONITORING"
        }
        androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
    }
}

@Composable
fun ComingSoonScreen(featureName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            PhosphorIcons.Bold.Sparkle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "$featureName Coming Soon",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Our Focus Agents are working hard to bring this feature to life. Stay tuned!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    hazeState: HazeState,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeChild(state = hazeState)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = CircleShape,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "dashboard" } == true,
                    onClick = { navigateToRoute(navController, "dashboard") },
                    iconRegular = PhosphorIcons.Regular.HouseLine,
                    iconFilled = PhosphorIcons.Bold.HouseLine,
                    label = "Dash"
                )
                
                NavItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "ai_agent" } == true,
                    onClick = { navigateToRoute(navController, "ai_agent") },
                    iconRegular = PhosphorIcons.Regular.Atom,
                    iconFilled = PhosphorIcons.Bold.Atom,
                    label = "Agent"
                )
                
                // Add Button (Floating inside the pill)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(PhosphorIcons.Bold.Plus, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                
                NavItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "rewards" } == true,
                    onClick = { navigateToRoute(navController, "rewards") },
                    iconRegular = PhosphorIcons.Regular.Storefront,
                    iconFilled = PhosphorIcons.Bold.Storefront,
                    label = "Shop"
                )
                
                NavItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
                    onClick = { navigateToRoute(navController, "profile") },
                    iconRegular = PhosphorIcons.Regular.UserCircle,
                    iconFilled = PhosphorIcons.Bold.UserCircle,
                    label = "Profile"
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconRegular: androidx.compose.ui.graphics.vector.ImageVector,
    iconFilled: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) iconFilled else iconRegular,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun navigateToRoute(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
