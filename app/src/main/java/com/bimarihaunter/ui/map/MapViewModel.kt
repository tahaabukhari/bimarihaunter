package com.bimarihaunter.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.data.api.FirebaseReportsDataSource
import com.bimarihaunter.data.mock.MOCK_SUMMARY_STATS
import com.bimarihaunter.data.models.OutbreakLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {
    
    // Fallback Mock Data source since Firebase is not fully wired
    private val firebaseReportsDataSource = FirebaseReportsDataSource()

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var dataStreamJob: Job? = null

    init {
        loadLocations()
    }

    fun loadLocations(isRefresh: Boolean = false) {
        dataStreamJob?.cancel()
        dataStreamJob = viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            
            try {
                firebaseReportsDataSource.getReportsStream().collect { locations ->
                    val filteredLocations = if (_uiState.value.searchQuery.isNotBlank()) {
                        val q = _uiState.value.searchQuery.lowercase()
                        locations.filter { 
                            it.city.lowercase().contains(q) || 
                            it.disease.lowercase().contains(q) || 
                            it.severity.lowercase().contains(q) 
                        }
                    } else {
                        locations
                    }

                    // In a real scenario, SummaryStats would be calculated from the real stream data
                    val summaryStats = if (filteredLocations.isNotEmpty()) {
                        MOCK_SUMMARY_STATS.copy(
                            totalCases = filteredLocations.sumOf { it.count },
                            criticalAreas = filteredLocations.count { it.severity == "high" }
                        )
                    } else null
                    
                    _uiState.update { 
                        it.copy(
                            outbreakLocations = filteredLocations,
                            summaryStats = summaryStats,
                            isLoading = false,
                            isRefreshing = false,
                            isEmpty = filteredLocations.isEmpty()
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isRefreshing = false,
                        errorMessage = e.message ?: "Failed to load map data"
                    ) 
                }
            }
        }
    }

    fun selectLocation(location: OutbreakLocation) {
        _uiState.update { it.copy(selectedLocation = location) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedLocation = null) }
    }
    
    fun toggleSearch() {
        _uiState.update { it.copy(isSearchActive = !it.isSearchActive, searchQuery = "") }
        if (!_uiState.value.isSearchActive) {
            loadLocations()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce search
            loadLocations()
        }
    }

    override fun onCleared() {
        super.onCleared()
        dataStreamJob?.cancel()
    }
}
