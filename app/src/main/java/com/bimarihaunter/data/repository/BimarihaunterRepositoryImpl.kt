package com.bimarihaunter.data.repository

import com.bimarihaunter.data.api.BimarihaunterApi
import com.bimarihaunter.data.mock.MOCK_INSIGHTS
import com.bimarihaunter.data.mock.PAKISTAN_REGIONS
import com.bimarihaunter.data.models.ArticleDto
import com.bimarihaunter.data.models.InsightsData
import com.bimarihaunter.data.models.PharmacyItem
import com.bimarihaunter.data.models.RegionData
import kotlinx.coroutines.delay
import javax.inject.Inject

class BimarihaunterRepositoryImpl @Inject constructor(
    private val api: BimarihaunterApi
) : BimarihaunterRepository {

    // Fallback coordinates for regions
    private val regionCoordinates = mapOf(
        "Lahore" to Pair(31.5204, 74.3587),
        "Karachi" to Pair(24.8607, 67.0011),
        "Islamabad" to Pair(33.6844, 73.0479),
        "Peshawar" to Pair(34.0151, 71.5249),
        "Quetta" to Pair(30.1798, 66.9750),
        "Multan" to Pair(30.1575, 71.5249),
        "Faisalabad" to Pair(31.4504, 73.1350),
        "Rawalpindi" to Pair(33.5651, 73.0169)
    )

    override suspend fun getRegions(): List<RegionData> {
        return try {
            val feed = api.getFeed()
            mapFeedToRegions(feed)
        } catch (e: Exception) {
            // Fallback to mock data
            delay(500)
            PAKISTAN_REGIONS
        }
    }

    override suspend fun searchRegions(query: String): List<RegionData> {
        return try {
            val results = api.search(query)
            mapFeedToRegions(results)
        } catch (e: Exception) {
            // Fallback to mock search
            delay(300)
            if (query.isBlank()) return PAKISTAN_REGIONS
            val lowerQuery = query.lowercase()
            PAKISTAN_REGIONS.filter { 
                it.name.lowercase().contains(lowerQuery) || 
                it.severity.lowercase().contains(lowerQuery) ||
                it.topCategory.lowercase().contains(lowerQuery)
            }
        }
    }

    override suspend fun getInsights(region: String): InsightsData {
        return try {
            val dto = api.getRegionInsights(region)
            mapInsightsDtoToData(region, dto)
        } catch (e: Exception) {
            try {
                // If /insights endpoint fails, try to aggregate from /feed
                val feed = api.getFeed(region = region)
                aggregateFeedToInsights(region, feed)
            } catch (innerE: Exception) {
                // Total failure, fallback to mock
                delay(300)
                MOCK_INSIGHTS.copy(region = region)
            }
        }
    }

    override suspend fun filterInsights(region: String, category: String): InsightsData {
        val data = getInsights(region)
        if (category == "All") return data
        
        // Filter the aggregated InsightsData locally since the API endpoint might just give full insights
        val filteredTotal = data.categoryBreakdown[category] ?: 0
        return data.copy(
            totalCases = filteredTotal,
            topCategory = category
        )
    }

    // --- Safe Mapping Functions ---

    private fun mapFeedToRegions(feed: List<ArticleDto>): List<RegionData> {
        val regionGroups = feed.filter { !it.region.isNullOrBlank() }.groupBy { it.region!! }
        return regionGroups.map { (regionName, articles) ->
            val totalCases = articles.sumOf { it.caseCount ?: 0 }
            val severity = articles.firstOrNull()?.severity ?: calculateSeverity(totalCases)
            val topCategory = articles.groupingBy { it.category ?: "Unknown" }.eachCount().maxByOrNull { it.value }?.key ?: "Unknown"
            
            val coords = regionCoordinates[regionName] ?: Pair(
                articles.firstOrNull { it.lat != null }?.lat ?: 30.0,
                articles.firstOrNull { it.lng != null }?.lng ?: 70.0
            )

            RegionData(
                name = regionName,
                lat = coords.first,
                lng = coords.second,
                caseCount = totalCases,
                severity = severity,
                topCategory = topCategory,
                lastUpdated = articles.maxOfOrNull { it.publishedAt ?: "" } ?: "Just now"
            )
        }
    }

    private fun mapInsightsDtoToData(region: String, dto: com.bimarihaunter.data.models.InsightsDto): InsightsData {
        return InsightsData(
            region = dto.region ?: region,
            totalCases = dto.totalCases ?: 0,
            severity = dto.severity ?: calculateSeverity(dto.totalCases ?: 0),
            topCategory = dto.categoryBreakdown?.maxByOrNull { it.value }?.key ?: "Unknown",
            trendPoints = dto.trendPoints?.map { Pair(it.date, it.value) } ?: emptyList(),
            categoryBreakdown = dto.categoryBreakdown ?: emptyMap(),
            highCount = dto.highCount ?: 0f,
            mediumCount = dto.mediumCount ?: 0f,
            lowCount = dto.lowCount ?: 0f,
            pharmacyItems = dto.pharmacyItems?.map { 
                val percent = if (it.previousPrice != null && it.previousPrice > 0) {
                    ((it.currentPrice - it.previousPrice) / it.previousPrice.toFloat()) * 100
                } else 0f
                PharmacyItem(it.name, it.currentPrice, String.format("%.1f", percent).toFloat()) 
            } ?: emptyList()
        )
    }

    private fun aggregateFeedToInsights(region: String, feed: List<ArticleDto>): InsightsData {
        val totalCases = feed.sumOf { it.caseCount ?: 0 }
        val breakdown = feed.groupingBy { it.category ?: "Unknown" }.fold(0) { acc, dto -> acc + (dto.caseCount ?: 0) }
        
        val high = feed.count { (it.severity ?: calculateSeverity(it.caseCount ?: 0)) == "HIGH" }.toFloat()
        val medium = feed.count { (it.severity ?: calculateSeverity(it.caseCount ?: 0)) == "MEDIUM" }.toFloat()
        val low = feed.count { (it.severity ?: calculateSeverity(it.caseCount ?: 0)) == "LOW" }.toFloat()

        val pharmacyList = feed.filter { !it.medicineName.isNullOrBlank() }.map {
            val percent = if (it.previousPrice != null && it.previousPrice > 0) {
                ((it.currentPrice!! - it.previousPrice) / it.previousPrice.toFloat()) * 100
            } else 0f
            PharmacyItem(it.medicineName!!, it.currentPrice ?: 0, String.format("%.1f", percent).toFloat())
        }

        return InsightsData(
            region = region,
            totalCases = totalCases,
            severity = calculateSeverity(totalCases),
            topCategory = breakdown.maxByOrNull { it.value }?.key ?: "Unknown",
            trendPoints = MOCK_INSIGHTS.trendPoints, // Hard to derive trend without historical API, use fallback
            categoryBreakdown = breakdown,
            highCount = high,
            mediumCount = medium,
            lowCount = low,
            pharmacyItems = pharmacyList.ifEmpty { MOCK_INSIGHTS.pharmacyItems }
        )
    }

    private fun calculateSeverity(caseCount: Int): String {
        return when {
            caseCount >= 70 -> "HIGH"
            caseCount >= 30 -> "MEDIUM"
            else -> "LOW"
        }
    }
}
