package com.bimarihaunter.network

import retrofit2.http.*
import com.bimarihaunter.data.model.User

interface ApiService {
    // Update user location
    @POST("/api/v1/users/location")
    suspend fun updateLocation(@Body body: LocationUpdateRequest): LocationUpdateResponse
    
    // Get personalized feed
    @GET("/api/v1/feed")
    suspend fun getFeed(@Query("limit") limit: Int = 50): List<OutbreakReport>

    // Trigger backend refresh job
    @POST("/api/v1/jobs/trigger")
    suspend fun triggerJob(): JobTriggerResponse
    
    // Send chat message
    @POST("/api/v1/chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Query("mode") mode: String, // "local" or "smart"
        @Body body: ChatMessageRequest
    ): ChatMessageResponse

    // Search users
    @GET("/api/v1/users/search")
    suspend fun searchUsers(@Query("query") query: String): UserSearchResponse
    
    // Add friend
    @POST("/api/v1/users/friends/{friendId}")
    suspend fun addFriend(@Path("friendId") friendId: String): SimpleResponse
    
    // Remove friend
    @DELETE("/api/v1/users/friends/{friendId}")
    suspend fun removeFriend(@Path("friendId") friendId: String): SimpleResponse
    
    // List friends
    @GET("/api/v1/users/friends")
    suspend fun getFriends(): FriendsListResponse
    
    // Block user
    @POST("/api/v1/users/block/{blockedId}")
    suspend fun blockUser(@Path("blockedId") blockedId: String): SimpleResponse
    
    // Unblock user
    @DELETE("/api/v1/users/block/{blockedId}")
    suspend fun unblockUser(@Path("blockedId") blockedId: String): SimpleResponse
    
    // List blocked users
    @GET("/api/v1/users/blocked")
    suspend fun getBlockedUsers(): BlockedListResponse
}

data class JobTriggerResponse(
    val status: String,
    val message: String
)

data class LocationUpdateRequest(
    val city: String,
    val latitude: Double,
    val longitude: Double
)

data class LocationUpdateResponse(
    val status: String,
    val message: String,
    val city: String,
    val feed_count: Int
)

data class OutbreakReport(
    val id: String,
    val title: String,
    val source: String,
    val url: String,
    val raw_text: String,
    val published_at: String,
    val scraped_at: String,
    val status: String,
    val source_type: String,
    val ai_analysis: AiAnalysis?
)

data class AiAnalysis(
    val disease: String? = null,
    val severity: String? = null, // "high", "medium", "low"
    val summary: List<String>? = null,
    val symptoms: List<String>? = null,
    val locations: List<String>? = null,
    val coordinates: Coordinates? = null,
    val confidence_score: Double? = null,
    val model_used: String? = null
)

data class Coordinates(
    val latitude: Double?,
    val longitude: Double?
)

data class ChatMessageRequest(
    val text: String,
    val local_slm_response: String? = null
)

data class ChatMessageResponse(
    val status: String,
    val mode: String,
    val user_message_id: String,
    val ai_message_id: String,
    val response: String
)

data class UserSearchResponse(
    val users: List<User>
)

data class FriendsListResponse(
    val friends: List<FriendInfo>
)

data class FriendInfo(
    val uid: String,
    val name: String,
    val email: String,
    val added_at: String? = null
)

data class BlockedListResponse(
    val blocked: List<String>
)

data class SimpleResponse(
    val status: String,
    val message: String
)
