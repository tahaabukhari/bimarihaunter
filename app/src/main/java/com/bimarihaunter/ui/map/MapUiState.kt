package com.bimarihaunter.ui.map

import com.bimarihaunter.data.models.OutbreakLocation
import com.bimarihaunter.data.models.SummaryStats

data class MapUiState(
    val isLoading: Boolean = false,
    val outbreakLocations: List<OutbreakLocation> = emptyList(),
    val summaryStats: SummaryStats? = null,
    val selectedLocation: OutbreakLocation? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isSearchActive: Boolean = false,
    val isEmpty: Boolean = false
)
