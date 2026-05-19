package com.bimarihaunter.data.models

data class ArticleDto(
    val id: String,
    val title: String,
    val summary: String?,
    val region: String?,
    val category: String?,
    val severity: String?,
    val lat: Double?,
    val lng: Double?,
    val caseCount: Int?,
    val publishedAt: String?,
    val source: String?,
    val medicineName: String?,
    val currentPrice: Int?,
    val previousPrice: Int?
)
