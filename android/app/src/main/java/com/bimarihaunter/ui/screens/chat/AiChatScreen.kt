package com.bimarihaunter.ui.screens.chat

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AiChatViewModel
import com.bimarihaunter.ui.viewmodel.AiMessage
import com.bimarihaunter.ui.viewmodel.ChatMode
import kotlinx.coroutines.launch

@Composable
fun AiChatScreen(onNavigateBack: () -> Unit = {}) {
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val chatViewModel: AiChatViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AiChatViewModel(context.applicationContext as Application) }
        }
    )

    val messages    by chatViewModel.messages.collectAsState()
    val isLoading   by chatViewModel.isLoading.collectAsState()
    val chatMode    by chatViewModel.chatMode.collectAsState()
    val errorMsg    by chatViewModel.errorMessage.collectAsState()

    val listState   = rememberLazyListState()
    val coroutine   = rememberCoroutineScope()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutine.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .navigationBarsPadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        BimarihaunterTopAppBar(
            title        = "Haunter AI",
            showBackArrow = true,
            onBackClick  = onNavigateBack,
            actions = {
                // Mode toggle pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CharcoalGrey)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeTab(
                        label     = "Thinking",
                        selected  = chatMode == ChatMode.THINKING,
                        onClick   = { chatViewModel.setChatMode(ChatMode.THINKING) }
                    )
                    ModeTab(
                        label     = "Simple",
                        selected  = chatMode == ChatMode.SIMPLE,
                        onClick   = { chatViewModel.setChatMode(ChatMode.SIMPLE) }
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
        )

        // ── Mode hint banner ─────────────────────────────────────────────────
        if (chatMode == ChatMode.SIMPLE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalGrey.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Simple mode — offline rule-based answers, no internet needed",
                    color    = MediumGrey,
                    fontSize = 11.sp,
                    fontFamily = InterFamily
                )
            }
        }

        // ── Error snackbar ───────────────────────────────────────────────────
        errorMsg?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmberRed.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(err, color = EmberRed, fontSize = 12.sp, fontFamily = InterFamily)
                TextButton(onClick = { chatViewModel.clearError() }) {
                    Text("Dismiss", color = EmberRed, fontSize = 11.sp)
                }
            }
        }

        // ── Message list ─────────────────────────────────────────────────────
        LazyColumn(
            state   = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.isTyping) {
                    TypingIndicator()
                } else {
                    AiMessageBubble(msg = msg, onQuickReply = { reply ->
                        chatViewModel.sendMessage(reply)
                    })
                }
            }
        }

        // ── Input bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CharcoalGrey)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value         = messageText,
                onValueChange = { messageText = it },
                placeholder   = {
                    Text("Ask me anything…", color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp)
                },
                modifier      = Modifier.weight(1f),
                colors        = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    cursorColor             = LimeGreen,
                    focusedTextColor        = OffWhite,
                    unfocusedTextColor      = OffWhite
                ),
                singleLine    = false,
                maxLines      = 4
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick  = {
                    val text = messageText.trim()
                    if (text.isNotBlank() && !isLoading) {
                        chatViewModel.sendMessage(text)
                        messageText = ""
                    }
                },
                enabled  = messageText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (messageText.isNotBlank() && !isLoading) LimeGreen else MediumGrey.copy(alpha = 0.4f))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color    = MidnightBlack,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, "Send", tint = MidnightBlack, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) LimeGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text       = label,
            color      = if (selected) MidnightBlack else MediumGrey,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = InterFamily
        )
    }
}

@Composable
private fun AiMessageBubble(msg: AiMessage, onQuickReply: (String) -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ghost avatar for AI messages
            if (!msg.isUser) {
                androidx.compose.foundation.Image(
                    painter            = painterResource(R.drawable.ghost_happy),
                    contentDescription = "Haunter AI",
                    modifier           = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CharcoalGrey)
                        .padding(4.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart    = 20.dp,
                            topEnd      = 20.dp,
                            bottomStart = if (msg.isUser) 20.dp else 4.dp,
                            bottomEnd   = if (msg.isUser) 4.dp else 20.dp
                        )
                    )
                    .background(if (msg.isUser) LimeGreen else CharcoalGrey)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = msg.message,
                    color      = if (msg.isUser) MidnightBlack else OffWhite,
                    fontFamily = InterFamily,
                    fontSize   = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        // Quick reply chips
        if (msg.quickReplies.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .padding(start = 44.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                msg.quickReplies.forEach { reply ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MidnightBlack)
                            .clickable { onQuickReply(reply) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text       = reply,
                            color      = LimeGreen,
                            fontSize   = 12.sp,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            maxLines   = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        androidx.compose.foundation.Image(
            painter            = painterResource(R.drawable.ghost_thinking),
            contentDescription = "Haunter AI thinking",
            modifier           = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CharcoalGrey)
                .padding(4.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp))
                .background(CharcoalGrey)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ThreeDotLoader()
        }
    }
}

@Composable
private fun ThreeDotLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue  = -6f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 130)
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(MediumGrey)
            )
        }
    }
}
