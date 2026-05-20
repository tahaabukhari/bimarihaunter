package com.bimarihaunter.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.R
import com.bimarihaunter.data.model.*
import com.bimarihaunter.ui.components.*
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatListScreen(
    onNavigateToGroupChat: (String) -> Unit = {},
    onNavigateToDirectChat: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToCreateGroup: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    var activeTab by remember { mutableStateOf("Groups") } // "Groups" or "DMs"
    var selectedChip by remember { mutableStateOf("All") }
    var showSearchDialog by remember { mutableStateOf(false) }

    val chatGroups by chatViewModel.chatGroups.collectAsState()
    val directChats by chatViewModel.directChats.collectAsState()
    val friends by chatViewModel.friends.collectAsState()
    val blockedUsers by chatViewModel.blockedUsers.collectAsState()

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val filteredGroups = remember(chatGroups, selectedChip) {
        if (selectedChip == "All") {
            chatGroups
        } else {
            chatGroups.filter { it.category.equals(selectedChip, ignoreCase = true) }
        }
    }

    // Refresh friends list when screen becomes active
    LaunchedEffect(Unit) {
        chatViewModel.loadFriends()
        chatViewModel.loadBlockedUsers()
    }

    Box(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        Column {
            BimarihaunterTopAppBar(
                title = "Community",
                actions = {
                    if (activeTab == "Groups") {
                        IconButton(onClick = onNavigateToCreateGroup) {
                            Icon(Icons.Default.Add, "New Group", tint = OffWhite)
                        }
                    } else {
                        IconButton(onClick = { showSearchDialog = true }) {
                            Icon(Icons.Default.Search, "Find Friends", tint = OffWhite)
                        }
                    }
                }
            )

            // Premium sliding-styled tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CharcoalGrey)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabButton(
                    text = "Outbreak Groups",
                    isActive = activeTab == "Groups",
                    onClick = { activeTab = "Groups" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Direct Messages",
                    isActive = activeTab == "DMs",
                    onClick = { activeTab = "DMs" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (activeTab == "Groups") {
                // Outbreak Group Chat view
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
                        Text("ACTIVE THREADS", color = MediumGrey, fontSize = 11.sp,
                            fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }

                    // Chat list
                    if (filteredGroups.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No discussions found", color = MediumGrey, fontFamily = InterFamily)
                            }
                        }
                    } else {
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
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                // Direct Messages view
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Active DMs list
                    item {
                        Text("CONVERSATIONS", color = MediumGrey, fontSize = 11.sp,
                            fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
                    }

                    val activeDms = directChats.filter { chat ->
                        chat.participants.contains(currentUid) &&
                        chat.participants.none { blockedUsers.contains(it) }
                    }

                    if (activeDms.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = CharcoalGrey.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.MailOutline, "No DMs", tint = LimeGreen, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Your Inbox is Quiet",
                                        color = OffWhite,
                                        fontFamily = SpaceGroteskFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Tap 'Add Friend' below or the search icon to connect with other users.",
                                        color = MediumGrey,
                                        fontSize = 12.sp,
                                        fontFamily = InterFamily,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(activeDms) { chat ->
                            val peerName = chat.names.filterKeys { it != currentUid }.values.firstOrNull() ?: chat.name
                            val peerId = chat.participants.firstOrNull { it != currentUid } ?: ""
                            
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigateToDirectChat(chat.id, peerId, peerName) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(Modifier.size(48.dp).clip(CircleShape).background(LimeGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center) {
                                    val initials = peerName.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("").uppercase()
                                    Text(initials.ifEmpty { "U" }, color = LimeGreen, fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(peerName, color = OffWhite, fontFamily = SpaceGroteskFamily,
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
                    }

                    // Friends Quick-start section
                    item {
                        Text("MY FRIENDS", color = MediumGrey, fontSize = 11.sp,
                            fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp, top = 20.dp))
                    }

                    val unblockedFriends = friends.filter { !blockedUsers.contains(it.uid) }

                    if (unblockedFriends.isEmpty()) {
                        item {
                            Text(
                                "No friends added yet. Search for UIDs or emails to add friends!",
                                color = MediumGrey,
                                fontSize = 13.sp,
                                fontFamily = InterFamily,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(unblockedFriends) { friend ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val chatId = if (currentUid < friend.uid) "${currentUid}_${friend.uid}" else "${friend.uid}_${currentUid}"
                                        onNavigateToDirectChat(chatId, friend.uid, friend.name)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(CharcoalGrey),
                                    contentAlignment = Alignment.Center) {
                                    val initials = friend.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("").uppercase()
                                    Text(initials.ifEmpty { "F" }, color = OffWhite, fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(friend.name, color = OffWhite, fontFamily = SpaceGroteskFamily,
                                        fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(friend.email, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                                }
                                Icon(Icons.Default.ChevronRight, "Chat", tint = MediumGrey)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (activeTab == "Groups") {
                    onNavigateToCreateGroup()
                } else {
                    showSearchDialog = true
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = LimeGreen,
            contentColor = MidnightBlack,
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (activeTab == "Groups") Icons.Default.Add else Icons.Default.PersonAdd,
                contentDescription = if (activeTab == "Groups") "Create Group" else "Add Friend"
            )
        }

        // User Search & Add Friend Dialog
        if (showSearchDialog) {
            UserSearchDialog(
                onDismiss = { showSearchDialog = false },
                viewModel = chatViewModel,
                friendsList = friends,
                blockedList = blockedUsers
            )
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MidnightBlack else CharcoalGrey)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) LimeGreen else MediumGrey,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchDialog(
    onDismiss: () -> Unit,
    viewModel: ChatViewModel,
    friendsList: List<com.bimarihaunter.network.FriendInfo>,
    blockedList: List<String>
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // Clear search results when dialog opens
    LaunchedEffect(Unit) {
        viewModel.searchUsers("")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalGrey),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Find Companions",
                        color = OffWhite,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = OffWhite)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Premium Dark Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchUsers(it)
                    },
                    placeholder = { Text("Search by name, email or UID...", color = MediumGrey, fontSize = 13.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite,
                        focusedContainerColor = MidnightBlack,
                        unfocusedContainerColor = MidnightBlack,
                        cursorColor = LimeGreen,
                        focusedIndicatorColor = LimeGreen,
                        unfocusedIndicatorColor = MediumGrey
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = LimeGreen,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search for a user by name prefix, email, or their Firebase UID.",
                            color = MediumGrey,
                            fontSize = 13.sp,
                            fontFamily = InterFamily,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (searchResults.isEmpty()) {
                        Text(
                            text = "No users found matching query.",
                            color = MediumGrey,
                            fontSize = 13.sp,
                            fontFamily = InterFamily,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults.filter { !blockedList.contains(it.uid) }) { user ->
                                val isFriend = friendsList.any { it.uid == user.uid }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MidnightBlack)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar Initials
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(CharcoalGrey),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(user.initials.ifEmpty { "U" }, color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(user.name, color = OffWhite, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(user.email, color = MediumGrey, fontSize = 11.sp, fontFamily = InterFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(Modifier.width(8.dp))

                                    if (isFriend) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Friends",
                                            tint = LimeGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        var isAdding by remember { mutableStateOf(false) }
                                        Button(
                                            onClick = {
                                                isAdding = true
                                                viewModel.addFriend(user.uid) {
                                                    isAdding = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = LimeGreen,
                                                contentColor = MidnightBlack
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp),
                                            enabled = !isAdding
                                        ) {
                                            if (isAdding) {
                                                CircularProgressIndicator(color = MidnightBlack, modifier = Modifier.size(16.dp))
                                            } else {
                                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
