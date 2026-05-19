package com.bimarihaunter.data.models

data class InsightsData(
    val region: String,
    val totalCases: Int,
    val severity: String,
    val trendPoints: List<Pair<String, Float>>,   // date -> count
    val categoryBreakdown: Map<String, Int>,       // category -> count
    val highCount: Float,
    val mediumCount: Float,
    val lowCount: Float,
    val pharmacyItems: List<PharmacyItem>
)
