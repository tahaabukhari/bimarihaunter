package com.bimarihaunter.ui.viewmodel

import android.app.NotificationManager
import android.content.Context
import android.location.Geocoder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.BimarihaunterApplication
import com.bimarihaunter.repository.FeedRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class MapViewModel(private val repository: FeedRepository) : ViewModel() {

    private val _mapMarkers = MutableStateFlow<List<MapMarker>>(emptyList())
    val mapMarkers: StateFlow<List<MapMarker>> = _mapMarkers

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    // Tracks which marker IDs we've already notified about so we don't spam
    private val notifiedMarkerIds = mutableSetOf<String>()

    private val _nearbyAlert = MutableStateFlow<Pair<MapMarker, Double>?>(null)
    val nearbyAlert: StateFlow<Pair<MapMarker, Double>?> = _nearbyAlert

    init {
        viewModelScope.launch {
            repository.getCachedFeed().collect { reports ->
                _mapMarkers.value = reports
                    .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                    .map { report ->
                        MapMarker(
                            id        = report.id,
                            title     = report.title,
                            latitude  = report.latitude,
                            longitude = report.longitude,
                            severity  = report.severity,
                            disease   = report.disease,
                            summary   = report.summary.joinToString(" "),
                            scrapedAt = report.scraped_at,
                            publishedAt = report.published_at
                        )
                    }
            }
        }
    }

    fun syncFeed(
        context: Context,
        latitude: Double?,
        longitude: Double?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val lat = latitude ?: 30.3753
                val lon = longitude ?: 69.3451
                var cityName = "Pakistan"
                try {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        cityName = addresses[0].locality
                            ?: addresses[0].subAdminArea
                            ?: addresses[0].adminArea
                            ?: "Pakistan"
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Geocoder failed, using default city name.")
                }
                repository.syncFeed(cityName, lat, lon)
                // After sync, check proximity for new markers
                if (latitude != null && longitude != null) {
                    checkProximityNotifications(context, LatLng(latitude, longitude))
                }
                onComplete()
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Sync failed"
                Timber.e(e, "MapViewModel syncFeed failed")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Checks all current markers and fires a local notification for any
     * marker within 15 km that hasn't been notified about yet.
     */
    fun checkProximityNotifications(context: Context, userLocation: LatLng) {
        val markers = _mapMarkers.value
        for (marker in markers) {
            if (marker.id in notifiedMarkerIds) continue
            val distKm = distanceKm(
                userLocation.latitude, userLocation.longitude,
                marker.latitude, marker.longitude
            )
            if (distKm <= 15.0) {
                notifiedMarkerIds.add(marker.id)
                showProximityNotification(context, marker, distKm)
                _nearbyAlert.value = Pair(marker, distKm)
            }
        }
    }

    private fun showProximityNotification(context: Context, marker: MapMarker, distKm: Double) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val priority = when (marker.severity.lowercase()) {
                "high", "critical" -> NotificationCompat.PRIORITY_MAX
                "medium"           -> NotificationCompat.PRIORITY_HIGH
                else               -> NotificationCompat.PRIORITY_DEFAULT
            }
            val distText = if (distKm < 1.0) "< 1 km" else "~${distKm.toInt()} km"
            val notification = NotificationCompat.Builder(context, BimarihaunterApplication.CHANNEL_OUTBREAK)
                .setContentTitle("⚠️ Outbreak Alert Nearby ($distText)")
                .setContentText("${marker.disease.replaceFirstChar { it.uppercase() }} reported near you — ${marker.title.take(80)}")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("${marker.disease.replaceFirstChar { it.uppercase() }} outbreak reported ${distText} from your location.\n\n${marker.summary.take(200)}"))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(priority)
                .setAutoCancel(true)
                .build()
            nm.notify(marker.id.hashCode(), notification)
        } catch (e: Exception) {
            Timber.w(e, "Failed to show proximity notification for marker ${marker.id}")
        }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R    = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2) * sin(dLat / 2) +
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                   sin(dLon / 2) * sin(dLon / 2)
        val c    = 2 * acos(kotlin.math.sqrt(a).coerceIn(0.0, 1.0))
        return R * c
    }
}

data class MapMarker(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String,       // "high", "medium", "low"
    val disease: String,
    val summary: String,
    val scrapedAt: String = "",  // ISO string or Firestore timestamp string
    val publishedAt: String = ""
) {
    /** Human-readable "last updated" label shown on the map pin info window. */
    fun lastUpdatedLabel(): String {
        val raw = scrapedAt.ifBlank { publishedAt }
        if (raw.isBlank()) return "Unknown time"
        return try {
            // Firestore timestamps come back as "MMM d, yyyy 'at' h:mm:ss a z"
            val formats = listOf(
                "MMM d, yyyy 'at' h:mm:ss a z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "EEE MMM dd HH:mm:ss zzz yyyy"
            )
            var parsed: Date? = null
            for (fmt in formats) {
                try { parsed = SimpleDateFormat(fmt, Locale.ENGLISH).parse(raw); break } catch (_: Exception) {}
            }
            if (parsed != null) {
                val now = System.currentTimeMillis()
                val diff = now - parsed.time
                when {
                    diff < 60_000L       -> "Just now"
                    diff < 3_600_000L    -> "${diff / 60_000}m ago"
                    diff < 86_400_000L   -> "${diff / 3_600_000}h ago"
                    diff < 604_800_000L  -> "${diff / 86_400_000}d ago"
                    else                 -> SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(parsed)
                }
            } else raw.take(16)
        } catch (e: Exception) {
            raw.take(16)
        }
    }
}

class MapViewModelFactory(private val repository: FeedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MapViewModel(repository) as T
    }
}
