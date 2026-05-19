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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.components.StatBox
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.MapViewModel

@Composable
fun MapDetailScreen(
    locationId: String? = null,
    onNavigateBack: () -> Unit = {},
    mapViewModel: MapViewModel = viewModel()
) {
    val outbreaks by mapViewModel.outbreaks.collectAsState()
    val outbreak = outbreaks.find { it.id == locationId }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack).navigationBarsPadding()) {
        // Top 30% map placeholder
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f)
                .background(androidx.compose.ui.graphics.Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape)
                .background(
                    when (outbreak?.severity) {
                        "CRITICAL" -> EmberRed.copy(alpha = 0.3f)
                        "HIGH" -> GoldWarning.copy(alpha = 0.3f)
                        else -> LimeGreen.copy(alpha = 0.3f)
                    }
                ),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(
                    when (outbreak?.severity) {
                        "CRITICAL" -> EmberRed
                        "HIGH" -> GoldWarning
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
                    Text(outbreak.name, color = OffWhite,
                        fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.weight(1f))
                    Text(outbreak.severity, color = OffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(50.dp))
                            .background(
                                when (outbreak.severity) {
                                    "CRITICAL" -> EmberRed
                                    "HIGH" -> GoldWarning
                                    else -> LimeGreen
                                }
                            ).padding(horizontal = 10.dp, vertical = 4.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Stat boxes
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBox(outbreak.cases, "Confirmed Cases", modifier = Modifier.weight(1f))
                    StatBox(outbreak.newToday, "New Today", modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = LimeGreen.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))

                // Affected Areas
                Text("Affected Areas", color = OffWhite, fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                outbreak.affectedAreas.forEach { (area, severity, riskLevel) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, tint = MediumGrey,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("$area ($riskLevel)", color = OffWhite, fontSize = 14.sp, fontFamily = InterFamily,
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
                if (outbreak.relatedNews.isNotEmpty()) {
                    Text("Related News", color = OffWhite, fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    outbreak.relatedNews.forEach { title ->
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
                }
            } else {
                Text("Outbreak details not found.", color = OffWhite, fontFamily = SpaceGroteskFamily)
            }

            Spacer(Modifier.height(16.dp))

            BimarihaunterButton("View Full Report", onClick = onNavigateBack)
        }
    }
}
