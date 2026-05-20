package com.bimarihaunter.navigation

sealed class Screen(val route: String) {
    // Auth
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")

    // Main — Feed
    object HomeFeed : Screen("home_feed")
    object SearchFilter : Screen("search_filter")
    object ArticleDetail : Screen("article_detail/{articleId}") {
        fun createRoute(articleId: String) = "article_detail/$articleId"
    }
    object EmptyState : Screen("empty_state")

    // Map
    object DiseaseMap : Screen("disease_map")
    object MapDetail : Screen("map_detail/{locationId}") {
        fun createRoute(locationId: String) = "map_detail/$locationId"
    }

    // Insights & Alerts
    object Insights : Screen("insights")
    object Alerts : Screen("alerts")

    // Community
    object ChatList : Screen("chat_list")
    object GroupChat : Screen("group_chat/{groupId}") {
        fun createRoute(groupId: String) = "group_chat/$groupId"
    }
    object DirectChat : Screen("direct_chat/{chatId}/{friendId}/{friendName}") {
        fun createRoute(chatId: String, friendId: String, friendName: String) =
            "direct_chat/$chatId/$friendId/$friendName"
    }
    object CreateGroup : Screen("create_group")
    object AiChat : Screen("ai_chat")
    object AddFriends : Screen("add_friends")
    object UserDirectChat : Screen("user_direct_chat/{userId}/{userName}") {
        fun createRoute(userId: String, userName: String) =
            "user_direct_chat/$userId/${java.net.URLEncoder.encode(userName, "UTF-8")}"
    }

    // Profile
    object Profile : Screen("profile")
    object Settings : Screen("settings")

    // Edge Cases
    object Offline : Screen("offline")
}
