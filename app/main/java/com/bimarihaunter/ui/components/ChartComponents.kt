package com.bimarihaunter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bimarihaunter.data.models.PharmacyItem
import com.bimarihaunter.ui.theme.AccentGreen
import com.bimarihaunter.ui.theme.CardBackground
import com.bimarihaunter.ui.theme.DangerRed
import com.bimarihaunter.ui.theme.WarningOrange
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.component.shape.shader.color
import com.patrykandpatrick.vico.core.chart.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.model.columnSeries
import com.patrykandpatrick.vico.core.model.lineSeries

@Composable
fun CasesTrendChart(dataPoints: List<Pair<String, Float>>) {
    if (dataPoints.isEmpty()) {
        Text("No trend data available", color = Color.Gray, modifier = Modifier.padding(16.dp))
        return
    }
    
    val modelProducer = remember { CartesianChartModelProducer.build() }
    LaunchedEffect(dataPoints) {
        modelProducer.tryRunTransaction {
            lineSeries { series(dataPoints.map { it.second }) }
        }
    }
    Surface(color = Color.Transparent, shape = MaterialTheme.shapes.medium) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(
                                com.patrykandpatrick.vico.compose.component.shape.shader.color(AccentGreen)
                            )
                        )
                    )
                ),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis()
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(8.dp)
        )
    }
}

@Composable
fun CategoryBreakdownChart(categories: Map<String, Int>) {
    if (categories.isEmpty()) {
        Text("No category data available", color = Color.Gray, modifier = Modifier.padding(16.dp))
        return
    }

    val modelProducer = remember { CartesianChartModelProducer.build() }
    LaunchedEffect(categories) {
        modelProducer.tryRunTransaction {
            columnSeries { series(categories.values.map { it.toFloat() }) }
        }
    }
    Surface(color = Color.Transparent, shape = MaterialTheme.shapes.medium) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis()
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(8.dp)
        )
    }
}

@Composable
fun SeverityDonutChart(high: Float, medium: Float, low: Float) {
    val total = high + medium + low
    if (total == 0f) {
        Text("No severity data", color = Color.Gray, modifier = Modifier.padding(16.dp))
        return
    }

    val sweepHigh   = (high / total) * 360f
    val sweepMedium = (medium / total) * 360f
    val sweepLow    = (low / total) * 360f

    Surface(color = Color.Transparent, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                var startAngle = -90f
                if (sweepHigh > 0) drawArc(DangerRed, startAngle, sweepHigh, false, style = Stroke(28.dp.toPx(), cap = StrokeCap.Round))
                startAngle += sweepHigh
                if (sweepMedium > 0) drawArc(WarningOrange, startAngle, sweepMedium, false, style = Stroke(28.dp.toPx(), cap = StrokeCap.Round))
                startAngle += sweepMedium
                if (sweepLow > 0) drawArc(AccentGreen, startAngle, sweepLow, false, style = Stroke(28.dp.toPx(), cap = StrokeCap.Round))
            }
            
            // Draw labels
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (sweepHigh > 0) Text("HIGH", color = DangerRed, style = MaterialTheme.typography.labelSmall)
                if (sweepMedium > 0) Text("MED", color = WarningOrange, style = MaterialTheme.typography.labelSmall)
                if (sweepLow > 0) Text("LOW", color = AccentGreen, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun PharmacyCostTracker(items: List<PharmacyItem>) {
    Surface(color = CardBackground, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("💊 Pharmacy Costs", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(16.dp))
            
            if (items.isEmpty()) {
                Text("No pharmacy data available.", color = Color.Gray)
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isUp = item.changePercent > 0
                            Icon(
                                imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isUp) DangerRed else AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Rs. ${item.price} (${if (isUp) "+" else ""}${item.changePercent}%)",
                                color = if (isUp) DangerRed else AccentGreen
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            }
        }
    }
}
