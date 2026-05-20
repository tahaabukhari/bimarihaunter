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
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.*
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.ChatViewModel

@Composable
fun ChatListScreen(
    onNavigateToGroupChat: (String) -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToCreateGroup: () -> Unit = {},
    onNavigateToAddFriends: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    var selectedChip by remember { mutableStateOf("All") }
    val chatGroups by chatViewModel.chatGroups.collectAsState()

    val filteredGroups = remember(chatGroups, selectedChip) {
        if (selectedChip == "All") {
            chatGroups
        } else {
            chatGroups.filter { it.category.equals(selectedChip, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        Column {
            BimarihaunterTopAppBar(
                title = "Community",
                actions = {
                    IconButton(onClick = onNavigateToAddFriends) {
                        Icon(Icons.Default.PersonAdd, "Add Friends", tint = OffWhite)
                    }
                    IconButton(onClick = onNavigateToCreateGroup) {
                        Icon(Icons.Default.Edit, "Compose", tint = OffWhite)
                    }
                }
            )

            FilterChipRow(
                chips = listOf("All", "Health", "Crisis", "General"),
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
                items(filteredGroups) { chat ->
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
                            val initials = chat.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("").uppercase()
                            Text(initials.ifEmpty { "G" }, color = OffWhite, fontWeight = FontWeight.Bold,
                                fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chat.name, color = OffWhite, fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(chat.lastMessage.ifEmpty { "No messages yet" }, color = MediumGrey, fontSize = 13.sp,
                                fontFamily = InterFamily, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                            Text(chat.lastMessageTime.ifEmpty { "Just now" }, color = MediumGrey, fontSize = 11.sp,
                                fontFamily = InterFamily)
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
