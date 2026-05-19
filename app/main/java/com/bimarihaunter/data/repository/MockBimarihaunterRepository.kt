package com.bimarihaunter.data.repository

import com.bimarihaunter.data.mock.MOCK_INSIGHTS
import com.bimarihaunter.data.mock.PAKISTAN_REGIONS
import com.bimarihaunter.data.models.InsightsData
import com.bimarihaunter.data.models.RegionData
import kotlinx.coroutines.delay

class MockBimarihaunterRepository : BimarihaunterRepository {

    // TODO: This mock implementation can be replaced with a real BackendRepository later
    
    override suspend fun getRegions(): List<RegionData> {
        delay(800) // Simulate network delay
        return PAKISTAN_REGIONS
    }

    override suspend fun getInsights(region: String): InsightsData {
        delay(600)
        // Simulate returning mock data for the selected region
        return MOCK_INSIGHTS.copy(region = region)
    }

    override suspend fun searchRegions(query: String): List<RegionData> {
        delay(300)
        if (query.isBlank()) return PAKISTAN_REGIONS
        val lowerQuery = query.lowercase()
        return PAKISTAN_REGIONS.filter { 
            it.name.lowercase().contains(lowerQuery) || 
            it.severity.lowercase().contains(lowerQuery) ||
            it.topCategory.lowercase().contains(lowerQuery)
        }
    }

    override suspend fun filterInsights(region: String, category: String): InsightsData {
        delay(300)
        val data = getInsights(region)
        if (category == "All") return data
        
        // Mock filtering behavior
        return data.copy(
            totalCases = data.categoryBreakdown[category] ?: 0,
            topCategory = category
        )
    }
}
