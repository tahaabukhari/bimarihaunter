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

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError
    
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

    fun syncFeed(context: android.content.Context, latitude: Double?, longitude: Double?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val lat = latitude ?: 30.3753
                val lon = longitude ?: 69.3451
                var cityName = "Pakistan"
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Pakistan"
                    }
                } catch (e: Exception) {
                    timber.log.Timber.w(e, "Geocoder failed, falling back to default city name.")
                }
                repository.syncFeed(cityName, lat, lon)
                onComplete()
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Sync failed"
                timber.log.Timber.e(e, "MapViewModel syncFeed failed")
            } finally {
                _isSyncing.value = false
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
