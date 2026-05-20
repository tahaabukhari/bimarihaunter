package com.bimarihaunter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.data.model.*
import com.bimarihaunter.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel() : ViewModel() {
    private val repository = FirebaseRepository()
    val newsArticles: StateFlow<List<NewsArticle>> = repository.getNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class AlertsViewModel() : ViewModel() {
    private val repository = FirebaseRepository()
    val alerts: StateFlow<List<AlertData>> = repository.getAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class ChatViewModel() : ViewModel() {
    private val repository = FirebaseRepository()
    val chatGroups: StateFlow<List<ChatGroup>> = repository.getChatGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var activeChatListenerJob: kotlinx.coroutines.Job? = null

    fun loadMessages(chatGroupId: String) {
        activeChatListenerJob?.cancel()
        activeChatListenerJob = viewModelScope.launch {
            repository.getMessages(chatGroupId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun sendMessage(chatGroupId: String, text: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val senderName = currentUser.displayName ?: currentUser.phoneNumber ?: "User"
        val message = Message(
            senderId = currentUser.uid,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.sendMessage(chatGroupId, message)
        }
    }
}
