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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.data.model.OutbreakPoint
import com.bimarihaunter.ui.components.FilterChipRow
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseMapScreen(
    onNavigateToDetail: (String) -> Unit = {},
    mapViewModel: MapViewModel = viewModel()
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val sheetState = rememberBottomSheetScaffoldState()
    
    val outbreaks by mapViewModel.outbreaks.collectAsState()
    
    val filteredOutbreaks = remember(outbreaks, selectedFilter) {
        if (selectedFilter == "All") {
            outbreaks
        } else {
            outbreaks.filter { it.name.contains(selectedFilter, ignoreCase = true) }
        }
    }

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
                    items(filteredOutbreaks) { outbreak ->
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
                                        "CRITICAL" -> EmberRed
                                        "HIGH" -> GoldWarning
                                        "MEDIUM" -> TealInfo
                                        else -> LimeGreen
                                    }
                                ))
                                Spacer(Modifier.width(6.dp))
                                Text(outbreak.severity, color = MediumGrey, fontSize = 10.sp,
                                    fontFamily = InterFamily)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(outbreak.name.substringBefore("—").trim(), color = OffWhite, fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(outbreak.name.substringAfter("—", "Pakistan").trim(), color = MediumGrey, fontSize = 12.sp,
                                fontFamily = InterFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

            // Simulated map markers based on real database entries
            filteredOutbreaks.forEachIndexed { index, outbreak ->
                val simulatedX = 0.15f + (index * 0.25f) % 0.7f
                val simulatedY = 0.2f + (index * 0.35f) % 0.6f
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (simulatedX * 300).dp,
                                y = (simulatedY * 500).dp
                            )
                            .clickable { onNavigateToDetail(outbreak.id) }
                    ) {
                        // Glowing dot
                        Box(Modifier.size(24.dp).clip(CircleShape)
                            .background(
                                when (outbreak.severity) {
                                    "CRITICAL" -> EmberRed.copy(alpha = 0.2f)
                                    "HIGH" -> GoldWarning.copy(alpha = 0.2f)
                                    else -> LimeGreen.copy(alpha = 0.2f)
                                },
                            ),
                            contentAlignment = Alignment.Center) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(
                                when (outbreak.severity) {
                                    "CRITICAL" -> EmberRed
                                    "HIGH" -> GoldWarning
                                    else -> LimeGreen
                                }
                            ))
                        }
                    }
                }
            }

            // Floating search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
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
                .statusBarsPadding()
                .padding(start = 20.dp, top = 64.dp)
                .align(Alignment.TopStart)) {
                FilterChipRow(
                    chips = listOf("All", "Dengue", "Cholera"),
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
