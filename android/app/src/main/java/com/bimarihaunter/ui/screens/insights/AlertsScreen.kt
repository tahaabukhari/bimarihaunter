package com.bimarihaunter.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.components.*
import com.bimarihaunter.ui.theme.*

private val mockAlerts = listOf(
    AlertCardData("1", AlertSeverity.CRITICAL, "Dengue Outbreak — Lahore",
        "340+ cases reported in urban centers. Emergency response activated.",
        "12 min ago", false),
    AlertCardData("2", AlertSeverity.WARNING, "Flood Warning — Southern Sindh",
        "Heavy rainfall expected. Evacuation advisories issued for low-lying areas.",
        "1h ago", false),
    AlertCardData("3", AlertSeverity.INFO, "COVID-19 Booster Available",
        "Free booster doses now available at all government hospitals.",
        "3h ago", true),
    AlertCardData("4", AlertSeverity.CRITICAL, "Water Contamination — Peshawar",
        "Multiple areas report unsafe drinking water. Boil water advisory in effect.",
        "5h ago", false),
    AlertCardData("5", AlertSeverity.WARNING, "Heatwave Alert — Karachi",
        "Temperatures expected to exceed 45°C this week.",
        "8h ago", true),
    AlertCardData("6", AlertSeverity.INFO, "New Malaria Vaccine Trial",
        "WHO-backed clinical trials begin at major hospitals in Islamabad.",
        "12h ago", true),
)

@Composable
fun AlertsScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedChip by remember { mutableStateOf("All") }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack).navigationBarsPadding()) {
        BimarihaunterTopAppBar(
            title = "Alerts",
            showBackArrow = true,
            onBackClick = onNavigateBack,
            actions = {
                TextButton(onClick = {}) {
                    Text("Mark all read", color = LimeGreen, fontFamily = InterFamily,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        )

        FilterChipRow(
            chips = listOf("All", "Health", "Disaster", "Pharmacy"),
            selectedChip = selectedChip,
            onChipSelected = { selectedChip = it },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mockAlerts) { alert ->
                AlertCard(data = alert)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
