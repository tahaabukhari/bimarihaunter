package com.bimarihaunter.ui.insights

import com.bimarihaunter.data.models.InsightsData

data class InsightsUiState(
    val isLoading: Boolean = false,
    val region: String = "",
    val selectedCategoryFilter: String = "All", // "All", "Disease", "Disaster", "Pharmacy"
    val insightsData: InsightsData? = null,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false
)
