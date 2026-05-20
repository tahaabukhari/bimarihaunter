package com.bimarihaunter.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.components.FilterChipRow
import com.bimarihaunter.ui.theme.*

@Composable
fun SearchFilterScreen(
    onNavigateBack: () -> Unit = {},
    onApplyFilters: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf("") }
    val categories = listOf("Dengue", "Malaria", "Cholera", "COVID-19", "Flood", "Earthquake",
        "Heatwave", "Polio")
    val selectedCategories = remember { mutableStateListOf<String>() }
    val recentSearches = listOf("Dengue Lahore", "Flood Sindh", "COVID vaccine")

    Column(
        modifier = Modifier.fillMaxSize().background(MidnightBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Search...", color = MediumGrey) },
            leadingIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OffWhite)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
            ), singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        // Recent Searches
        Text("Recent Searches", color = OffWhite, fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recentSearches.forEach { search ->
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(50.dp))
                        .background(CharcoalGrey).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(search, color = OffWhite, fontSize = 12.sp, fontFamily = InterFamily)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Close, "Remove", tint = MediumGrey, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Filter by Category
        Text("Filter by Category", color = OffWhite, fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(180.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat in selectedCategories
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) LimeGreen.copy(0.15f) else CharcoalGrey)
                        .clickable {
                            if (isSelected) selectedCategories.remove(cat)
                            else selectedCategories.add(cat)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat, color = if (isSelected) LimeGreen else OffWhite, fontSize = 13.sp,
                        fontFamily = InterFamily, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Default.Check, null, tint = LimeGreen,
                        modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Filter by Severity
        Text("Filter by Severity", color = OffWhite, fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        FilterChipRow(
            chips = listOf("Critical", "High", "Medium", "Low"),
            selectedChip = selectedSeverity,
            onChipSelected = { selectedSeverity = if (selectedSeverity == it) "" else it }
        )

        Spacer(Modifier.weight(1f))

        // Buttons
        BimarihaunterButton(text = "Apply Filters", onClick = onApplyFilters)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = {
            selectedCategories.clear(); selectedSeverity = ""; searchQuery = ""
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Clear All", color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp)
        }
    }
}
