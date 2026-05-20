package com.bimarihaunter.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.components.StatBox
import com.bimarihaunter.ui.theme.*

@Composable
fun MapDetailScreen(
    locationId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = com.bimarihaunter.db.BimarihaunterDatabase.getDatabase(context)
    val repository = com.bimarihaunter.repository.FeedRepository(database)
    val factory = com.bimarihaunter.ui.viewmodel.MapViewModelFactory(repository)
    val mapViewModel: com.bimarihaunter.ui.viewmodel.MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

    val markers by mapViewModel.mapMarkers.collectAsState()
    val outbreak = markers.find { it.id == locationId }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack).navigationBarsPadding()) {
        // Top 30% map placeholder
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f)
                .background(androidx.compose.ui.graphics.Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape)
                .background(
                    when (outbreak?.severity?.lowercase()) {
                        "high" -> EmberRed.copy(alpha = 0.3f)
                        "medium" -> GoldWarning.copy(alpha = 0.3f)
                        else -> LimeGreen.copy(alpha = 0.3f)
                    }
                ),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(
                    when (outbreak?.severity?.lowercase()) {
                        "high" -> EmberRed
                        "medium" -> GoldWarning
                        else -> LimeGreen
                    }
                ))
            }
        }

        // Bottom sheet content
        Column(
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CharcoalGrey)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Drag handle
            Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MediumGrey).align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(16.dp))

            if (outbreak != null) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = LimeGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(outbreak.title, color = OffWhite,
                        fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.weight(1f))
                    Text(outbreak.severity.uppercase(), color = OffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(50.dp))
                            .background(
                                when (outbreak.severity.lowercase()) {
                                    "high" -> EmberRed
                                    "medium" -> GoldWarning
                                    else -> LimeGreen
                                }
                            ).padding(horizontal = 10.dp, vertical = 4.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Stat boxes
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBox(outbreak.disease, "Disease", modifier = Modifier.weight(1f))
                    StatBox("Lat: %.2f, Lon: %.2f".format(outbreak.latitude, outbreak.longitude), "Coordinates", modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = LimeGreen.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))

                // Outbreak Summary
                Text("Outbreak Summary", color = OffWhite, fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = outbreak.summary,
                    color = OffWhite,
                    fontSize = 14.sp,
                    fontFamily = InterFamily
                )
            } else {
                Text("Outbreak details not found.", color = OffWhite, fontFamily = SpaceGroteskFamily)
            }

            Spacer(Modifier.height(32.dp))

            BimarihaunterButton("View Full Report", onClick = onNavigateBack)
        }
    }
}
