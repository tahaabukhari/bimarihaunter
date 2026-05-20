package com.bimarihaunter.ui.insights

import com.bimarihaunter.data.models.InsightReport

data class InsightsUiState(
    val isLoading: Boolean = false,
    val region: String = "",
    val insightReport: InsightReport? = null,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false
)
