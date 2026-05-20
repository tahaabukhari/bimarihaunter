package com.bimarihaunter.data.model

import com.google.gson.annotations.SerializedName

data class Report(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("source") val source: String,
    @SerializedName("url") val url: String,
    @SerializedName("raw_text") val rawText: String,
    @SerializedName("published_at") val publishedAt: String,  // ISO-8601
    @SerializedName("scraped_at") val scrapedAt: String,      // ISO-8601
    @SerializedName("status") val status: String,
    @SerializedName("source_type") val sourceType: String,
    @SerializedName("platform") val platform: String?,
    @SerializedName("ai_analysis") val aiAnalysis: AiAnalysis?
)

data class AiAnalysis(
    @SerializedName("disease") val disease: String,
    @SerializedName("severity") val severity: String,  // "high", "medium", "low"
    @SerializedName("summary") val summary: List<String>,
    @SerializedName("symptoms") val symptoms: List<String>,
    @SerializedName("locations") val locations: List<String>,
    @SerializedName("coordinates") val coordinates: Coordinates?,
    @SerializedName("confidence_score") val confidenceScore: Double,
    @SerializedName("model_used") val modelUsed: String
)

data class Coordinates(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
