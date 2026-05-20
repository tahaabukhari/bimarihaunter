package com.bimarihaunter.network

import retrofit2.http.*

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
