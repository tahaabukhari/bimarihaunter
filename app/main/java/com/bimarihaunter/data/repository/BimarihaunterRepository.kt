package com.bimarihaunter.data.repository

import com.bimarihaunter.data.models.InsightsData
import com.bimarihaunter.data.models.RegionData

interface BimarihaunterRepository {
    suspend fun getRegions(): List<RegionData>
    suspend fun getInsights(region: String): InsightsData
    suspend fun searchRegions(query: String): List<RegionData>
    suspend fun filterInsights(region: String, category: String): InsightsData
}
