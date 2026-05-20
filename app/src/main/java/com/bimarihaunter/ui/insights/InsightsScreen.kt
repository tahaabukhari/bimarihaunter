package com.bimarihaunter.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.bimarihaunter.data.models.InsightReport
import com.bimarihaunter.ui.theme.AccentGreen
import com.bimarihaunter.ui.theme.CardBackground
import com.bimarihaunter.ui.theme.DangerRed
import com.bimarihaunter.ui.theme.MidnightBlack
import com.bimarihaunter.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.insightReport

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Insights Dashboard", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("AI-powered regional health insights", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadInsights(isRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlack)
            )
        },
        containerColor = MidnightBlack
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            if (report != null && !uiState.isEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Region: ${uiState.region}", style = MaterialTheme.typography.titleLarge, color = AccentGreen, fontWeight = FontWeight.Bold)
                    
                    // Key Metrics
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(modifier = Modifier.weight(1f), title = "Total Cases", value = report.totalCases.toString())
                            MetricCard(modifier = Modifier.weight(1f), title = "Weekly Growth", value = report.weeklyGrowth, valueColor = DangerRed)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(modifier = Modifier.weight(1f), title = "Critical Regions", value = report.criticalRegions.toString(), valueColor = WarningOrange)
                            
                            val topDisease = report.diseaseBreakdown.maxByOrNull { it.value }?.key ?: "Unknown"
                            MetricCard(modifier = Modifier.weight(1f), title = "Top Disease", value = topDisease)
                        }
                    }

                    // Risk Level
                    RiskLevelCard(riskLevel = report.riskLevel)

                    // Disease Breakdown
                    DiseaseBreakdownCard(breakdown = report.diseaseBreakdown, totalCases = report.totalCases)

                    // Top Affected Areas / Hotspots
                    SectionCard(title = "Top Affected Areas (Hotspots)") {
                        report.hotspots.forEachIndexed { index, hotspot ->
                            Text("${index + 1}. $hotspot", color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    // Trend Analysis
                    SectionCard(title = "Trend Analysis") {
                        Text(report.trends, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                    }

                    // Recommendations
                    SectionCard(title = "Recommendations") {
                        report.recommendations.forEach { recommendation ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Text("✓", color = AccentGreen, modifier = Modifier.padding(right = 8.dp))
                                Text(recommendation, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // 7-Day Prediction
                    SectionCard(title = "7-Day Prediction") {
                        Text(report.prediction7Days, color = WarningOrange, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp)) // bottom padding
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
                ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadInsights() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.isEmpty && !uiState.isLoading) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier = Modifier, title: String, value: String, valueColor: Color = Color.White) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RiskLevelCard(riskLevel: String) {
    val (color, explanation) = when (riskLevel.lowercase()) {
        "high" -> Pair(DangerRed, "Critical situation requires immediate intervention.")
        "medium" -> Pair(WarningOrange, "Elevated risk. Monitor closely and prepare resources.")
        else -> Pair(AccentGreen, "Risk is currently managed. Continue standard procedures.")
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(riskLevel.uppercase().take(1), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Risk Level: ${riskLevel.uppercase()}", color = color, fontWeight = FontWeight.Bold)
                Text(explanation, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DiseaseBreakdownCard(breakdown: Map<String, Int>, totalCases: Int) {
    SectionCard(title = "Disease Breakdown") {
        if (totalCases == 0 || breakdown.isEmpty()) {
            Text("No breakdown available.", color = Color.Gray)
        } else {
            breakdown.forEach { (disease, count) ->
                val percent = (count.toFloat() / totalCases.toFloat()) * 100
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(disease, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text("$count (${String.format("%.1f", percent)}%)", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = percent / 100f,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = AccentGreen,
                        trackColor = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, color = DangerRed, fontWeight = FontWeight.Bold)
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

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(32.dp)
    ) {
        Text(
            "No insights available for this region.",
            color = Color.LightGray,
            modifier = Modifier.padding(24.dp)
        )
    }
}
