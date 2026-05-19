package com.bimarihaunter.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.theme.*

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("home_feed", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("disease_map", "Map", Icons.Filled.Map, Icons.Outlined.Map),
    BottomNavItem("chat_list", "Community", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    BottomNavItem("insights", "Insights", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MidnightBlack,
        contentColor = OffWhite,
        tonalElevation = 0.dp,
        // Let M3 handle system navigation bar insets automatically
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = { onItemSelected(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LimeGreen,
                    selectedTextColor = LimeGreen,
                    unselectedIconColor = MediumGrey,
                    unselectedTextColor = MediumGrey,
                    indicatorColor = LimeGreen.copy(alpha = 0.12f)
                )
            )
        }
    }
}
