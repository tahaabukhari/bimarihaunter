package com.bimarihaunter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bimarihaunter.data.models.RegionData
import com.bimarihaunter.ui.theme.AccentGreen
import com.bimarihaunter.ui.theme.DangerRed
import com.bimarihaunter.ui.theme.WarningOrange
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMapScope
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

@Composable
fun GoogleMapScope.OutbreakMarker(region: RegionData, onClick: (RegionData) -> Unit) {
    val color = when (region.severity) {
        "HIGH" -> DangerRed
        "MEDIUM" -> WarningOrange
        else -> AccentGreen
    }
    
    // Safety bounds for radius
    val radius = (region.caseCount * 500.0).coerceIn(10000.0, 150000.0)
    
    Circle(
        center = LatLng(region.lat, region.lng),
        radius = radius,
        fillColor = color.copy(alpha = 0.35f),
        strokeColor = color,
        strokeWidth = 2f,
        clickable = true,
        onClick = { onClick(region) }
    )
    Marker(
        state = MarkerState(position = LatLng(region.lat, region.lng)),
        title = region.name,
        snippet = "${region.caseCount} cases · ${region.severity}",
        onClick = { onClick(region); false }
    )
}

@Composable
fun RegionDetailBottomSheet(region: RegionData, onViewInsights: (String) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(region.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(8.dp))
        SeverityBadge(severity = region.severity)
        Spacer(Modifier.height(16.dp))
        
        StatRow("Active Cases", region.caseCount.toString())
        StatRow("Top Category", region.topCategory)
        StatRow("Last Updated", region.lastUpdated)
        
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onViewInsights(region.name) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
        ) { 
            Text("View Full Insights →") 
        }
    }
}

@Composable
fun SeverityBadge(severity: String) {
    val color = when (severity) {
        "HIGH" -> DangerRed
        "MEDIUM" -> WarningOrange
        else -> AccentGreen
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = severity,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A1A).copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Legend:", color = Color.White, style = MaterialTheme.typography.labelMedium)
            LegendDot(AccentGreen, "Low")
            LegendDot(WarningOrange, "Med")
            LegendDot(DangerRed, "High")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.small, color = color) {}
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
    }
}
