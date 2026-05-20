package com.bimarihaunter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.repository.FeedRepository
import com.bimarihaunter.db.OutbreakReportEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {
    val feed: Flow<List<OutbreakReportEntity>> = repository.getCachedFeed()
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    // Cache the resolved city and GPS coordinates for refresh triggers
    private var lastCity: String = "Karachi"
    private var lastLatitude: Double = 24.8607
    private var lastLongitude: Double = 67.0011
    
    fun syncFeed(city: String, latitude: Double, longitude: Double) {
        lastCity = city
        lastLatitude = latitude
        lastLongitude = longitude
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Loading
            try {
                repository.syncFeed(city, latitude, longitude)
                _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
                _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshFeed() {
        syncFeed(lastCity, lastLatitude, lastLongitude)
    }
    
    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Loading : SyncStatus()
        object Success : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }
}

class FeedViewModelFactory(private val repository: FeedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FeedViewModel(repository) as T
    }
}
