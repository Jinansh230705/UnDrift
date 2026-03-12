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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

class MainActivity : ComponentActivity() {
    private lateinit var userPreferences: UserPreferences
    private val mongoRepository by lazy { MongoRepository() }
    
    // State to track the latest intent for navigation
    private var intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(this)
        intentState.value = intent
        
        checkPermissions()
        startMonitoringService()
        
        enableEdgeToEdge()
        setContent {
            val userProfile by userPreferences.userProfileFlow.collectAsStateWithLifecycle(
                initialValue = UserProfile("", "", "", "", "", false, true)
            )
            
            UnDriftTheme(themeColor = Color(userProfile.themeColor)) {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination
                
                val showBottomBar = currentDestination?.route?.startsWith("nudge") != true && 
                                    currentDestination?.route in listOf("dashboard", "ai_agent", "badges", "profile")

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
                                onAddClick = { navController.navigate("app_block") }
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = when {
                            userProfile.isLoggedIn -> "dashboard"
                            userProfile.isFirstLaunch -> "splash"
                            else -> "auth"
                        },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onGetStarted = { navController.navigate("onboarding") },
                                onSignInClick = { navController.navigate("auth") }
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
                                userPreferences = userPreferences
                            )
                        }
                        composable("onboarding") {
                            OnboardingAgentsScreen(onGetStarted = { 
                                scope.launch {
                                    userPreferences.setFirstLaunchCompleted()
                                    navController.navigate("auth")
                                }
                            })
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                userProfile = userProfile,
                                userPreferences = userPreferences,
                                onProfileClick = { navController.navigate("profile") },
                                onAddClick = { navController.navigate("app_block") },
                                onRewardsClick = { navController.navigate("rewards") }
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
                                }
                            )
                        }
                        composable("app_block") {
                            AppBlockScreen(
                                onBack = { navController.popBackStack() },
                                userPreferences = userPreferences
                            )
                        }
                        composable("rewards") {
                            RewardsShopScreen(
                                onBack = { navController.popBackStack() },
                                userProfile = userProfile,
                                userPreferences = userPreferences,
                                mongoRepository = mongoRepository
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
                                onNeedTime = { navController.navigate("rewards") }
                            )
                        }
                        composable("ai_agent") {
                            ComingSoonScreen("AI Agent")
                        }
                        composable("badges") {
                            ComingSoonScreen("Badges & Achievements")
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
        startForegroundService(serviceIntent)
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
            Icons.Default.AutoAwesome,
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
    onAddClick: () -> Unit
) {
    // Use Box to allow the FAB to overlap the NavigationBar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = com.undrift.ui.theme.SurfaceColor,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "dashboard" } == true,
                    onClick = { navigateToRoute(navController, "dashboard") },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "DASH") },
                    label = { Text("DASH") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "ai_agent" } == true,
                    onClick = { navigateToRoute(navController, "ai_agent") },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "AI AGENT") },
                    label = { Text("AI AGENT") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.Transparent
                    )
                )
                
                Spacer(Modifier.weight(0.4f))
                
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "badges" } == true,
                    onClick = { navigateToRoute(navController, "badges") },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "BADGES") },
                    label = { Text("BADGES") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
                    onClick = { navigateToRoute(navController, "profile") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "PROFILE") },
                    label = { Text("PROFILE") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
        
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .offset(y = (-30).dp)
                .size(60.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(30.dp))
        }
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
