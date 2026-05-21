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

enum class ChatMode { THINKING, SIMPLE }

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val isUser: Boolean,
    val quickReplies: List<String> = emptyList(),
    val isTyping: Boolean = false
)

class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BimarihaunterDatabase.getDatabase(application)

    // Initialised with welcome message so list is NEVER empty on first render — prevents crash
    private val _messages = MutableStateFlow<List<AiMessage>>(
        listOf(
            AiMessage(
                id           = "welcome",
                message      = "Assalam o Alaikum! 👋 I'm Haunter AI, your health assistant.\n\nAsk me about symptoms, disease prevention, or nearby outbreaks in Pakistan.",
                isUser       = false,
                quickReplies = listOf("Prevention tips", "Nearest hospital", "Current outbreaks", "Dengue symptoms")
            )
        )
    )
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _chatMode = MutableStateFlow(ChatMode.THINKING)
    val chatMode: StateFlow<ChatMode> = _chatMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        addMessage(AiMessage(message = trimmed, isUser = true))

        viewModelScope.launch {
            _isLoading.value    = true
            _errorMessage.value = null
            val typingId = UUID.randomUUID().toString()
            addMessage(AiMessage(id = typingId, message = "…", isUser = false, isTyping = true))
            try {
                val reply = fetchAiReply(trimmed)
                _messages.value = _messages.value.filter { it.id != typingId } +
                    AiMessage(
                        message      = reply,
                        isUser       = false,
                        quickReplies = buildQuickReplies(trimmed)
                    )
            } catch (t: Throwable) {
                Log.e(TAG, "AI reply failed", t)
                _messages.value = _messages.value.filter { it.id != typingId } +
                    AiMessage(
                        message = "I couldn't reach the server right now. Please check your internet connection and try again.",
                        isUser  = false
                    )
                _errorMessage.value = "Unable to reach Haunter AI."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setChatMode(mode: ChatMode) { _chatMode.value = mode }
    fun clearError() { _errorMessage.value = null }

    private fun addMessage(msg: AiMessage) {
        _messages.value = _messages.value + msg
    }

    private fun buildQuickReplies(query: String): List<String> {
        val q = query.lowercase()
        return when {
            q.contains("dengue") || q.contains("malaria") || q.contains("mosquito") ->
                listOf("How to prevent?", "What are symptoms?", "Nearest hospital")
            q.contains("hospital") || q.contains("doctor") ->
                listOf("Emergency numbers", "Prevention tips", "Current outbreaks")
            q.contains("prevent") || q.contains("safety") ->
                listOf("Dengue prevention", "Water safety", "Vaccination info")
            else -> listOf("How to prevent?", "What are symptoms?", "Where to get help?")
        }
    }

    private suspend fun fetchAiReply(query: String): String = withContext(Dispatchers.IO) {
        when (_chatMode.value) {
            ChatMode.THINKING -> fetchBackendReply(query)
            ChatMode.SIMPLE   -> generateSimpleReply(query)
        }
    }

    private suspend fun fetchBackendReply(query: String): String {
        return try {
            val response = RetrofitClient.apiService.sendMessage(
                chatId = "default_chat_session",
                mode   = "smart",
                body   = ChatMessageRequest(text = buildPrompt(query))
            )
            response.response.ifBlank { generateLocalIntelligentReply(query) }
        } catch (t: Throwable) {
            Log.w(TAG, "Backend unreachable — using local DB intelligence", t)
            generateLocalIntelligentReply(query)
        }
    }

    private suspend fun generateLocalIntelligentReply(query: String): String =
        withContext(Dispatchers.IO) {
            try {
                val reports = database.outbreakReportDao().getAllReports().first()
                if (reports.isNotEmpty()) {
                    val q = query.lowercase()
                    val matched = reports.firstOrNull { r ->
                        q.contains(r.disease.lowercase()) ||
                        r.title.lowercase().split(" ").any { q.contains(it) && it.length > 3 }
                    } ?: reports.first()

                    val emoji = when (matched.severity.uppercase()) {
                        "CRITICAL" -> "🚨"; "HIGH" -> "⚠️"; else -> "ℹ️"
                    }
                    val locations = matched.locations.joinToString(", ").ifEmpty { "your region" }
                    val summary   = matched.summary.joinToString(". ").ifEmpty { matched.raw_text.take(200) }

                    return@withContext """
$emoji *Active Local Report Detected*

• *Disease:* ${matched.disease.replaceFirstChar { it.uppercase() }}
• *Source:* ${matched.source} (${matched.severity.uppercase()})
• *Location:* $locations
• *Details:* $summary

*Precautionary Steps:* Stay updated with the BimariHaunter feed, avoid contaminated areas, boil drinking water, and report new symptoms to local authorities.
                    """.trimIndent()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Local DB intelligence failed", e)
            }
            generateSimpleReply(query)
        }

    private fun generateSimpleReply(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("fever") || q.contains("bukhar") ->
                "🌡️ *Fever* can signal dengue, malaria, or typhoid.\n\nStay hydrated, rest, and see a doctor if it exceeds 38.5°C or lasts more than 2 days."
            q.contains("dengue") ->
                "🦟 *Dengue* is mosquito-borne.\n\n• Use repellents and wear long sleeves\n• Eliminate standing water\n• Seek urgent care for platelet drops or severe headache"
            q.contains("malaria") ->
                "🦟 *Malaria* spreads through mosquito bites.\n\nSymptoms: cyclical fever, chills, sweats. See a doctor immediately if suspected."
            q.contains("cholera") || q.contains("diarrhea") || q.contains("diarrhoea") ->
                "💧 *Cholera / Diarrhoea*\n\n• Drink only boiled or filtered water\n• Use ORS for dehydration\n• Report clusters to local health authorities"
            q.contains("hospital") || q.contains("doctor") || q.contains("emergency") ->
                "🏥 *Emergency Numbers*\n\n• Rescue Punjab: **1122**\n• Edhi Foundation: **115**\n• Aman Foundation (Karachi): **021-111-AMAN**"
            q.contains("prevent") || q.contains("safety") ->
                "🛡️ *Prevention*\n\n• Wash hands frequently\n• Drink boiled or filtered water\n• Use mosquito nets at night\n• Keep vaccinations up to date"
            q.contains("outbreak") || q.contains("alert") ->
                "📊 Check the *BimariHaunter feed and map* for real-time outbreak reports in your area."
            q.contains("polio") ->
                "💉 *Polio* is vaccine-preventable. Ensure all children under 5 receive the oral polio vaccine (OPV)."
            q.contains("hepatitis") ->
                "🩸 *Hepatitis* spreads via contaminated water and blood.\n\n• Get vaccinated for Hep A and B\n• Avoid sharing needles\n• Use clean water and cooked food"
            q.contains("water") || q.contains("pani") ->
                "💧 *Water Safety*\n\n• Always boil or filter tap water\n• Avoid ice made from tap water\n• Contaminated water causes cholera, typhoid, and hepatitis A"
            else ->
                "🩺 I'm *Haunter AI*, your offline health assistant.\n\nAsk me about dengue, malaria, cholera, fever, prevention, and more.\n\nFor real-time data, switch to **Thinking** mode."
        }
    }

    private fun buildPrompt(userQuery: String): String = """
You are Haunter AI, a friendly health advisor for BimariHaunter — a public health surveillance app for Pakistan.
Help users understand disease outbreaks, symptoms, and prevention. Be concise (under 150 words), empathetic, and always recommend professional care for serious concerns.

USER QUERY: $userQuery
RESPONSE:
    """.trimIndent()

    companion object { private const val TAG = "AiChatViewModel" }
}
