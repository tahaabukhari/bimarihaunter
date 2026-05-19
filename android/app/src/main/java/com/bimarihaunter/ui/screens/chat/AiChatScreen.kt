package com.bimarihaunter.ui.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
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
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.components.ChatBubble
import com.bimarihaunter.ui.theme.*

data class AiMessage(
    val id: String, val message: String, val isUser: Boolean,
    val quickReplies: List<String> = emptyList()
)

private val mockAiMessages = listOf(
    AiMessage("1", "Assalam o Alaikum! 👋 I'm your Bimarihaunter AI health assistant. I can help you with disease prevention tips, find nearby hospitals, or give you the latest health alerts. What would you like to know?",
        false, listOf("Prevention tips", "Nearest hospital", "Current alerts")),
    AiMessage("2", "What are the symptoms of dengue?", true),
    AiMessage("3", "Dengue fever symptoms typically appear 4-10 days after infection:\n\n🔴 High fever (40°C/104°F)\n🔴 Severe headache\n🔴 Pain behind the eyes\n🔴 Muscle and joint pain\n🔴 Nausea and vomiting\n🔴 Skin rash (appears 2-5 days after fever)\n🔴 Mild bleeding (nose/gums)\n\n⚠️ Seek immediate medical care if you experience severe abdominal pain, persistent vomiting, or bleeding.",
        false, listOf("How to prevent?", "Where to get tested?", "Is it serious?")),
    AiMessage("4", "How to prevent dengue?", true),
    AiMessage("5", "Here are key prevention measures for dengue:\n\n✅ Use mosquito repellent (DEET-based)\n✅ Wear long sleeves and pants\n✅ Remove standing water around your home\n✅ Use mosquito nets while sleeping\n✅ Keep windows/doors closed or screened\n✅ Use coils or electric vapor mats\n\n🏥 Fumigation drives are active in Lahore — check your local area schedule.",
        false, listOf("Current outbreaks", "Nearest hospital")),
)

@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack).navigationBarsPadding()) {
        // Top bar
        BimarihaunterTopAppBar(
            title = "Bimarihaunter AI",
            showBackArrow = true,
            onBackClick = onNavigateBack,
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Info, "Info", tint = OffWhite)
                }
            }
        )

        // Subtitle
        Text("Health & Prevention Assistant", color = MediumGrey, fontSize = 12.sp,
            fontFamily = InterFamily, modifier = Modifier.padding(start = 56.dp, bottom = 8.dp))

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(mockAiMessages.reversed()) { msg ->
                Column {
                    if (msg.isUser) {
                        ChatBubble(message = msg.message, isOutgoing = true)
                    } else {
                        // AI message with ghost avatar
                        Row(verticalAlignment = Alignment.Top) {
                            Image(painterResource(R.drawable.ghost_happy), "AI",
                                Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp,
                                        bottomEnd = 20.dp, bottomStart = 4.dp))
                                    .background(CharcoalGrey).padding(14.dp)
                            ) {
                                Text(msg.message, color = OffWhite, fontFamily = InterFamily,
                                    fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }

                        // Quick reply chips
                        if (msg.quickReplies.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.padding(start = 36.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                msg.quickReplies.forEach { reply ->
                                    Text(reply, color = OffWhite, fontSize = 12.sp,
                                        fontFamily = InterFamily,
                                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                            .background(CharcoalGrey)
                                            .clickable { messageText = reply }
                                            .padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
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
                placeholder = { Text("Ask me anything...", color = MediumGrey, fontFamily = InterFamily) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = LimeGreen,
                    focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                ), singleLine = true
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
