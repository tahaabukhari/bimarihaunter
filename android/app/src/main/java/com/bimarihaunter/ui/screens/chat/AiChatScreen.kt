package com.bimarihaunter.ui.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.components.ChatBubble
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AiChatViewModel
import com.bimarihaunter.ui.viewmodel.AiMessage

@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val chatViewModel: AiChatViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AiChatViewModel(context.applicationContext as Application)
            }
        }
    )
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val errorMessageState by chatViewModel.errorMessage.collectAsState()
    val errorMessage = errorMessageState

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack).navigationBarsPadding()) {
        BimarihaunterTopAppBar(
            title = "Haunter AI",
            showBackArrow = true,
            onBackClick = onNavigateBack,
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Info, "Info", tint = OffWhite)
                }
            }
        )

        Text(
            "🎃 Health & Prevention Assistant · Powered by Gemini",
            color = MediumGrey,
            fontSize = 12.sp,
            fontFamily = InterFamily,
            modifier = Modifier.padding(start = 56.dp, bottom = 8.dp)
        )

        val chatMode by chatViewModel.chatMode.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = chatMode == com.bimarihaunter.ui.viewmodel.ChatMode.THINKING,
                onClick = { chatViewModel.setChatMode(com.bimarihaunter.ui.viewmodel.ChatMode.THINKING) },
                label = {
                    Text(
                        "Thinking",
                        color = if (chatMode == com.bimarihaunter.ui.viewmodel.ChatMode.THINKING) MidnightBlack else OffWhite
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LimeGreen,
                    containerColor = CharcoalGrey
                ),
                border = null
            )
            FilterChip(
                selected = chatMode == com.bimarihaunter.ui.viewmodel.ChatMode.SIMPLE,
                onClick = { chatViewModel.setChatMode(com.bimarihaunter.ui.viewmodel.ChatMode.SIMPLE) },
                label = {
                    Text(
                        "Simple",
                        color = if (chatMode == com.bimarihaunter.ui.viewmodel.ChatMode.SIMPLE) MidnightBlack else OffWhite
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LimeGreen,
                    containerColor = CharcoalGrey
                ),
                border = null
            )
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = LimeGreen,
                trackColor = CharcoalGrey
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages.reversed()) { msg ->
                Column {
                    if (msg.isUser) {
                        ChatBubble(message = msg.message, isOutgoing = true)
                    } else {
                        Row(verticalAlignment = Alignment.Top) {
                            Image(
                                painterResource(R.drawable.ghost_happy),
                                "AI",
                                Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.widthIn(max = 280.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 20.dp,
                                            topEnd = 20.dp,
                                            bottomEnd = 20.dp,
                                            bottomStart = 4.dp
                                        )
                                    )
                                    .background(CharcoalGrey)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    msg.message,
                                    color = OffWhite,
                                    fontFamily = InterFamily,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        if (msg.quickReplies.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .padding(start = 36.dp)
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                msg.quickReplies.forEach { reply ->
                                    Text(
                                        reply,
                                        color = OffWhite,
                                        fontSize = 12.sp,
                                        fontFamily = InterFamily,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CharcoalGrey)
                                            .clickable {
                                                messageText = reply
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CharcoalGrey)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = {
                    Text(
                        "Ask me anything...",
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
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.sendMessage(messageText.trim())
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
                    "Send",
                    tint = MidnightBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
