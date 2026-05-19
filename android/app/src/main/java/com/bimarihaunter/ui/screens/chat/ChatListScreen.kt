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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.*
import com.bimarihaunter.ui.theme.*

data class ChatItem(
    val id: String, val name: String, val initials: String,
    val lastMessage: String, val timestamp: String, val unreadCount: Int = 0
)

private val mockChats = listOf(
    ChatItem("1", "Dengue Watch — Lahore", "DW",
        "Ahmad: Has anyone seen fumigation in Gulberg?", "2m ago", 3),
    ChatItem("2", "Flood Relief Coordination", "FR",
        "Sana: We need more volunteers at the camp", "15m ago", 1),
    ChatItem("3", "Karachi Health Workers", "KH",
        "Dr. Fatima: New cases reported in Korangi", "1h ago", 0),
    ChatItem("4", "COVID Updates Pakistan", "CU",
        "Ali: WHO released new guidelines today", "3h ago", 0),
    ChatItem("5", "Peshawar Water Crisis", "PW",
        "Imran: Water testing results are out", "5h ago", 2),
)

@Composable
fun ChatListScreen(
    onNavigateToGroupChat: (String) -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToCreateGroup: () -> Unit = {}
) {
    var selectedChip by remember { mutableStateOf("All") }

    Box(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        Column {
            BimarihaunterTopAppBar(
                title = "Community",
                actions = {
                    IconButton(onClick = onNavigateToCreateGroup) {
                        Icon(Icons.Default.Edit, "Compose", tint = OffWhite)
                    }
                }
            )

            FilterChipRow(
                chips = listOf("All", "Groups", "Direct"),
                selectedChip = selectedChip,
                onChipSelected = { selectedChip = it },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // AI Card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CharcoalGrey)
                            .clickable { onNavigateToAiChat() }
                    ) {
                        Box(Modifier.width(4.dp).height(72.dp).background(LimeGreen))
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.ghost_happy), "AI",
                                Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Bimarihaunter AI", color = OffWhite,
                                    fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp)
                                Text("Ask me about health & prevention",
                                    color = MediumGrey, fontSize = 13.sp, fontFamily = InterFamily)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Section header
                item {
                    Text("RECENT", color = MediumGrey, fontSize = 11.sp,
                        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp))
                }

                // Chat list
                items(mockChats) { chat ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToGroupChat(chat.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(Modifier.size(48.dp).clip(CircleShape).background(CharcoalGrey),
                            contentAlignment = Alignment.Center) {
                            Text(chat.initials, color = OffWhite, fontWeight = FontWeight.Bold,
                                fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chat.name, color = OffWhite, fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(chat.lastMessage, color = MediumGrey, fontSize = 13.sp,
                                fontFamily = InterFamily, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(chat.timestamp, color = MediumGrey, fontSize = 11.sp,
                                fontFamily = InterFamily)
                            if (chat.unreadCount > 0) {
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.size(20.dp).clip(CircleShape).background(LimeGreen),
                                    contentAlignment = Alignment.Center) {
                                    Text("${chat.unreadCount}", color = MidnightBlack,
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onNavigateToCreateGroup,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = LimeGreen,
            contentColor = MidnightBlack,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, "Create Group")
        }
    }
}
