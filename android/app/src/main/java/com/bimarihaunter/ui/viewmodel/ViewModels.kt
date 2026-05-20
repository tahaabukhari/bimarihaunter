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

    val directChats: StateFlow<List<ChatGroup>> = repository.getDirectChats(
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<String>>(emptyList())
    val blockedUsers: StateFlow<List<String>> = _blockedUsers.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var activeChatListenerJob: kotlinx.coroutines.Job? = null

    init {
        loadFriends()
        loadBlockedUsers()
    }

    fun loadMessages(chatGroupId: String) {
        activeChatListenerJob?.cancel()
        activeChatListenerJob = viewModelScope.launch {
            repository.getMessages(chatGroupId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun loadDirectMessages(chatId: String) {
        activeChatListenerJob?.cancel()
        activeChatListenerJob = viewModelScope.launch {
            repository.getDirectMessages(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun sendMessage(chatGroupId: String, text: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val senderName = currentUser.displayName ?: currentUser.email ?: "User"
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

    fun sendDirectMessage(
        chatId: String,
        recipientId: String,
        recipientName: String,
        text: String,
        sharedPostId: String? = null,
        sharedPostTitle: String? = null,
        sharedPostDisease: String? = null,
        sharedPostSeverity: String? = null,
        sharedPostUrl: String? = null
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val senderName = currentUser.displayName ?: currentUser.email ?: "User"
        val message = Message(
            senderId = currentUser.uid,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            sharedPostId = sharedPostId,
            sharedPostTitle = sharedPostTitle,
            sharedPostDisease = sharedPostDisease,
            sharedPostSeverity = sharedPostSeverity,
            sharedPostUrl = sharedPostUrl
        )
        viewModelScope.launch {
            repository.sendDirectMessage(
                chatId = chatId,
                senderId = currentUser.uid,
                senderName = senderName,
                recipientId = recipientId,
                recipientName = recipientName,
                message = message
            )
        }
    }

    fun editMessage(chatId: String, messageId: String, newText: String, isGroup: Boolean) {
        viewModelScope.launch {
            if (isGroup) {
                repository.editGroupMessage(chatId, messageId, newText)
            } else {
                repository.editDirectMessage(chatId, messageId, newText)
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String, isGroup: Boolean) {
        viewModelScope.launch {
            if (isGroup) {
                repository.deleteGroupMessage(chatId, messageId)
            } else {
                repository.deleteDirectMessage(chatId, messageId)
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            _friends.value = repository.getFriends()
        }
    }

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _blockedUsers.value = repository.getBlockedUsers()
        }
    }

    fun searchUsers(query: String) {
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repository.searchUsers(query)
            _isSearching.value = false
        }
    }

    fun addFriend(friendId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.addFriend(friendId)
            if (success) {
                loadFriends()
            }
            onResult(success)
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            val success = repository.removeFriend(friendId)
            if (success) {
                loadFriends()
            }
        }
    }

    fun blockUser(blockedId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.blockUser(blockedId)
            if (success) {
                loadBlockedUsers()
                loadFriends() // Blocking automatically removes from friends
            }
            onResult(success)
        }
    }

    fun unblockUser(blockedId: String) {
        viewModelScope.launch {
            val success = repository.unblockUser(blockedId)
            if (success) {
                loadBlockedUsers()
            }
        }
    }
}
