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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    
    val displayName = currentUser?.displayName ?: "Bimari Haunter"
    val subtitleText = currentUser?.phoneNumber ?: currentUser?.email ?: "@anonymous"
    val initials = displayName.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("").uppercase()

    Column(
        modifier = Modifier.fillMaxSize().background(MidnightBlack)
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

        Column(modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))

            // Avatar
            Box(Modifier.size(80.dp).clip(CircleShape).background(CharcoalGrey),
                contentAlignment = Alignment.Center) {
                Text(initials.ifEmpty { "U" }, color = LimeGreen, fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }

            Spacer(Modifier.height(12.dp))

            Text(displayName, color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(subtitleText, color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Health tech enthusiast. Building for Pakistan 🇵🇰",
                color = MediumGrey, fontFamily = InterFamily, fontSize = 13.sp)

            Spacer(Modifier.height(16.dp))

            BimarihaunterButton("Edit Profile", onClick = {})

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("24", "Reports", modifier = Modifier.weight(1f))
                StatBox("156", "Upvotes", modifier = Modifier.weight(1f))
                StatBox("12", "Saved", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Saved Articles
            Text("Saved Articles", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(12.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listOf(
                Pair("Dengue Prevention Guide for Monsoon Season", "Dawn News"),
                Pair("Flood Relief Centers in Southern Punjab", "Geo News")
            )) { (title, source) ->
                Column(
                    Modifier.width(200.dp).clip(RoundedCornerShape(14.dp))
                        .background(CharcoalGrey).padding(14.dp)
                ) {
                    Text(title, color = OffWhite, fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(source, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))

            // My Groups
            Text("My Groups", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("Dengue Watch — Lahore", "DW", "24 members"),
                Triple("Flood Relief Coordination", "FR", "56 members")
            ).forEach { (name, initialsVal, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CharcoalGrey).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MidnightBlack),
                        contentAlignment = Alignment.Center) {
                        Text(initialsVal, color = LimeGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(name, color = OffWhite, fontSize = 14.sp, fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold)
                        Text(count, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
