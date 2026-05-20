package com.bimarihaunter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.repository.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(private val repository: FeedRepository) : ViewModel() {
    private val _mapMarkers = MutableStateFlow<List<MapMarker>>(emptyList())
    val mapMarkers: StateFlow<List<MapMarker>> = _mapMarkers
    
    init {
        viewModelScope.launch {
            repository.getCachedFeed().collect { reports ->
                _mapMarkers.value = reports.map { report ->
                    MapMarker(
                        id = report.id,
                        title = report.title,
                        latitude = report.latitude,
                        longitude = report.longitude,
                        severity = report.severity,
                        disease = report.disease,
                        summary = report.summary.joinToString(separator = " ")
                    )
                }
            }
        }
    }
}

data class MapMarker(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String, // "high", "medium", "low"
    val disease: String,
    val summary: String
)

class MapViewModelFactory(private val repository: FeedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MapViewModel(repository) as T
    }
}
