package com.bimarihaunter.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimarihaunter.ui.components.MapLegend
import com.bimarihaunter.ui.components.OutbreakMarker
import com.bimarihaunter.ui.components.RegionDetailBottomSheet
import com.bimarihaunter.ui.theme.AccentGreen
import com.bimarihaunter.ui.theme.DangerRed
import com.bimarihaunter.ui.theme.MidnightBlack
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
        position = CameraPosition.fromLatLngZoom(
            LatLng(30.3753, 69.3451), // Pakistan center
            5f
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState.isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search city, disease, severity...", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AccentGreen,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Outbreak Map", color = Color.White)
                    }
                },
                navigationIcon = {
                    if (uiState.isSearchActive) {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { /* Handle actual back navigation if applicable */ }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (uiState.isSearchActive) {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlack)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // The Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NORMAL),
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                uiState.regions.forEach { region ->
                    OutbreakMarker(region = region, onClick = { viewModel.selectRegion(it) })
                }
            }
            
            // Legend
            MapLegend(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart)
            )

            // States overlay
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AccentGreen
                )
            } else if (uiState.errorMessage != null) {
                ErrorCard(
                    message = uiState.errorMessage!!, 
                    onRetry = { viewModel.loadRegions() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.isEmpty) {
                Surface(
                    color = Color(0xFF1A1A1A).copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                ) {
                    Text(
                        "No matching region found.",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bottom Sheet
            if (uiState.selectedRegion != null) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.clearSelection() },
                    containerColor = Color(0xFF1A1A1A)
                ) {
                    RegionDetailBottomSheet(
                        region = uiState.selectedRegion!!,
                        onViewInsights = { 
                            viewModel.clearSelection()
                            onNavigateToInsights(it) 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF1A1A1A).copy(alpha = 0.9f),
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
                Text("Retry", color = Color.Black)
            }
        }
    }
}
