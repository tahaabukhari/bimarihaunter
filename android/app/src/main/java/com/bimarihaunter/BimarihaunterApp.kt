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
                    
                    LaunchedEffect(Unit) {
                        feedViewModel.syncFeed("Karachi", 24.8607, 67.0011)
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
                        onNavigateToAiChat = {
                            navController.navigate(Screen.AiChat.route)
                        },
                        onNavigateToCreateGroup = {
                            navController.navigate(Screen.CreateGroup.route)
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
