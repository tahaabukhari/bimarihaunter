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
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.components.ChatBubble
import com.bimarihaunter.ui.theme.*

data class ChatMessage(
    val id: String, val sender: String, val message: String,
    val timestamp: String, val isOutgoing: Boolean, val isSystem: Boolean = false
)

private val mockMessages = listOf(
    ChatMessage("1", "Ahmad", "Has anyone seen fumigation drives in Gulberg area?",
        "10:30 AM", false),
    ChatMessage("2", "You", "Yes, I saw a team near MM Alam Road this morning",
        "10:32 AM", true),
    ChatMessage("3", "Sana", "My neighbor got dengue last week. Be careful everyone!",
        "10:35 AM", false),
    ChatMessage("4", "Dr. Fatima", "Please make sure to wear full sleeves and use repellent. Cases are rising rapidly in Model Town area.",
        "10:38 AM", false),
    ChatMessage("5", "You", "Thanks Dr. Fatima. Any specific repellent brand you recommend?",
        "10:40 AM", true),
    ChatMessage("6", "System", "Ahmad shared a news article",
        "10:42 AM", false, true),
    ChatMessage("7", "Imran", "I've reported standing water near my building to the health department",
        "10:45 AM", false),
)

@Composable
fun GroupChatScreen(
    groupId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        // Top bar
        BimarihaunterTopAppBar(
            title = "Dengue Watch — Lahore",
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
        Text("24 members", color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily,
            modifier = Modifier.padding(start = 56.dp, top = 0.dp, bottom = 8.dp))

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(mockMessages.reversed()) { msg ->
                if (msg.isSystem) {
                    // System message
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(msg.message, color = MediumGrey, fontSize = 12.sp,
                            fontFamily = InterFamily)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(CharcoalGrey)
                            .padding(12.dp)) {
                            Text("Punjab Govt Activates Emergency Response for Dengue",
                                color = OffWhite, fontSize = 13.sp, fontFamily = InterFamily)
                        }
                    }
                } else {
                    ChatBubble(
                        message = msg.message,
                        isOutgoing = msg.isOutgoing,
                        senderName = if (!msg.isOutgoing) msg.sender else null,
                        timestamp = msg.timestamp,
                        showAvatar = !msg.isOutgoing
                    )
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
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
                onClick = { messageText = "" },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(LimeGreen)
            ) {
                Icon(Icons.Default.Send, "Send", tint = MidnightBlack, modifier = Modifier.size(20.dp))
            }
        }
    }
}
