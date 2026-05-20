package com.bimarihaunter.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.ai.SLMManager
import com.bimarihaunter.db.BimarihaunterDatabase
import com.bimarihaunter.network.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val slmManager = SLMManager(application, BimarihaunterDatabase.getDatabase(application))

    private val _messages = MutableStateFlow<List<AiMessage>>(listOf(
        AiMessage(
            id = UUID.randomUUID().toString(),
            message = "Assalam o Alaikum! 👋 I'm your Bimarihaunter AI health assistant. Ask me about symptoms, prevention, or nearby outbreaks.",
            isUser = false,
            quickReplies = listOf("Prevention tips", "Nearest hospital", "Current outbreaks")
        )
    ))
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _chatMode = MutableStateFlow(ChatMode.THINKING)
    val chatMode: StateFlow<ChatMode> = _chatMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                slmManager.initialize()
            } catch (t: Throwable) {
                Log.w(TAG, "SLM initialization failed", t)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmedText = text.trim()
        appendMessage(
            AiMessage(
                id = UUID.randomUUID().toString(),
                message = trimmedText,
                isUser = true
            )
        )

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val aiResponse = fetchAiReply(trimmedText)
                appendMessage(
                    AiMessage(
                        id = UUID.randomUUID().toString(),
                        message = aiResponse,
                        isUser = false,
                        quickReplies = listOf("How to prevent?", "What are the symptoms?", "Where to get help?")
                    )
                )
            } catch (t: Throwable) {
                Log.e(TAG, "AI reply failed", t)
                _errorMessage.value = "Unable to process your message. Please try again."
                appendMessage(
                    AiMessage(
                        id = UUID.randomUUID().toString(),
                        message = "I couldn't generate a response right now. Please try again later.",
                        isUser = false
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

    private suspend fun fetchAiReply(userQuery: String): String = withContext(Dispatchers.IO) {
        when (chatMode.value) {
            ChatMode.THINKING -> fetchGeminiReply(userQuery)
            ChatMode.SIMPLE -> generateOfflineReply(userQuery)
        }
    }

    private suspend fun fetchGeminiReply(userQuery: String): String = withContext(Dispatchers.IO) {
        try {
            val contextText = slmManager.getContextSummary()
            val prompt = buildGeminiPrompt(userQuery, contextText)
            GeminiClient.generateReply(prompt)
        } catch (t: Throwable) {
            Log.w(TAG, "Gemini API call failed, falling back to offline SLM", t)
            generateOfflineReply(userQuery)
        }
    }

    private fun buildGeminiPrompt(userQuery: String, contextText: String): String {
        return """
            You are Bimarihaunter Smart Assistant.
            Use the following local outbreak context and answer the user's question clearly and safely.

            LOCAL FEED CONTEXT:
            $contextText

            USER QUERY: $userQuery

            RESPONSE:
        """.trimIndent()
    }

    private suspend fun generateOfflineReply(userQuery: String): String = withContext(Dispatchers.IO) {
        try {
            slmManager.generateOfflineResponse(userQuery)
        } catch (t: Throwable) {
            Log.e(TAG, "Offline SLM failed", t)
            _errorMessage.value = "Unable to fetch AI response. Please try again later."
            return@withContext "I don't have current data on that. Please check with local health authorities."
        }
    }

    private fun appendMessage(message: AiMessage) {
        _messages.value = _messages.value + message
    }

    override fun onCleared() {
        super.onCleared()
        slmManager.release()
    }

    companion object {
        private const val TAG = "AiChatViewModel"
    }
}
