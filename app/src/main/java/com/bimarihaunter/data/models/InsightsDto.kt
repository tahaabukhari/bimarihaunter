package com.bimarihaunter.data.models

data class InsightsDto(
    val region: String?,
    val totalCases: Int?,
    val severity: String?,
    val trendPoints: List<TrendPointDto>?,
    val categoryBreakdown: Map<String, Int>?,
    val highCount: Float?,
    val mediumCount: Float?,
    val lowCount: Float?,
    val pharmacyItems: List<PharmacyItemDto>?
)

data class TrendPointDto(
    val date: String,
    val value: Float
)

data class PharmacyItemDto(
    val name: String,
    val currentPrice: Int,
    val previousPrice: Int?
)
