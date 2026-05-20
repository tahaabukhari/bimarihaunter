package com.bimarihaunter.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.components.StatBox
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    // Dynamic display values from Firebase Auth + Firestore profile
    val displayName = userProfile?.name?.ifBlank { null }
        ?: currentUser?.displayName?.ifBlank { null }
        ?: "Bimari Haunter"
    val subtitleText = userProfile?.email?.ifBlank { null }
        ?: currentUser?.email?.ifBlank { null }
        ?: currentUser?.phoneNumber?.ifBlank { null }
        ?: "@anonymous"
    val initials = displayName.split(" ").take(2)
        .mapNotNull { it.firstOrNull() }.joinToString("").uppercase()

    // Dynamic stats from Firestore
    var reportsCount by remember { mutableStateOf<Int?>(null) }
    var savedCount by remember { mutableStateOf<Int?>(null) }
    var groupsCount by remember { mutableStateOf<Int?>(null) }

    // Dynamic saved articles from Firestore
    var savedArticles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    // Dynamic groups from Firestore
    var userGroups by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }

    val uid = currentUser?.uid

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()

        // Count reports submitted by this user
        try {
            val reportsSnap = db.collection("reports")
                .whereEqualTo("user_id", uid)
                .get().await()
            reportsCount = reportsSnap.size()
        } catch (_: Exception) { reportsCount = 0 }

        // Count saved articles
        try {
            val savedSnap = db.collection("users").document(uid)
                .collection("saved").get().await()
            savedCount = savedSnap.size()

            // Load actual saved article titles/sources
            savedArticles = savedSnap.documents.take(6).map { doc ->
                val title = doc.getString("title") ?: "Saved Article"
                val source = doc.getString("source") ?: "BimariHaunter"
                Pair(title, source)
            }
        } catch (_: Exception) { savedCount = 0 }

        // Count and load groups
        try {
            val groupsSnap = db.collection("groups")
                .whereArrayContains("members", uid)
                .get().await()
            groupsCount = groupsSnap.size()

            userGroups = groupsSnap.documents.take(5).map { doc ->
                val name = doc.getString("name") ?: "Group"
                val memberCount = (doc.get("members") as? List<*>)?.size ?: 0
                val ini = name.split(" ").take(2)
                    .mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
                    .ifEmpty { "G" }
                Triple(name, ini, "$memberCount member${if (memberCount != 1) "s" else ""}")
            }
        } catch (_: Exception) { groupsCount = 0 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .verticalScroll(rememberScrollState())
    ) {
        BimarihaunterTopAppBar(
            title = "Profile",
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, "Settings", tint = OffWhite)
                }
            }
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Avatar — initials from real display name
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(CharcoalGrey),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials.ifEmpty { "U" },
                    color = LimeGreen,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                displayName,
                color = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(subtitleText, color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp)

            Spacer(Modifier.height(16.dp))

            BimarihaunterButton("Edit Profile", onClick = {})

            Spacer(Modifier.height(20.dp))

            // Dynamic stats row — shows loading skeleton until data arrives
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    value = reportsCount?.toString() ?: "—",
                    label = "Reports",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = savedCount?.toString() ?: "—",
                    label = "Saved",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = groupsCount?.toString() ?: "—",
                    label = "Groups",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Saved Articles header
            Text(
                "Saved Articles",
                color = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(12.dp))
        }

        // Saved Articles — dynamic from Firestore, empty state if none
        if (savedArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CharcoalGrey)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (savedCount == null) "Loading saved articles..." else "No saved articles yet.",
                    color = MediumGrey,
                    fontFamily = InterFamily,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedArticles) { (title, source) ->
                    Column(
                        Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CharcoalGrey)
                            .padding(14.dp)
                    ) {
                        Text(
                            title,
                            color = OffWhite,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(source, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))

            Text(
                "My Groups",
                color = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(12.dp))

            if (userGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalGrey)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (groupsCount == null) "Loading groups..." else "You haven't joined any groups yet.",
                        color = MediumGrey,
                        fontFamily = InterFamily,
                        fontSize = 14.sp
                    )
                }
            } else {
                userGroups.forEach { (name, ini, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CharcoalGrey)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MidnightBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                ini,
                                color = LimeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                name,
                                color = OffWhite,
                                fontSize = 14.sp,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(count, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
