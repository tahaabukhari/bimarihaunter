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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.data.model.AlertData
import com.bimarihaunter.ui.components.*
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AlertsViewModel

@Composable
fun AlertsScreen(
    onNavigateBack: () -> Unit = {},
    alertsViewModel: AlertsViewModel = viewModel()
) {
    var selectedChip by remember { mutableStateOf("All") }
    val alertsList by alertsViewModel.alerts.collectAsState()

    val filteredAlerts = remember(alertsList, selectedChip) {
        if (selectedChip == "All") {
            alertsList
        } else {
            alertsList.filter { it.severity.equals(selectedChip, ignoreCase = true) }
        }
    }

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
            chips = listOf("All", "CRITICAL", "WARNING", "INFO"),
            selectedChip = selectedChip,
            onChipSelected = { selectedChip = it },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredAlerts) { alert ->
                AlertCard(
                    data = AlertCardData(
                        id = alert.id,
                        severity = when (alert.severity) {
                            "CRITICAL" -> AlertSeverity.CRITICAL
                            "WARNING" -> AlertSeverity.WARNING
                            else -> AlertSeverity.INFO
                        },
                        heading = alert.title,
                        subtitle = alert.description,
                        timestamp = alert.timestamp,
                        isRead = alert.read
                    )
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
