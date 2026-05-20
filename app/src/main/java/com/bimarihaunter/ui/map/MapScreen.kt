package com.bimarihaunter.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimarihaunter.data.models.OutbreakLocation
import com.bimarihaunter.ui.theme.AccentGreen
import com.bimarihaunter.ui.theme.CardBackground
import com.bimarihaunter.ui.theme.DangerRed
import com.bimarihaunter.ui.theme.MidnightBlack
import com.bimarihaunter.ui.theme.WarningOrange
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onNavigateToInsights: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(30.3753, 69.3451), 5f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Disease Outbreak Map", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Real-time tracking across Pakistan", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Back if needed */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLocations(isRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlack)
            )
        },
        containerColor = MidnightBlack
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Map Section
                Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(mapType = MapType.NORMAL),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        uiState.outbreakLocations.forEach { loc ->
                            val colorHue = when (loc.disease.lowercase()) {
                                "dengue" -> BitmapDescriptorFactory.HUE_RED
                                "cholera" -> BitmapDescriptorFactory.HUE_CYAN
                                "malaria" -> BitmapDescriptorFactory.HUE_ORANGE
                                else -> BitmapDescriptorFactory.HUE_GREEN
                            }
                            Marker(
                                state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                                title = "${loc.city} - ${loc.disease}",
                                snippet = "Cases: ${loc.count} | Severity: ${loc.severity}",
                                icon = BitmapDescriptorFactory.defaultMarker(colorHue),
                                onClick = { 
                                    viewModel.selectLocation(loc)
                                    true 
                                }
                            )
                        }
                    }

                    // Summary Stats Overlay
                    uiState.summaryStats?.let { stats ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatOverlayCard(modifier = Modifier.fillMaxWidth(), title = "Total Cases", value = stats.totalCases.toString())
                                StatOverlayCard(modifier = Modifier.fillMaxWidth(), title = "Weekly Trend", value = "+${stats.weeklyIncrease}%", color = WarningOrange)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatOverlayCard(modifier = Modifier.fillMaxWidth(), title = "Critical Areas", value = stats.criticalAreas.toString(), color = DangerRed)
                                StatOverlayCard(modifier = Modifier.fillMaxWidth(), title = "Top Disease", value = stats.topDisease)
                            }
                        }
                    }
                }

                // Top Affected Areas Section
                Column(modifier = Modifier.weight(0.4f).fillMaxWidth().background(MidnightBlack).padding(16.dp)) {
                    Text("Top Affected Areas", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    
                    val topAreas = uiState.outbreakLocations.sortedByDescending { it.count }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(topAreas) { area ->
                            AffectedAreaItem(
                                location = area, 
                                onClick = { onNavigateToInsights(area.city) }
                            )
                        }
                    }
                }
            }

            // States overlay
            AnimatedVisibility(
                visible = uiState.isLoading || uiState.isRefreshing,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator(color = AccentGreen)
            }

            if (uiState.errorMessage != null) {
                ErrorCard(
                    message = uiState.errorMessage!!, 
                    onRetry = { viewModel.loadLocations() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.isEmpty && !uiState.isLoading) {
                Surface(
                    color = CardBackground,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                ) {
                    Text(
                        "No outbreak data available.",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Selected Location Details Bottom Sheet
            if (uiState.selectedLocation != null) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.clearSelection() },
                    containerColor = CardBackground
                ) {
                    SelectedLocationDetail(
                        location = uiState.selectedLocation!!,
                        onViewInsights = { 
                            val city = uiState.selectedLocation!!.city
                            viewModel.clearSelection()
                            onNavigateToInsights(city) 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatOverlayCard(modifier: Modifier = Modifier, title: String, value: String, color: Color = Color.White) {
    Surface(
        modifier = modifier,
        color = MidnightBlack.copy(alpha = 0.8f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AffectedAreaItem(location: OutbreakLocation, onClick: () -> Unit) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(location.city, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(location.disease, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${location.count} Cases", style = MaterialTheme.typography.bodyMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
                val severityColor = when (location.severity.lowercase()) {
                    "high" -> DangerRed
                    "medium" -> WarningOrange
                    else -> AccentGreen
                }
                Text(location.severity.uppercase(), style = MaterialTheme.typography.labelSmall, color = severityColor)
            }
        }
    }
}

@Composable
fun SelectedLocationDetail(location: OutbreakLocation, onViewInsights: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(location.city, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Disease: ${location.disease}", color = Color.LightGray)
        Text("Cases: ${location.count}", color = Color.LightGray)
        Text("Severity: ${location.severity.uppercase()}", color = Color.LightGray)
        Text("Reported: ${location.timestamp}", color = Color.LightGray)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onViewInsights,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Detailed Insights", color = MidnightBlack, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = CardBackground,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.padding(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, color = DangerRed)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry, 
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Retry", color = MidnightBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}
