package com.bimarihaunter.data.mock

import com.bimarihaunter.data.models.InsightReport
import com.bimarihaunter.data.models.OutbreakLocation
import com.bimarihaunter.data.models.SummaryStats

val MOCK_OUTBREAK_LOCATIONS = listOf(
    OutbreakLocation("1", "Karachi", 24.8607, 67.0011, "Dengue", 150, "high", "2023-10-25T10:00:00Z"),
    OutbreakLocation("2", "Lahore", 31.5204, 74.3587, "Cholera", 85, "medium", "2023-10-25T11:00:00Z"),
    OutbreakLocation("3", "Islamabad", 33.6844, 73.0479, "Malaria", 45, "low", "2023-10-25T12:00:00Z"),
    OutbreakLocation("4", "Peshawar", 34.0151, 71.5249, "Dengue", 95, "high", "2023-10-25T09:00:00Z"),
    OutbreakLocation("5", "Quetta", 30.1798, 66.9750, "Other", 30, "low", "2023-10-24T14:00:00Z"),
    OutbreakLocation("6", "Multan", 30.1575, 71.5249, "Cholera", 60, "medium", "2023-10-25T08:00:00Z"),
    OutbreakLocation("7", "Faisalabad", 31.4504, 73.1350, "Malaria", 110, "high", "2023-10-25T07:00:00Z"),
    OutbreakLocation("8", "Rawalpindi", 33.5651, 73.0169, "Dengue", 75, "medium", "2023-10-24T18:00:00Z")
)

val MOCK_SUMMARY_STATS = SummaryStats(
    totalCases = MOCK_OUTBREAK_LOCATIONS.sumOf { it.count },
    criticalAreas = MOCK_OUTBREAK_LOCATIONS.count { it.severity == "high" },
    weeklyIncrease = 15, // Mock value
    topDisease = MOCK_OUTBREAK_LOCATIONS.groupBy { it.disease }
        .maxByOrNull { it.value.sumOf { loc -> loc.count } }?.key ?: "Unknown"
)

val MOCK_INSIGHT_REPORT = InsightReport(
    totalCases = MOCK_SUMMARY_STATS.totalCases,
    criticalRegions = MOCK_SUMMARY_STATS.criticalAreas,
    diseaseBreakdown = MOCK_OUTBREAK_LOCATIONS.groupBy { it.disease }.mapValues { it.value.sumOf { loc -> loc.count } },
    weeklyGrowth = "+15% this week",
    hotspots = MOCK_OUTBREAK_LOCATIONS.filter { it.severity == "high" }.map { it.city },
    trends = "Dengue cases are rising rapidly in urban centers like Karachi and Faisalabad. Immediate vector control is recommended.",
    riskLevel = "High",
    recommendations = listOf(
        "Initiate fogging in Karachi and Faisalabad.",
        "Distribute mosquito nets in high-risk zones.",
        "Issue public health advisory for water storage."
    ),
    prediction7Days = "Expected 20% increase in Dengue cases if no immediate action is taken. Cholera expected to stabilize."
)
