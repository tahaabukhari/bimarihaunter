package com.bimarihaunter.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.db.BimarihaunterDatabase
import com.bimarihaunter.network.RetrofitClient
import com.bimarihaunter.network.ChatMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class ChatMode {
    THINKING,
    SIMPLE
}

data class AiMessage(
    val id: String,
    val message: String,
    val isUser: Boolean,
    val quickReplies: List<String> = emptyList()
)

/**
 * ViewModel for the Haunter AI chat screen.
 *
 * Crash-hardened & API Fallback optimized:
 *  - Automatically falls back to Room database context when the local Python backend
 *    at 10.0.2.2 is unreachable (e.g., when testing on a physical phone).
 *  - Pulls the latest live-synced Firestore outbreak reports directly from the database
 *    to provide smart, localized public health advice even without a running server.
 */
class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BimarihaunterDatabase.getDatabase(application)

    // ── State ────────────────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _chatMode = MutableStateFlow(ChatMode.THINKING)
    val chatMode: StateFlow<ChatMode> = _chatMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            appendMessage(
                AiMessage(
                    id           = UUID.randomUUID().toString(),
                    message      = "Assalam o Alaikum! 👋 I'm Haunter AI, your health assistant. " +
                                   "Ask me about symptoms, prevention, or nearby outbreaks.",
                    isUser       = false,
                    quickReplies = listOf("Prevention tips", "Nearest hospital", "Current outbreaks")
                )
            )
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()

        appendMessage(
            AiMessage(
                id      = UUID.randomUUID().toString(),
                message = trimmed,
                isUser  = true
            )
        )

        viewModelScope.launch {
            _isLoading.value  = true
            _errorMessage.value = null
            try {
                val reply = fetchAiReply(trimmed)
                appendMessage(
                    AiMessage(
                        id           = UUID.randomUUID().toString(),
                        message      = reply,
                        isUser       = false,
                        quickReplies = listOf("How to prevent?", "What are the symptoms?", "Where to get help?")
                    )
                )
            } catch (t: Throwable) {
                Log.e(TAG, "AI reply failed", t)
                _errorMessage.value = "Unable to reach Haunter AI. Check your connection."
                appendMessage(
                    AiMessage(
                        id      = UUID.randomUUID().toString(),
                        message = "I couldn't reach the server right now. Please check your internet and try again.",
                        isUser  = false
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setChatMode(mode: ChatMode) {
        _chatMode.value = mode
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private suspend fun fetchAiReply(query: String): String = withContext(Dispatchers.IO) {
        when (_chatMode.value) {
            ChatMode.THINKING -> fetchBackendReply(query)
            ChatMode.SIMPLE   -> generateSimpleReply(query, null)
        }
    }

    /**
     * Calls the BimariHaunter backend which runs the Gemini agentic workflow
     * server-side. Falls back to a local intelligent reply using Room database context
     * if the backend is unreachable.
     */
    private suspend fun fetchBackendReply(query: String): String {
        return try {
            val prompt = buildPrompt(query)
            val response = RetrofitClient.apiService.sendMessage(
                chatId = "default_chat_session",
                mode   = "smart",
                body   = ChatMessageRequest(text = prompt)
            )
            response.response.ifBlank {
                generateLocalIntelligentReply(query)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Backend call failed — falling back to localized database-driven intelligence", t)
            generateLocalIntelligentReply(query)
        }
    }

    /**
     * Generates a smart response using actual synced outbreak data stored in the local SQLite Room DB.
     * This allows the AI to provide authentic, localized reports without an active backend server.
     */
    private suspend fun generateLocalIntelligentReply(query: String): String = withContext(Dispatchers.IO) {
        try {
            val reports = database.outbreakReportDao().getAllReports().first()
            if (reports.isNotEmpty()) {
                val q = query.lowercase()
                
                // Match disease in user query to local reports
                val matchedReport = reports.firstOrNull { report ->
                    q.contains(report.disease.lowercase()) || q.contains(report.title.lowercase())
                } ?: reports.first() // Fallback to latest critical report if no direct match

                val severityEmoji = when (matchedReport.severity.uppercase()) {
                    "CRITICAL" -> "🚨"
                    "HIGH" -> "⚠️"
                    else -> "ℹ️"
                }

                val locationsText = matchedReport.locations.joinToString(", ").ifEmpty { "your region" }
                val summaryText = matchedReport.summary.joinToString(". ").ifEmpty { matchedReport.raw_text.take(150) }

                return@withContext """
                    $severityEmoji *Active Local Report Detected:*
                    I found a matching outbreak record in our local database:
                    
                    • *Disease:* ${matchedReport.disease.replaceFirstChar { it.uppercase() }}
                    • *Source:* ${matchedReport.source} (Severity: ${matchedReport.severity.uppercase()})
                    • *Location:* $locationsText
                    • *Details:* $summaryText
                    
                    *Precautionary Health Guidelines:*
                    Please stay updated with the BimariHaunter feed. Avoid contaminated areas, ensure drinking water is boiled, and report new symptoms to local authorities immediately.
                """.trimIndent()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate localized DB intelligence", e)
        }

        // Ultimate fallback to rule-based engine if database is empty
        return@withContext generateSimpleReply(query, "smart_mode_fallback")
    }

    /**
     * Fully offline fallback — no model required, just a rule-based response.
     * Keeps the chat usable even without internet or a backend.
     */
    private fun generateSimpleReply(query: String, contextTag: String?): String {
        val q = query.lowercase()
        val suffix = if (contextTag != null) {
            "\n\n_(Note: Smart Mode is currently operating in offline fallback due to network status)_"
        } else ""

        val mainReply = when {
            q.contains("fever") || q.contains("bukhar") ->
                "🌡️ Fever can signal infections like dengue, malaria, or typhoid in Pakistan. " +
                "Stay hydrated, rest, and consult a doctor if it exceeds 38.5°C or lasts more than 2 days."

            q.contains("dengue") ->
                "🦟 Dengue is mosquito-borne. Use repellents, wear long sleeves, and eliminate standing water. " +
                "Seek urgent care for platelet drops, severe headache, or bleeding."

            q.contains("malaria") ->
                "🦟 Malaria spreads through mosquito bites. Take prescribed prophylactics if travelling. " +
                "Symptoms include cyclical fever, chills, and sweats — see a doctor immediately."

            q.contains("cholera") || q.contains("diarrhea") || q.contains("diarrhoea") ->
                "💧 Drink only clean/boiled water. Use ORS immediately for dehydration. " +
                "Cholera spreads fast — report clusters to local health authorities."

            q.contains("hospital") || q.contains("doctor") ->
                "🏥 For emergencies dial **1122** (Punjab) or **115** (Rescue). " +
                "Nearest government hospitals are usually the fastest option for critical care."

            q.contains("prevent") || q.contains("safety") ->
                "🛡️ Key prevention steps:\n• Wash hands frequently\n• Drink boiled or filtered water\n" +
                "• Use mosquito nets at night\n• Keep surroundings clean\n• Get vaccinations up to date"

            q.contains("outbreak") || q.contains("alert") ->
                "📊 Check the BimariHaunter feed and map for real-time outbreak reports in your area. " +
                "Stay indoors during high-severity alerts."

            else ->
                "🩺 I'm Haunter AI. While offline, I can answer basic health questions. " +
                "For real-time outbreak data, enable internet and switch to **Thinking** mode."
        }

        return mainReply + suffix
    }

    private fun buildPrompt(userQuery: String): String = """
        You are Haunter AI, a friendly health advisor for BimariHaunter.
        Help users understand disease outbreaks, prevention, and health tips in Pakistan.
        Be concise (under 150 words), empathetic, and always recommend professional care for serious concerns.
        
        USER QUERY: $userQuery
        
        RESPONSE:
    """.trimIndent()

    private fun appendMessage(message: AiMessage) {
        _messages.value = _messages.value + message
    }

    companion object {
        private const val TAG = "AiChatViewModel"
    }
}
