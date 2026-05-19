package com.bimarihaunter.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.components.FilterChipRow
import com.bimarihaunter.ui.theme.*

data class OutbreakSummary(
    val id: String,
    val disease: String,
    val location: String,
    val severity: String
)

private val mockOutbreaks = listOf(
    OutbreakSummary("1", "Dengue", "Lahore", "Critical"),
    OutbreakSummary("2", "Malaria", "Karachi", "High"),
    OutbreakSummary("3", "Cholera", "Hyderabad", "Medium"),
    OutbreakSummary("4", "COVID-19", "Islamabad", "Low"),
    OutbreakSummary("5", "Typhoid", "Peshawar", "High"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseMapScreen(
    onNavigateToDetail: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("Disease") }
    val sheetState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 140.dp,
        sheetContainerColor = CharcoalGrey,
        sheetContentColor = OffWhite,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(MediumGrey))
            }
        },
        sheetContent = {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Active Outbreaks", color = OffWhite, fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(mockOutbreaks) { outbreak ->
                        Column(
                            modifier = Modifier.width(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MidnightBlack)
                                .clickable { onNavigateToDetail(outbreak.id) }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(
                                    when (outbreak.severity) {
                                        "Critical" -> EmberRed
                                        "High" -> GoldWarning
                                        "Medium" -> TealInfo
                                        else -> MediumGrey
                                    }
                                ))
                                Spacer(Modifier.width(6.dp))
                                Text(outbreak.severity, color = MediumGrey, fontSize = 10.sp,
                                    fontFamily = InterFamily)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(outbreak.disease, color = OffWhite, fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(outbreak.location, color = MediumGrey, fontSize = 12.sp,
                                fontFamily = InterFamily)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        },
        containerColor = MidnightBlack
    ) {
        // Map area (placeholder since no API key)
        Box(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
            // Dark map background placeholder
            Box(Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Color(0xFF1A1A2E)
            ))

            // Simulated map markers
            val markers = listOf(
                Triple("Lahore", 0.35f, 0.28f),
                Triple("Karachi", 0.22f, 0.72f),
                Triple("Islamabad", 0.45f, 0.20f),
                Triple("Peshawar", 0.30f, 0.15f),
                Triple("Quetta", 0.12f, 0.40f),
            )
            markers.forEach { (name, x, y) ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (x * 300).dp,
                                y = (y * 500).dp
                            )
                    ) {
                        // Glowing dot
                        Box(Modifier.size(24.dp).clip(CircleShape)
                            .background(LimeGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(LimeGreen))
                        }
                    }
                }
            }

            // Floating search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CharcoalGrey.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = MediumGrey, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Search location...", color = MediumGrey, fontSize = 14.sp,
                    fontFamily = InterFamily)
            }

            // Filter chips below search
            Row(modifier = Modifier
                .padding(start = 20.dp, top = 80.dp)
                .align(Alignment.TopStart)) {
                FilterChipRow(
                    chips = listOf("Disease", "Disaster", "All"),
                    selectedChip = selectedFilter,
                    onChipSelected = { selectedFilter = it }
                )
            }

            // Location FAB
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 160.dp),
                containerColor = CharcoalGrey,
                contentColor = OffWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, "My location")
            }
        }
    }
}
