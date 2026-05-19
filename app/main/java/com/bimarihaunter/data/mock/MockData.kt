package com.bimarihaunter.data.mock

import com.bimarihaunter.data.models.InsightsData
import com.bimarihaunter.data.models.PharmacyItem
import com.bimarihaunter.data.models.RegionData

val PAKISTAN_REGIONS = listOf(
    RegionData("Lahore",     31.5204, 74.3587, 142, "HIGH", "Disease Outbreak", "10 mins ago"),
    RegionData("Karachi",    24.8607, 67.0011, 210, "HIGH", "Pharmacy/Cost", "5 mins ago"),
    RegionData("Islamabad",  33.6844, 73.0479, 45, "LOW", "General News", "1 hr ago"),
    RegionData("Peshawar",   34.0151, 71.5249, 89, "MEDIUM", "Natural Disaster", "30 mins ago"),
    RegionData("Quetta",     30.1798, 66.9750, 115, "MEDIUM", "Disease Outbreak", "2 hrs ago"),
    RegionData("Multan",     30.1575, 71.5249, 30, "LOW", "General News", "4 hrs ago"),
    RegionData("Faisalabad", 31.4504, 73.1350, 60, "LOW", "Disease Outbreak", "1 day ago"),
    RegionData("Rawalpindi", 33.5651, 73.0169, 75, "MEDIUM", "Pharmacy/Cost", "2 days ago"),
)

val MOCK_INSIGHTS = InsightsData(
    region = "Lahore",
    totalCases = 142,
    severity = "HIGH",
    trendPoints = listOf("Mon" to 12f, "Tue" to 28f, "Wed" to 45f, "Thu" to 38f, "Fri" to 62f, "Sat" to 55f, "Sun" to 71f),
    categoryBreakdown = mapOf("Disease" to 89, "Disaster" to 23, "Pharmacy" to 18, "General" to 12),
    highCount = 64f, mediumCount = 48f, lowCount = 30f,
    pharmacyItems = listOf(
        PharmacyItem("Paracetamol 500mg", 45, 12.5f),
        PharmacyItem("ORS Sachets", 30, -5.2f),
        PharmacyItem("Amoxicillin 250mg", 180, 8.1f),
        PharmacyItem("Chlorine Tablets", 25, 22.0f)
    )
)
