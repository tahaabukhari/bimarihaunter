package com.bimarihaunter.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.bimarihaunter.db.BimarihaunterDatabase
import com.bimarihaunter.db.OutbreakReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

class SLMManager(private val context: Context, private val database: BimarihaunterDatabase) {
    private var llmInference: LlmInference? = null

    suspend fun initialize() = withContext(Dispatchers.Default) {
        try {
            // Initialize MediaPipe LLM Inference
            // Place a compatible model in assets/models/
            llmInference = LlmInference.createFromOptions(
                context,
                LlmInference.LlmInferenceOptions.builder()
                    .setModelPath("file:///android_asset/models/llama-3.2-1b-instruct.task")
                    .build()
            )
            Timber.d("SLM initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize SLM")
        }
    }

    suspend fun generateOfflineResponse(userQuery: String): String = withContext(Dispatchers.Default) {
        return@withContext try {
            val reportsList = database.outbreakReportDao().getAllReports().first()
            val contextText = buildContextFromReports(reportsList)

            val systemPrompt = """
                You are Bimarihaunter Local SLM, a lightweight offline public health advisor.
                Use ONLY the outbreak context provided below. If the answer is not available, say:
                "I don't have current data on that. Please check with local health authorities."
            """.trimIndent()

            val fullPrompt = """
                $systemPrompt

                CURRENT OUTBREAK CONTEXT:
                $contextText

                USER QUERY: $userQuery

                RESPONSE:
            """.trimIndent()

            val response = llmInference?.generateResponse(fullPrompt) ?: "Unable to generate response"
            response.trim().replace(Regex("\\s+"), " ")
        } catch (e: Exception) {
            Timber.e(e, "SLM generation failed")
            "I encountered an error while generating the response. Please try again."
        }
    }

    suspend fun getContextSummary(): String = withContext(Dispatchers.Default) {
        return@withContext try {
            val reportsList = database.outbreakReportDao().getAllReports().first()
            val summaryText = buildContextFromReports(reportsList)
            if (summaryText.isBlank()) {
                "No local outbreak feed context is available right now."
            } else {
                summaryText
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to build context summary")
            "No local outbreak feed context is available right now."
        }
    }

    private fun buildContextFromReports(reports: List<OutbreakReportEntity>): String {
        return reports
            .sortedWith(compareByDescending<OutbreakReportEntity> { it.published_at }.thenBy { it.severity })
            .take(10)
            .joinToString("\n\n") { report ->
                val summaryText = report.summary.joinToString("; ").ifEmpty { report.raw_text }
                val locationsText = report.locations.joinToString(", ").ifEmpty { "Unknown location" }
                "• ${report.source} — ${report.disease.replaceFirstChar { it.uppercase() }} (Severity: ${report.severity.replaceFirstChar { it.uppercase() }})\n  Title: ${report.title}\n  Locations: $locationsText\n  Summary: $summaryText"
            }
    }

    fun release() {
        llmInference?.close()
    }
}
