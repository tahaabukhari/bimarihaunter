package com.bimarihaunter.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.components.ChatBubble
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun GroupChatScreen(
    groupId: String? = null,
    onNavigateBack: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val chatGroups by chatViewModel.chatGroups.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    
    val currentGroup = remember(chatGroups, groupId) {
        chatGroups.find { it.id == groupId }
    }

    LaunchedEffect(groupId) {
        if (groupId != null) {
            chatViewModel.loadMessages(groupId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack).navigationBarsPadding()) {
        // Top bar
        BimarihaunterTopAppBar(
            title = currentGroup?.name ?: "Community Chat",
            showBackArrow = true,
            onBackClick = onNavigateBack,
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Link, "Info", tint = OffWhite)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, "More", tint = OffWhite)
                }
            }
        )

        // Subtitle
        Text("Active discussion group", color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily,
            modifier = Modifier.padding(start = 56.dp, top = 0.dp, bottom = 8.dp))

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages.sortedByDescending { it.timestamp }) { msg ->
                val isOutgoing = msg.senderId == currentUserId
                val formattedTime = if (msg.timestamp > 0) {
                    val date = java.util.Date(msg.timestamp)
                    val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    format.format(date)
                } else ""

                ChatBubble(
                    message = msg.text,
                    isOutgoing = isOutgoing,
                    senderName = if (!isOutgoing) msg.senderName else null,
                    timestamp = formattedTime,
                    showAvatar = !isOutgoing
                )
            }
        }

        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp)).background(CharcoalGrey)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.AttachFile, "Attach", tint = MediumGrey)
            }
            TextField(
                value = messageText, onValueChange = { messageText = it },
                placeholder = { Text("Message...", color = MediumGrey, fontFamily = InterFamily) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = LimeGreen,
                    focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                ),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (messageText.isNotBlank() && groupId != null) {
                        chatViewModel.sendMessage(groupId, messageText.trim())
                        messageText = ""
                    }
                },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(LimeGreen)
            ) {
                Icon(Icons.Default.Send, "Send", tint = MidnightBlack, modifier = Modifier.size(20.dp))
            }
        }
    }
}
