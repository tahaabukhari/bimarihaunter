package com.bimarihaunter.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.bimarihaunter.ui.viewmodel.ChatViewModel
import androidx.compose.ui.text.style.TextAlign
import com.bimarihaunter.db.OutbreakReportEntity
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel
) {
    val rawFeed by viewModel.feed.collectAsState(initial = emptyList())
    val syncStatus by viewModel.syncStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var diseaseFilter by remember { mutableStateOf<String?>(null) }
    var severityFilter by remember { mutableStateOf<String?>(null) }
    var activeSharePost by remember { mutableStateOf<OutbreakReportEntity?>(null) }

    // Client-side search and filter execution over local cache flow
    val filteredFeed = remember(rawFeed, searchQuery, diseaseFilter, severityFilter) {
        var list = rawFeed
        if (searchQuery.isNotEmpty()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.raw_text.contains(searchQuery, ignoreCase = true) ||
                it.summary.joinToString(" ").contains(searchQuery, ignoreCase = true)
            }
        }
        if (diseaseFilter != null) {
            list = list.filter { it.disease.equals(diseaseFilter, ignoreCase = true) }
        }
        if (severityFilter != null) {
            list = list.filter { it.severity.equals(severityFilter, ignoreCase = true) }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .statusBarsPadding()
    ) {
        // App Branding Header with Sync Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LimeGreen)) { append("bimari") }
                    withStyle(SpanStyle(color = OffWhite)) { append("haunter") }
                },
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "• Feed",
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Sync status icon/indicator
            when (syncStatus) {
                is FeedViewModel.SyncStatus.Loading -> {
                    CircularProgressIndicator(
                        color = LimeGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is FeedViewModel.SyncStatus.Error -> {
                    IconButton(onClick = { viewModel.refreshFeed() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry Sync",
                            tint = EmberRed
                        )
                    }
                }
                else -> {
                    val refreshRotation by animateFloatAsState(
                        targetValue = if (syncStatus is FeedViewModel.SyncStatus.Loading) 360f else 0f,
                        animationSpec = tween(durationMillis = 600)
                    )
                    IconButton(onClick = { viewModel.refreshFeed() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Feed",
                            tint = MediumGrey,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = refreshRotation
                            }
                        )
                    }
                }
            }
        }

        // Search Bar Block
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filters Block
        FilterChips(
            diseaseFilter = diseaseFilter,
            severityFilter = severityFilter,
            onDiseaseFilterChange = { diseaseFilter = it },
            onSeverityFilterChange = { severityFilter = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic State Content based on filtered results
        AnimatedContent(
            targetState = filteredFeed,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "feed_list_transition"
        ) { feedList ->
            if (feedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(
                                if (rawFeed.isEmpty() && syncStatus is FeedViewModel.SyncStatus.Loading)
                                    com.bimarihaunter.R.drawable.ghost_sleep
                                else com.bimarihaunter.R.drawable.ghost_sad
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (rawFeed.isEmpty() && syncStatus is FeedViewModel.SyncStatus.Loading) 
                                "Syncing latest updates..." else "No reports found",
                            color = OffWhite,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (rawFeed.isEmpty() && syncStatus is FeedViewModel.SyncStatus.Loading)
                                "Fetching updates from Firestore..." else "Check your network connection or filters.",
                            color = MediumGrey,
                            fontFamily = InterFamily,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(feedList, key = { it.id }) { report ->
                        ReportCard(
                            report = report,
                            onShareClick = { activeSharePost = report }
                        )
                    }
                }
            }
        }

        if (activeSharePost != null) {
            QuickShareDialog(
                onDismiss = { activeSharePost = null },
                report = activeSharePost!!
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search outbreaks, symptoms, locations...",
                    fontFamily = InterFamily,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MediumGrey,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = OffWhite
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CharcoalGrey,
                unfocusedContainerColor = CharcoalGrey,
                focusedBorderColor = LimeGreen.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = OffWhite,
                unfocusedTextColor = OffWhite,
                focusedPlaceholderColor = MediumGrey,
                unfocusedPlaceholderColor = MediumGrey
            ),
            singleLine = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    diseaseFilter: String?,
    severityFilter: String?,
    onDiseaseFilterChange: (String?) -> Unit,
    onSeverityFilterChange: (String?) -> Unit
) {
    val diseases = listOf("Dengue", "Cholera", "Typhoid", "COVID-19")
    val severities = listOf("High", "Medium", "Low")
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Disease Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            diseases.forEach { disease ->
                val isSelected = diseaseFilter == disease
                val selectedBgColor by animateColorAsState(
                    targetValue = if (isSelected) LimeGreen else CharcoalGrey,
                    animationSpec = tween(durationMillis = 250)
                )
                val selectedLabelColor by animateColorAsState(
                    targetValue = if (isSelected) MidnightBlack else OffWhite,
                    animationSpec = tween(durationMillis = 250)
                )
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onDiseaseFilterChange(if (isSelected) null else disease)
                    },
                    label = {
                        Text(
                            disease,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = selectedLabelColor
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedBgColor,
                        containerColor = CharcoalGrey,
                        labelColor = OffWhite,
                        selectedLabelColor = selectedLabelColor
                    ),
                    border = null
                )
            }
        }
        
        // Severity Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            severities.forEach { severity ->
                val isSelected = severityFilter == severity
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSeverityFilterChange(if (isSelected) null else severity)
                    },
                    label = {
                        Text(
                            severity,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) MidnightBlack else OffWhite
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LimeGreen,
                        containerColor = CharcoalGrey,
                        labelColor = OffWhite,
                        selectedLabelColor = MidnightBlack
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
fun ReportCard(
    report: OutbreakReportEntity,
    onShareClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = LimeGreen),
                onClick = {}
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalGrey)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Info: Source & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.source,
                    color = LimeGreen,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                
                // Formatted/Readable date extraction
                val displayDate = report.published_at.take(10)
                Text(
                    text = displayDate,
                    color = MediumGrey,
                    fontFamily = InterFamily,
                    fontSize = 11.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = report.title,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OffWhite,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Summary text
            Text(
                text = report.summary.joinToString(separator = " ").ifEmpty { report.raw_text },
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = OffWhite.copy(alpha = 0.7f),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Badges & location metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Badge(
                        text = report.disease.replaceFirstChar { it.uppercase() },
                        backgroundColor = LimeGreen.copy(alpha = 0.2f),
                        textColor = LimeGreen
                    )
                    
                    val (backColor, textColor) = when (report.severity.lowercase()) {
                        "high" -> Pair(EmberRed.copy(alpha = 0.2f), EmberRed)
                        "medium" -> Pair(Color(0xFFEAB308).copy(alpha = 0.2f), Color(0xFFEAB308))
                        "low" -> Pair(Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF22C55E))
                        else -> Pair(MediumGrey.copy(alpha = 0.2f), MediumGrey)
                    }
                    Badge(
                        text = report.severity.replaceFirstChar { it.uppercase() },
                        backgroundColor = backColor,
                        textColor = textColor
                    )
                    
                    // Show first location name if available
                    val locationLabel = report.locations.firstOrNull()?.trim()?.ifBlank { null }
                    if (locationLabel != null) {
                        Text(
                            text = "📍 $locationLabel",
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            color = MediumGrey,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = LimeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Badge(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

@Composable
fun QuickShareDialog(
    onDismiss: () -> Unit,
    report: OutbreakReportEntity,
    chatViewModel: ChatViewModel = viewModel()
) {
    val friends by chatViewModel.friends.collectAsState()
    val blockedUsers by chatViewModel.blockedUsers.collectAsState()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        chatViewModel.loadFriends()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalGrey),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
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
                        "Share Outbreak",
                        color = OffWhite,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = OffWhite)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = report.title,
                    color = LimeGreen,
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(16.dp))

                val unblockedFriends = friends.filter { !blockedUsers.contains(it.uid) }

                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    if (unblockedFriends.isEmpty()) {
                        Text(
                            "Add friends in the Community tab to share outbreak updates!",
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
                            items(unblockedFriends) { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MidnightBlack)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(CharcoalGrey),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = friend.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("").uppercase()
                                        Text(initials, color = OffWhite, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(friend.name, color = OffWhite, fontFamily = SpaceGroteskFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(friend.email, color = MediumGrey, fontSize = 11.sp, fontFamily = InterFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    
                                    var isSent by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = {
                                            isSent = true
                                            val chatId = if (currentUid < friend.uid) "${currentUid}_${friend.uid}" else "${friend.uid}_${currentUid}"
                                            chatViewModel.sendDirectMessage(
                                                chatId = chatId,
                                                recipientId = friend.uid,
                                                recipientName = friend.name,
                                                text = "Shared an outbreak report: ${report.title}",
                                                sharedPostId = report.id,
                                                sharedPostTitle = report.title,
                                                sharedPostDisease = report.disease,
                                                sharedPostSeverity = report.severity,
                                                sharedPostUrl = report.url
                                            )
                                            Toast.makeText(context, "Report shared with ${friend.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSent) CharcoalGrey else LimeGreen,
                                            contentColor = if (isSent) LimeGreen else MidnightBlack
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        enabled = !isSent
                                    ) {
                                        Text(if (isSent) "Sent" else "Send", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
