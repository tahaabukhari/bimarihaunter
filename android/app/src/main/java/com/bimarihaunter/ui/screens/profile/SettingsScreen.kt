package com.bimarihaunter.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var pushNotifications by remember { mutableStateOf(true) }
    var criticalAlerts by remember { mutableStateOf(true) }
    var locationAlerts by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(true) }
    var dataSharing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(MidnightBlack)
            .verticalScroll(rememberScrollState())
    ) {
        BimarihaunterTopAppBar(
            title = "Settings",
            showBackArrow = true,
            onBackClick = onNavigateBack
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Notifications
            SettingsSection("Notifications") {
                SettingsToggleRow(Icons.Default.Notifications, "Push Notifications",
                    pushNotifications) { pushNotifications = it }
                SettingsDivider()
                SettingsToggleRow(Icons.Default.Warning, "Critical Alerts",
                    criticalAlerts) { criticalAlerts = it }
                SettingsDivider()
                SettingsToggleRow(Icons.Default.LocationOn, "Location-based Alerts",
                    locationAlerts) { locationAlerts = it }
            }

            // Appearance
            SettingsSection("Appearance") {
                SettingsToggleRow(Icons.Default.DarkMode, "Dark Theme",
                    darkTheme) { darkTheme = it }
                SettingsDivider()
                SettingsChevronRow(Icons.Default.TextFields, "Text Size", "Medium")
            }

            // Privacy
            SettingsSection("Privacy") {
                SettingsChevronRow(Icons.Default.MyLocation, "Location Permissions", "Always On")
                SettingsDivider()
                SettingsToggleRow(Icons.Default.Share, "Data Sharing",
                    dataSharing) { dataSharing = it }
            }

            // Account
            SettingsSection("Account") {
                SettingsChevronRow(Icons.Default.Person, "Edit Profile", "")
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = EmberRed,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Log Out", color = EmberRed, fontFamily = InterFamily,
                        fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(CharcoalGrey).padding(horizontal = 16.dp)
        ) { content() }
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = OffWhite, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = OffWhite, fontFamily = InterFamily, fontSize = 15.sp,
            modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MidnightBlack,
                checkedTrackColor = LimeGreen,
                uncheckedThumbColor = MediumGrey,
                uncheckedTrackColor = CharcoalGrey
            )
        )
    }
}

@Composable
private fun SettingsChevronRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = OffWhite, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = OffWhite, fontFamily = InterFamily, fontSize = 15.sp,
            modifier = Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(value, color = MediumGrey, fontFamily = InterFamily, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = MediumGrey, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MidnightBlack.copy(alpha = 0.5f), thickness = 0.5.dp)
}
