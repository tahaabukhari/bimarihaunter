package com.bimarihaunter.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.data.models.RegionData
import com.bimarihaunter.data.repository.BimarihaunterRepository
import com.bimarihaunter.data.repository.MockBimarihaunterRepository
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
class MapViewModel @Inject constructor(
    // Normally injected. Using Mock manually for frontend-only architecture.
    private val repository: BimarihaunterRepository = MockBimarihaunterRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var pollingJob: Job? = null

    init {
        loadRegions()
        startPolling()
    }

    fun loadRegions(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            
            try {
                val data = if (_uiState.value.searchQuery.isNotBlank()) {
                    repository.searchRegions(_uiState.value.searchQuery)
                } else {
                    repository.getRegions()
                }
                
                _uiState.update { 
                    it.copy(
                        regions = data,
                        isLoading = false,
                        isRefreshing = false,
                        isEmpty = data.isEmpty()
                    ) 
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

    fun selectRegion(region: RegionData) {
        _uiState.update { it.copy(selectedRegion = region) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedRegion = null) }
    }
    
    fun toggleSearch() {
        _uiState.update { it.copy(isSearchActive = !it.isSearchActive, searchQuery = "") }
        if (!_uiState.value.isSearchActive) {
            loadRegions()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce search
            loadRegions()
        }
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(30000) // 30 seconds
                // Background refresh without showing loading spinners
                if (_uiState.value.searchQuery.isBlank()) {
                    try {
                        val data = repository.getRegions()
                        _uiState.update { it.copy(regions = data, isEmpty = data.isEmpty()) }
                    } catch (e: Exception) {
                        // Ignore polling errors
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
