package com.bimarihaunter.ui.screens.chat

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.data.model.Message
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectChatScreen(
    chatId: String? = null,
    friendId: String? = null,
    friendName: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToArticleDetail: (String) -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // Message context menu state
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var isEditingMode by remember { mutableStateOf(false) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val messages by chatViewModel.messages.collectAsState()
    val blockedUsers by chatViewModel.blockedUsers.collectAsState()

    val isUserBlocked = remember(blockedUsers, friendId) {
        friendId != null && blockedUsers.contains(friendId)
    }

    LaunchedEffect(chatId) {
        if (chatId != null) {
            chatViewModel.loadDirectMessages(chatId)
            chatViewModel.loadBlockedUsers()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .navigationBarsPadding()
    ) {
        // Custom Top Bar with Block Menu
        BimarihaunterTopAppBar(
            title = friendName ?: "Direct Chat",
            showBackArrow = true,
            onBackClick = onNavigateBack,
            actions = {
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, "More Options", tint = OffWhite)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CharcoalGrey)
                    ) {
                        if (isUserBlocked) {
                            DropdownMenuItem(
                                text = { Text("Unblock User", color = LimeGreen, fontFamily = SpaceGroteskFamily) },
                                onClick = {
                                    showMenu = false
                                    if (friendId != null) {
                                        chatViewModel.unblockUser(friendId)
                                        Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Block User", color = EmberRed, fontFamily = SpaceGroteskFamily) },
                                onClick = {
                                    showMenu = false
                                    if (friendId != null) {
                                        chatViewModel.blockUser(friendId) { success ->
                                            if (success) {
                                                Toast.makeText(context, "User blocked successfully", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to block user", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        )

        // Block Warning Banner
        if (isUserBlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmberRed.copy(alpha = 0.2f))
                    .clickable {
                        if (friendId != null) {
                            chatViewModel.unblockUser(friendId)
                            Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, "Blocked", tint = EmberRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "You blocked this user. Tap to Unblock.",
                        color = EmberRed,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages.sortedByDescending { it.timestamp }) { msg ->
                val isOutgoing = msg.senderId == currentUserId
                val formattedTime = if (msg.timestamp > 0) {
                    val date = java.util.Date(msg.timestamp)
                    val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    format.format(date)
                } else ""

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.combinedClickable(
                            onLongClick = {
                                if (!msg.deleted) {
                                    selectedMessageForMenu = msg
                                }
                            },
                            onClick = {
                                if (msg.sharedPostId != null) {
                                    onNavigateToArticleDetail(msg.sharedPostId)
                                }
                            }
                        )
                    ) {
                        // Incoming Avatar
                        if (!isOutgoing) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CharcoalGrey),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (msg.senderName.firstOrNull() ?: 'U').toString().uppercase(),
                                    color = LimeGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Message bubble body
                        Column(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (isOutgoing) 20.dp else 4.dp,
                                        bottomEnd = if (isOutgoing) 4.dp else 20.dp
                                    )
                                )
                                .background(if (isOutgoing) LimeGreen else CharcoalGrey)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // If contains shared outbreak report card
                            if (msg.sharedPostId != null) {
                                OutbreakPreviewCard(
                                    title = msg.sharedPostTitle ?: "Outbreak Report",
                                    disease = msg.sharedPostDisease ?: "Infection",
                                    severity = msg.sharedPostSeverity ?: "INFO",
                                    url = msg.sharedPostUrl ?: "",
                                    isOutgoing = isOutgoing
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            // Message text
                            Text(
                                text = msg.text,
                                color = if (isOutgoing) MidnightBlack else OffWhite,
                                fontFamily = InterFamily,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontStyle = if (msg.deleted) FontStyle.Italic else FontStyle.Normal
                            )
                        }
                    }

                    // Bottom info label (Time + edited flags)
                    Row(
                        modifier = Modifier.padding(
                            start = if (!isOutgoing) 36.dp else 0.dp,
                            top = 2.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            color = MediumGrey,
                            fontSize = 10.sp,
                            fontFamily = InterFamily
                        )
                        if (msg.edited && !msg.deleted) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "• Edited",
                                color = MediumGrey,
                                fontSize = 10.sp,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            }
        }

        // Edit Mode Banner
        if (isEditingMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalGrey)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, "Editing", tint = LimeGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Editing Message",
                        color = OffWhite,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = {
                        isEditingMode = false
                        editingMessageId = null
                        messageText = ""
                    },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(Icons.Default.Close, "Cancel", tint = MediumGrey)
                }
            }
        }

        // Message Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CharcoalGrey)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { if (!isUserBlocked) messageText = it },
                placeholder = {
                    Text(
                        text = if (isUserBlocked) "Messaging disabled" else "Message...",
                        color = MediumGrey,
                        fontFamily = InterFamily
                    )
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = LimeGreen,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite
                ),
                singleLine = true,
                enabled = !isUserBlocked
            )

            IconButton(
                onClick = {
                    if (messageText.isNotBlank() && chatId != null && friendId != null && friendName != null) {
                        if (isEditingMode && editingMessageId != null) {
                            chatViewModel.editMessage(chatId, editingMessageId!!, messageText.trim(), isGroup = false)
                            isEditingMode = false
                            editingMessageId = null
                        } else {
                            chatViewModel.sendDirectMessage(
                                chatId = chatId,
                                recipientId = friendId,
                                recipientName = friendName,
                                text = messageText.trim()
                            )
                        }
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (messageText.isNotBlank() && !isUserBlocked) LimeGreen else MediumGrey),
                enabled = messageText.isNotBlank() && !isUserBlocked
            ) {
                Icon(
                    imageVector = if (isEditingMode) Icons.Default.Check else Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MidnightBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Message Actions bottom sheet dialog
    selectedMessageForMenu?.let { msg ->
        val isMyMsg = msg.senderId == currentUserId
        
        Dialog(onDismissRequest = { selectedMessageForMenu = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalGrey),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Message Actions",
                        color = OffWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Action 1: Copy Text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                clipboardManager.setText(AnnotatedString(msg.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = OffWhite, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Copy message", color = OffWhite, fontFamily = InterFamily)
                    }

                    // Action 2: Edit (Only own message, and not a shared post)
                    if (isMyMsg && msg.sharedPostId == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMessageForMenu = null
                                    isEditingMode = true
                                    editingMessageId = msg.id
                                    messageText = msg.text
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, "Edit", tint = OffWhite, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("Edit message", color = OffWhite, fontFamily = InterFamily)
                        }
                    }

                    // Action 3: Delete (Only own message)
                    if (isMyMsg) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMessageForMenu = null
                                    if (chatId != null) {
                                        chatViewModel.deleteMessage(chatId, msg.id, isGroup = false)
                                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = EmberRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("Delete message", color = EmberRed, fontFamily = InterFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OutbreakPreviewCard(
    title: String,
    disease: String,
    severity: String,
    url: String,
    isOutgoing: Boolean
) {
    val cardBgColor = when (severity.uppercase()) {
        "CRITICAL" -> EmberRed.copy(alpha = 0.15f)
        "WARNING", "HIGH" -> GoldWarning.copy(alpha = 0.15f)
        else -> LimeGreen.copy(alpha = 0.12f)
    }

    val badgeColor = when (severity.uppercase()) {
        "CRITICAL" -> EmberRed
        "WARNING", "HIGH" -> GoldWarning
        else -> LimeGreen
    }

    val textColor = if (isOutgoing) MidnightBlack else OffWhite
    val subTextColor = if (isOutgoing) MidnightBlack.copy(alpha = 0.7f) else MediumGrey

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = disease.uppercase(),
                    color = badgeColor,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = severity,
                        color = MidnightBlack,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFamily
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                color = textColor,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map",
                    tint = badgeColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Tap to view outbreak details",
                    color = subTextColor,
                    fontSize = 11.sp,
                    fontFamily = InterFamily
                )
            }
        }
    }
}
