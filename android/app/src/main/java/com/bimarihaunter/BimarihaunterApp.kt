package com.bimarihaunter

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bimarihaunter.navigation.Screen
import com.bimarihaunter.ui.components.BottomNavBar
import com.bimarihaunter.ui.components.bottomNavItems
import com.bimarihaunter.ui.screens.auth.*
import com.bimarihaunter.ui.screens.chat.*
import com.bimarihaunter.ui.screens.feed.*
import com.bimarihaunter.ui.screens.insights.*
import com.bimarihaunter.ui.screens.map.*
import com.bimarihaunter.ui.screens.profile.*
import com.bimarihaunter.ui.theme.BimarihaunterTheme
import com.bimarihaunter.ui.feed.FeedScreen

@Composable
fun BimarihaunterApp() {
    val navController = rememberNavController()

    BimarihaunterTheme {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Bottom nav only on main 5 screens
        val mainRoutes = bottomNavItems.map { it.route }
        val showBottomBar = currentRoute in mainRoutes

        Scaffold(
            containerColor = com.bimarihaunter.ui.theme.MidnightBlack,
            // Let Scaffold handle all insets (status bar + nav bar)
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onItemSelected = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.HomeFeed.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(animationSpec = tween(300)) { it / 4 }
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutHorizontally(animationSpec = tween(300)) { -it / 4 }
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(animationSpec = tween(300)) { -it / 4 }
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutHorizontally(animationSpec = tween(300)) { it / 4 }
                }
            ) {
                // ===== AUTH FLOW =====
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onNavigateToOnboarding = {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Screen.HomeFeed.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onGetStarted = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Login.route) {
                    LoginScreen(
                        onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                        onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                        onNavigateToHome = {
                            navController.navigate(Screen.HomeFeed.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.SignUp.route) {
                    SignUpScreen(
                        onNavigateToLogin = { navController.popBackStack() },
                        onNavigateBack = { navController.popBackStack() },
                        onCreateAccount = {
                            navController.navigate(Screen.HomeFeed.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSendResetLink = { navController.popBackStack() }
                    )
                }

                // ===== MAIN FLOW =====
                composable(Screen.HomeFeed.route) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val database = com.bimarihaunter.db.BimarihaunterDatabase.getDatabase(context)
                    val repository = com.bimarihaunter.repository.FeedRepository(database)
                    val factory = com.bimarihaunter.ui.viewmodel.FeedViewModelFactory(repository)
                    val feedViewModel: com.bimarihaunter.ui.viewmodel.FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                    
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        
                        if (fineGranted || coarseGranted) {
                            try {
                                val locationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                locationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null) {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                        var cityName = "Karachi"
                                        try {
                                            @Suppress("DEPRECATION")
                                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                            if (!addresses.isNullOrEmpty()) {
                                                cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Karachi"
                                            }
                                        } catch (e: Exception) {
                                            // Use default city
                                        }
                                        feedViewModel.syncFeed(cityName, location.latitude, location.longitude)
                                    } else {
                                        feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
                                    }
                                }.addOnFailureListener {
                                    feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
                                }
                            } catch (e: SecurityException) {
                                feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
                            }
                        } else {
                            feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
                        }
                    }
                    
                    LaunchedEffect(Unit) {
                        val finePermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        val coarsePermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (finePermission || coarsePermission) {
                            try {
                                val locationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                locationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null) {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                        var cityName = "Karachi"
                                        try {
                                            @Suppress("DEPRECATION")
                                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                            if (!addresses.isNullOrEmpty()) {
                                                cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Karachi"
                                            }
                                        } catch (e: Exception) {
                                            // Use default city
                                        }
                                        feedViewModel.syncFeed(cityName, location.latitude, location.longitude)
                                    } else {
                                        feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
                                    }
                                }.addOnFailureListener {
                                    feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
                                }
                            } catch (e: SecurityException) {
                                feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                    
                    FeedScreen(viewModel = feedViewModel)
                }

                composable(Screen.SearchFilter.route) {
                    SearchFilterScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onApplyFilters = { navController.popBackStack() }
                    )
                }

                composable(
                    Screen.ArticleDetail.route,
                    arguments = listOf(navArgument("articleId") { type = NavType.StringType })
                ) { backStackEntry ->
                    ArticleDetailScreen(
                        articleId = backStackEntry.arguments?.getString("articleId"),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.EmptyState.route) {
                    EmptyStateScreen(
                        onClearFilters = { navController.popBackStack() },
                        onBrowseAll = {
                            navController.navigate(Screen.HomeFeed.route) {
                                popUpTo(Screen.HomeFeed.route) { inclusive = true }
                            }
                        }
                    )
                }

                // ===== MAP =====
                composable(Screen.DiseaseMap.route) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val database = com.bimarihaunter.db.BimarihaunterDatabase.getDatabase(context)
                    val repository = com.bimarihaunter.repository.FeedRepository(database)
                    val factory = com.bimarihaunter.ui.viewmodel.MapViewModelFactory(repository)
                    val mapViewModel: com.bimarihaunter.ui.viewmodel.MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                    com.bimarihaunter.ui.screens.MapScreen(viewModel = mapViewModel)
                }

                composable(
                    Screen.MapDetail.route,
                    arguments = listOf(navArgument("locationId") { type = NavType.StringType })
                ) { backStackEntry ->
                    MapDetailScreen(
                        locationId = backStackEntry.arguments?.getString("locationId"),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ===== INSIGHTS =====
                composable(Screen.Insights.route) {
                    InsightsScreen()
                }

                composable(Screen.Alerts.route) {
                    AlertsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ===== CHAT =====
                composable(Screen.ChatList.route) {
                    ChatListScreen(
                        onNavigateToGroupChat = { id ->
                            navController.navigate(Screen.GroupChat.createRoute(id))
                        },
                        onNavigateToDirectChat = { chatId, friendId, friendName ->
                            navController.navigate(Screen.DirectChat.createRoute(chatId, friendId, friendName))
                        },
                        onNavigateToAiChat = {
                            navController.navigate(Screen.AiChat.route)
                        },
                        onNavigateToCreateGroup = {
                            navController.navigate(Screen.CreateGroup.route)
                        },
                        onNavigateToAddFriends = {
                            navController.navigate(Screen.AddFriends.route)
                        }
                    )
                }

                composable(
                    Screen.GroupChat.route,
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                ) { backStackEntry ->
                    GroupChatScreen(
                        groupId = backStackEntry.arguments?.getString("groupId"),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    Screen.DirectChat.route,
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("friendId") { type = NavType.StringType },
                        navArgument("friendName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    DirectChatScreen(
                        chatId = backStackEntry.arguments?.getString("chatId"),
                        friendId = backStackEntry.arguments?.getString("friendId"),
                        friendName = backStackEntry.arguments?.getString("friendName"),
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToArticleDetail = { articleId ->
                            navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                        }
                    )
                }

                composable(Screen.CreateGroup.route) {
                    CreateGroupScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onCreate = { navController.popBackStack() }
                    )
                }

                composable(Screen.AiChat.route) {
                    AiChatScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AddFriends.route) {
                    AddFriendsScreen(
                        onNavigateBack   = { navController.popBackStack() },
                        onNavigateToChat = { userId, userName ->
                            navController.navigate(
                                Screen.UserDirectChat.createRoute(userId, userName)
                            )
                        }
                    )
                }

                composable(
                    Screen.UserDirectChat.route,
                    arguments = listOf(
                        navArgument("userId")   { type = NavType.StringType },
                        navArgument("userName") { type = NavType.StringType }
                    )
                ) { back ->
                    val userId   = back.arguments?.getString("userId") ?: ""
                    val userName = java.net.URLDecoder.decode(
                        back.arguments?.getString("userName") ?: "User", "UTF-8"
                    )
                    UserDirectChatScreen(
                        otherUserId   = userId,
                        otherUserName = userName,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ===== PROFILE =====
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Offline.route) {
                    OfflineScreen(
                        onRetry = { navController.popBackStack() },
                        onViewSaved = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
