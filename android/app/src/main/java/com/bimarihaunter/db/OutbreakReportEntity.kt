package com.bimarihaunter.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbreak_reports")
data class OutbreakReportEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val source: String,
    val url: String,
    val raw_text: String,
    val published_at: String,
    val scraped_at: String,
    val disease: String,
    val severity: String,
    val summary: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val latitude: Double,
    val longitude: Double,
    val confidence_score: Double,
    val cached_at: Long = System.currentTimeMillis()
)
