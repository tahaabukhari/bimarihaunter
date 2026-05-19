package com.bimarihaunter.data.models

data class RegionData(
    val name: String,
    val lat: Double,
    val lng: Double,
    val caseCount: Int = 0,
    val severity: String = "LOW",   // "HIGH" | "MEDIUM" | "LOW"
    val topCategory: String = "",
    val lastUpdated: String = ""
)
