package com.bimarihaunter.data.api

import com.bimarihaunter.data.mock.MOCK_INSIGHT_REPORT
import com.bimarihaunter.data.models.InsightReport
import com.bimarihaunter.data.models.ReportData
import kotlinx.coroutines.delay

/**
 * Isolated Gemini Service for generating Insights.
 */
class InsightsService {

    // TODO: When Gemini SDK is configured, initialize GenerativeModel here
    // private val generativeModel = GenerativeModel(
    //     modelName = "gemini-pro",
    //     apiKey = BuildConfig.GEMINI_API_KEY // Ensure key is not exposed in UI
    // )

    /**
     * Generates structured insights from a list of ReportData.
     * Uses mock fallback if Gemini is not configured or fails.
     */
    suspend fun generateInsights(reports: List<ReportData>): InsightReport {
        return try {
            // Mock delay for AI generation
            delay(1500)
            
            // Return mock fallback as Gemini SDK is not yet available
            MOCK_INSIGHT_REPORT
            
            // TODO: Real Gemini Implementation
            // val summaryText = buildSummaryForPrompt(reports)
            // val prompt = "Analyze the following outbreak reports and provide structured JSON insights...\n$summaryText"
            // val response = generativeModel.generateContent(prompt)
            // val jsonString = response.text ?: ""
            // parseResponseSafely(jsonString)
        } catch (e: Exception) {
            // Return empty fallback on total failure
            InsightReport.empty
        }
    }

    private fun buildSummaryForPrompt(reports: List<ReportData>): String {
        return reports.joinToString("\n") { 
            "${it.city}: ${it.count} cases of ${it.disease} (${it.severity})"
        }
    }

    // private fun parseResponseSafely(json: String): InsightReport {
    //     // Implementation for safely parsing the JSON using Gson/Moshi and handling malformed cases.
    // }
}
