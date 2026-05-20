package com.bimarihaunter.ui.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.data.api.FirebaseReportsDataSource
import com.bimarihaunter.data.api.InsightsService
import com.bimarihaunter.data.models.ReportData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Safely get region name. Use "Pakistan" or "All Regions" as fallback if null
    private val regionName: String = savedStateHandle["region"] ?: "All Regions"

    private val firebaseDataSource = FirebaseReportsDataSource()
    private val insightsService = InsightsService()

    private val _uiState = MutableStateFlow(InsightsUiState(region = regionName))
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    fun loadInsights(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            
            try {
                // Fetch reports from Firebase fallback
                val locations = firebaseDataSource.getReportsStream().firstOrNull() ?: emptyList()
                
                // Filter by region if a specific region was passed
                val filteredLocations = if (regionName != "All Regions") {
                    locations.filter { it.city.equals(regionName, ignoreCase = true) }
                } else {
                    locations
                }

                // Convert to ReportData
                val reportDataList = filteredLocations.map { loc ->
                    ReportData(
                        id = loc.id,
                        disease = loc.disease,
                        city = loc.city,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        severity = loc.severity,
                        count = loc.count,
                        timestamp = loc.timestamp
                    )
                }

                // Send to Gemini
                val insightReport = insightsService.generateInsights(reportDataList)
                
                _uiState.update { 
                    it.copy(
                        insightReport = insightReport,
                        isLoading = false,
                        isRefreshing = false,
                        isEmpty = reportDataList.isEmpty()
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isRefreshing = false,
                        errorMessage = e.message ?: "Failed to load insights"
                    ) 
                }
            }
        }
    }
}
