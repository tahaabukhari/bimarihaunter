package com.bimarihaunter.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimarihaunter.ui.components.*
import com.bimarihaunter.ui.map.ErrorCard
import com.bimarihaunter.ui.theme.AccentGreen
import com.bimarihaunter.ui.theme.CardBackground
import com.bimarihaunter.ui.theme.DangerRed
import com.bimarihaunter.ui.theme.MidnightBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val data = uiState.insightsData

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${uiState.region} Insights", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlack)
            )
        },
        containerColor = MidnightBlack
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AccentGreen
                )
            } else if (uiState.errorMessage != null) {
                ErrorCard(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadInsights() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (data != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    FilterChipsRow(uiState.selectedCategoryFilter, onFilterChange = { viewModel.setFilter(it) })
                    Spacer(Modifier.height(16.dp))

                    if (uiState.isEmpty) {
                        Surface(
                            color = CardBackground,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        ) {
                            Text(
                                "No insights available for ${uiState.selectedCategoryFilter}.",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatCard(modifier = Modifier.weight(1f), title = "Total Cases", value = "${data.totalCases}")
                            StatCard(modifier = Modifier.weight(1f), title = "Risk Level", value = data.severity, valueColor = DangerRed)
                        }
                        Spacer(Modifier.height(24.dp))

                        Text("Cases Over Time", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        CasesTrendChart(data.trendPoints)
                        Spacer(Modifier.height(24.dp))

                        Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        CategoryBreakdownChart(data.categoryBreakdown)
                        Spacer(Modifier.height(24.dp))

                        Text("Severity Distribution", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SeverityDonutChart(data.highCount, data.mediumCount, data.lowCount)
                        }
                        Spacer(Modifier.height(24.dp))

                        PharmacyCostTracker(data.pharmacyItems)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(selectedFilter: String, onFilterChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("All", "Disease", "Disaster", "Pharmacy").forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                    selectedLabelColor = AccentGreen,
                    labelColor = Color.LightGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = AccentGreen,
                    borderColor = Color.DarkGray,
                    selected = selectedFilter == filter,
                    enabled = true
                )
            )
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, valueColor: Color = Color.White) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        }
    }
}
