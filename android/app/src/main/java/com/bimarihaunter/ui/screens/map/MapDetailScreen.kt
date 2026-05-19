package com.bimarihaunter.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        // Top 30% map placeholder
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f)
                .background(androidx.compose.ui.graphics.Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape)
                .background(LimeGreen.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(LimeGreen))
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

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, null, tint = LimeGreen, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Dengue Outbreak — Lahore", color = OffWhite,
                    fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    modifier = Modifier.weight(1f))
                Text("CRITICAL", color = OffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50.dp))
                        .background(EmberRed).padding(horizontal = 10.dp, vertical = 4.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Stat boxes
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox("1,240", "Confirmed Cases", modifier = Modifier.weight(1f))
                StatBox("18", "New Today", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = LimeGreen.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))

            // Affected Areas
            Text("Affected Areas", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("Gulberg", 0.85f, "High"),
                Triple("Model Town", 0.65f, "Medium"),
                Triple("DHA Phase 5", 0.45f, "Moderate")
            ).forEach { (area, severity, _) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = MediumGrey,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(area, color = OffWhite, fontSize = 14.sp, fontFamily = InterFamily,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(MediumGrey.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(severity)
                            .clip(RoundedCornerShape(3.dp)).background(LimeGreen))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Related News
            Text("Related News", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            listOf(
                "Punjab Govt Activates Emergency Response",
                "Fumigation Drives Begin Across Lahore"
            ).forEach { title ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MidnightBlack).padding(12.dp)) {
                    Column {
                        Text(title, color = OffWhite, fontSize = 13.sp,
                            fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Dawn News • 1h ago", color = MediumGrey, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            BimarihaunterButton("View Full Report", onClick = {})
        }
    }
}
