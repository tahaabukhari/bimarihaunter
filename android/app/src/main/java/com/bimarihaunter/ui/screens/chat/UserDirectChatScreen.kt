package com.bimarihaunter.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.data.model.Message
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.components.ChatBubble
import com.bimarihaunter.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// ─────────────────────────── ViewModel ───────────────────────────────────────

/**
 * Manages a 1-to-1 direct chat between the current user and [otherUserId].
 * Chat documents live at:  Firestore → direct_chats / {chatId} / messages
 * where chatId = sorted(uid1, uid2).join("_")
 */
class UserDirectChatViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _messages   = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    /** Derive a deterministic chat-room ID from the two participants. */
    fun chatId(uid1: String, uid2: String): String =
        listOf(uid1, uid2).sorted().joinToString("_")

    /** Start listening for real-time messages in this direct chat. */
    fun startListening(chatId: String) {
        listenerRegistration?.remove()
        listenerRegistration = db.collection("direct_chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { Timber.e(err, "Direct chat listener error"); return@addSnapshotListener }
                _messages.value = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    /** Send a new message to the direct chat. */
    fun sendMessage(chatId: String, text: String) {
        val user = auth.currentUser ?: return
        val message = hashMapOf(
            "senderId"   to user.uid,
            "senderName" to (user.displayName ?: user.email ?: "User"),
            "text"       to text,
            "timestamp"  to System.currentTimeMillis(),
            "isSystem"   to false
        )
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("direct_chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .await()
            } catch (e: Exception) {
                Timber.e(e, "Failed to send direct message")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

// ─────────────────────────── Screen ──────────────────────────────────────────

/**
 * 1-to-1 direct chat screen.
 *
 * @param otherUserId  Firestore UID of the chat partner.
 * @param otherUserName Display name shown in the top bar.
 */
@Composable
fun UserDirectChatScreen(
    otherUserId: String,
    otherUserName: String,
    onNavigateBack: () -> Unit = {},
    viewModel: UserDirectChatViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val messages    by viewModel.messages.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()

    val chatId = remember(viewModel.currentUid, otherUserId) {
        viewModel.chatId(viewModel.currentUid, otherUserId)
    }

    // Start real-time listener once
    LaunchedEffect(chatId) {
        viewModel.startListening(chatId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        // ─── Top Bar ──────────────────────────────────────────────────────
        BimarihaunterTopAppBar(
            title       = otherUserName,
            showBackArrow = true,
            onBackClick   = onNavigateBack
        )

        // Online status badge
        Row(
            modifier = Modifier.padding(start = 56.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(LimeGreen)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "Direct message",
                color      = MediumGrey,
                fontFamily = InterFamily,
                fontSize   = 12.sp
            )
        }

        // ─── Messages ─────────────────────────────────────────────────────
        LazyColumn(
            modifier       = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout  = true,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages.sortedByDescending { it.timestamp }) { msg ->
                val isOutgoing = msg.senderId == viewModel.currentUid
                val formattedTime = if (msg.timestamp > 0) {
                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(msg.timestamp))
                } else ""

                ChatBubble(
                    message     = msg.text,
                    isOutgoing  = isOutgoing,
                    senderName  = if (!isOutgoing) msg.senderName else null,
                    timestamp   = formattedTime,
                    showAvatar  = !isOutgoing
                )
            }
        }

        // Typing / loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier    = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 16.dp),
                color       = LimeGreen,
                trackColor  = CharcoalGrey
            )
        }

        // ─── Input Bar ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CharcoalGrey)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value           = messageText,
                onValueChange   = { messageText = it },
                placeholder     = {
                    Text("Message $otherUserName…", color = MediumGrey, fontFamily = InterFamily)
                },
                modifier        = Modifier.weight(1f),
                colors          = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor             = LimeGreen,
                    focusedTextColor        = OffWhite,
                    unfocusedTextColor      = OffWhite
                ),
                singleLine      = true
            )

            IconButton(
                onClick  = {
                    val text = messageText.trim()
                    if (text.isNotBlank()) {
                        viewModel.sendMessage(chatId, text)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LimeGreen)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MidnightBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
