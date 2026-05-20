package com.bimarihaunter.data.models

data class OutbreakLocation(
    val id: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val disease: String,
    val count: Int,
    val severity: String,
    val timestamp: String
)

data class SummaryStats(
    val totalCases: Int,
    val criticalAreas: Int,
    val weeklyIncrease: Int,
    val topDisease: String
)

data class InsightReport(
    val totalCases: Int,
    val criticalRegions: Int,
    val diseaseBreakdown: Map<String, Int>,
    val weeklyGrowth: String,
    val hotspots: List<String>,
    val trends: String,
    val riskLevel: String,
    val recommendations: List<String>,
    val prediction7Days: String
) {
    companion object {
        val empty = InsightReport(
            totalCases = 0,
            criticalRegions = 0,
            diseaseBreakdown = emptyMap(),
            weeklyGrowth = "",
            hotspots = emptyList(),
            trends = "",
            riskLevel = "",
            recommendations = emptyList(),
            prediction7Days = ""
        )
    }
}

data class ReportData(
    val id: String,
    val disease: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String,
    val count: Int,
    val timestamp: String
)
