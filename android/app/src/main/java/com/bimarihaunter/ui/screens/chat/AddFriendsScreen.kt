package com.bimarihaunter.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.data.model.User
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AddFriendsViewModel
import com.bimarihaunter.ui.viewmodel.FriendRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToChat: (userId: String, userName: String) -> Unit = { _, _ -> },
    viewModel: AddFriendsViewModel = viewModel()
) {
    var searchQuery      by remember { mutableStateOf("") }
    var selectedTab      by remember { mutableIntStateOf(0) }
    val searchResults    by viewModel.searchResults.collectAsState()
    val myFriends        by viewModel.myFriends.collectAsState()
    val pendingRequests  by viewModel.pendingRequests.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val snackMessage     by viewModel.snackMessage.collectAsState()

    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackState.showSnackbar(it)
            viewModel.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CharcoalGrey,
                    contentColor = OffWhite,
                    actionColor = LimeGreen
                )
            }
        },
        containerColor = MidnightBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightBlack)
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // ─── App Bar ─────────────────────────────────────────────────────
            BimarihaunterTopAppBar(
                title     = "Add Friends",
                showBackArrow = true,
                onBackClick   = onNavigateBack
            )

            // ─── Search Bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CharcoalGrey)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint   = MediumGrey,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                TextField(
                    value       = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchUsers(it)
                        if (it.isNotBlank()) selectedTab = 0
                    },
                    modifier    = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Search by name…",
                            color      = MediumGrey,
                            fontFamily = InterFamily,
                            fontSize   = 14.sp
                        )
                    },
                    singleLine = true,
                    colors     = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        cursorColor             = LimeGreen,
                        focusedTextColor        = OffWhite,
                        unfocusedTextColor      = OffWhite
                    )
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color    = LimeGreen,
                        strokeWidth = 2.dp
                    )
                }
            }

            // ─── Tabs ─────────────────────────────────────────────────────────
            val tabs = listOf(
                "Search",
                "Friends (${myFriends.size})",
                "Requests (${pendingRequests.size})"
            )
            TabRow(
                selectedTabIndex = selectedTab,
                modifier         = Modifier.fillMaxWidth(),
                containerColor   = MidnightBlack,
                contentColor     = LimeGreen,
                indicator        = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = LimeGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected  = selectedTab == index,
                        onClick   = { selectedTab = index },
                        text      = {
                            Text(
                                label,
                                fontFamily = InterFamily,
                                fontSize   = 13.sp,
                                color      = if (selectedTab == index) LimeGreen else MediumGrey
                            )
                        }
                    )
                }
            }

            // ─── Content ──────────────────────────────────────────────────────
            when (selectedTab) {
                0 -> SearchResultsContent(
                    results  = searchResults,
                    isEmpty  = searchQuery.length >= 2 && searchResults.isEmpty() && !isLoading,
                    onAdd    = { viewModel.sendFriendRequest(it) }
                )
                1 -> FriendsListContent(
                    friends   = myFriends,
                    onMessage = { user -> onNavigateToChat(user.uid, user.name) }
                )
                2 -> PendingRequestsContent(
                    requests = pendingRequests,
                    onAccept = { viewModel.acceptFriendRequest(it) },
                    onReject = { viewModel.rejectFriendRequest(it) }
                )
            }
        }
    }
}

// ─────────────────────────── Search Results ──────────────────────────────────

@Composable
private fun SearchResultsContent(
    results: List<User>,
    isEmpty: Boolean,
    onAdd: (User) -> Unit
) {
    if (isEmpty) {
        EmptyTabMessage("No users found. Try a different name.")
        return
    }
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(results, key = { it.uid }) { user ->
            UserListCard(
                user       = user,
                actionLabel= "Add",
                actionIcon = { Icon(Icons.Default.PersonAdd, null, tint = MidnightBlack, modifier = Modifier.size(16.dp)) },
                onAction   = { onAdd(user) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────── Friends List ────────────────────────────────────

@Composable
private fun FriendsListContent(
    friends: List<User>,
    onMessage: (User) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyTabMessage("You haven't added any friends yet.\nSearch for people above!")
        return
    }
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(friends, key = { it.uid }) { user ->
            UserListCard(
                user        = user,
                actionLabel = "Message",
                onAction    = { onMessage(user) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────── Pending Requests ────────────────────────────────

@Composable
private fun PendingRequestsContent(
    requests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onReject: (FriendRequest) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyTabMessage("No pending friend requests.")
        return
    }
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CharcoalGrey)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar initials
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LimeGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        request.fromName.take(1).uppercase(),
                        color      = LimeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        request.fromName,
                        color      = OffWhite,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        request.fromEmail,
                        color      = MediumGrey,
                        fontFamily = InterFamily,
                        fontSize   = 12.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Accept
                IconButton(
                    onClick  = { onAccept(request) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LimeGreen)
                ) {
                    Icon(Icons.Default.Check, "Accept", tint = MidnightBlack, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                // Reject
                IconButton(
                    onClick  = { onReject(request) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CharcoalGrey)
                ) {
                    Icon(Icons.Default.Close, "Reject", tint = MediumGrey, modifier = Modifier.size(18.dp))
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────── Shared Composables ──────────────────────────────

@Composable
private fun UserListCard(
    user: User,
    actionLabel: String,
    actionIcon: (@Composable () -> Unit)? = null,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CharcoalGrey)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar initials circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LimeGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            val initials = user.name.split(" ").take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
                .ifBlank { "?" }
            Text(initials, color = LimeGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                user.name,
                color      = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                user.email.ifBlank { user.phoneNumber },
                color      = MediumGrey,
                fontFamily = InterFamily,
                fontSize   = 12.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Button(
            onClick  = onAction,
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = LimeGreen),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            if (actionIcon != null) {
                actionIcon()
                Spacer(Modifier.width(4.dp))
            }
            Text(actionLabel, color = MidnightBlack, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyTabMessage(message: String) {
    Box(
        modifier          = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment  = Alignment.Center
    ) {
        Text(
            message,
            color      = MediumGrey,
            fontFamily = InterFamily,
            fontSize   = 14.sp,
            lineHeight = 22.sp,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
