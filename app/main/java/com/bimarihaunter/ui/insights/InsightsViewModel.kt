package com.bimarihaunter.ui.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.data.repository.BimarihaunterRepository
import com.bimarihaunter.data.repository.MockBimarihaunterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // Normally injected. Using Mock manually for frontend-only architecture.
    private val repository: BimarihaunterRepository = MockBimarihaunterRepository()
) : ViewModel() {

    private val regionName: String = checkNotNull(savedStateHandle["region"])

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
                val data = repository.filterInsights(
                    region = _uiState.value.region,
                    category = _uiState.value.selectedCategoryFilter
                )
                
                _uiState.update { 
                    it.copy(
                        insightsData = data,
                        isLoading = false,
                        isRefreshing = false,
                        isEmpty = data.totalCases == 0
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

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedCategoryFilter = filter) }
        loadInsights()
    }
}
