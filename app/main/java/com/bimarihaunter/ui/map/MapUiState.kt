package com.bimarihaunter.ui.map

import com.bimarihaunter.data.models.RegionData

data class MapUiState(
    val isLoading: Boolean = false,
    val regions: List<RegionData> = emptyList(),
    val selectedRegion: RegionData? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isSearchActive: Boolean = false,
    val isEmpty: Boolean = false
)
