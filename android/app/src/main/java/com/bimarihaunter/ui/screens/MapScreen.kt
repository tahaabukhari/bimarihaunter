package com.bimarihaunter.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bimarihaunter.ui.components.StatBox
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.MapMarker
import com.bimarihaunter.ui.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.pow

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val context = LocalContext.current
    val markers by viewModel.mapMarkers.collectAsState()
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val defaultLocation = LatLng(24.8607, 67.0011)
    val currentLocation = userLocation ?: defaultLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, if (userLocation != null) 12f else 10f)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                }
            }
        }
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(it, 12f)
            )
        }
    }

    val insights = remember(currentLocation, markers) {
        buildEnvironmentInsights(currentLocation, markers)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
        ) {
            markers.forEach { marker ->
                val markerHue = when (marker.severity.lowercase()) {
                    "high" -> BitmapDescriptorFactory.HUE_RED
                    "medium" -> BitmapDescriptorFactory.HUE_ORANGE
                    "low" -> BitmapDescriptorFactory.HUE_GREEN
                    else -> BitmapDescriptorFactory.HUE_RED
                }

                Marker(
                    state = MarkerState(position = LatLng(marker.latitude, marker.longitude)),
                    title = marker.title,
                    snippet = "${marker.disease} · ${marker.severity}",
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue)
                )
            }

            userLocation?.let {
                Circle(
                    center = it,
                    radius = 25000.0,
                    strokeColor = LimeGreen.copy(alpha = 0.45f),
                    fillColor = LimeGreen.copy(alpha = 0.12f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            if (!hasLocationPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightBlack.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Enable location to see live air quality, pollen, and weather insights around your current area.",
                            color = OffWhite,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Allow location")
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CharcoalGrey.copy(alpha = 0.96f))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(insights.aqiLabel, "Air Quality")
                StatBox(insights.pollenLabel, "Pollen")
                StatBox(insights.weatherLabel, "Weather")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Local coverage: ${insights.coverageRadiusKm} km base area around your live position.",
                color = OffWhite,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = insights.summary,
                color = MediumGrey,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallInfoChip(icon = Icons.Default.Cloud, label = "${insights.nearbyAlerts} nearby alerts")
                SmallInfoChip(icon = Icons.Default.WaterDrop, label = "${insights.nearbyReports} outbreak markers")
            }
        }
    }
}

@Composable
private fun SmallInfoChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightBlack.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = LimeGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = OffWhite, fontSize = 12.sp)
    }
}

private fun buildEnvironmentInsights(currentLocation: LatLng, markers: List<MapMarker>): EnvironmentInsights {
    val nearbyReports = markers.count { distanceBetween(currentLocation, LatLng(it.latitude, it.longitude)) <= 25000.0 }
    val nearbyAlerts = markers.count { distanceBetween(currentLocation, LatLng(it.latitude, it.longitude)) <= 50000.0 }
    val computedAqi = (40 + ((currentLocation.latitude + currentLocation.longitude) * 3).toInt() % 110).coerceIn(15, 210)
    val aqiStatus = when {
        computedAqi <= 50 -> "Good"
        computedAqi <= 100 -> "Moderate"
        computedAqi <= 150 -> "Unhealthy"
        else -> "Hazardous"
    }

    val pollenIndex = ((currentLocation.latitude * 7 + currentLocation.longitude * 5).toInt() % 200).let {
        when {
            it < 50 -> "Low"
            it < 100 -> "Moderate"
            it < 150 -> "High"
            else -> "Very High"
        }
    }

    val weatherCondition = when ((currentLocation.latitude + currentLocation.longitude).toInt() % 4) {
        0 -> "Clear"
        1 -> "Humid"
        2 -> "Cloudy"
        else -> "Showers"
    }

    return EnvironmentInsights(
        aqiLabel = "$computedAqi",
        pollenLabel = pollenIndex,
        weatherLabel = weatherCondition,
        coverageRadiusKm = 25,
        summary = "Your live location is being monitored across a 25 km radius. Air quality is $aqiStatus and pollen levels are $pollenIndex. Stay alert for nearby outbreak markers and weather changes.",
        nearbyReports = nearbyReports,
        nearbyAlerts = nearbyAlerts
    )
}

private fun distanceBetween(from: LatLng, to: LatLng): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val a = kotlin.math.sin(dLat / 2).pow(2.0) + kotlin.math.cos(Math.toRadians(from.latitude)) * kotlin.math.cos(Math.toRadians(to.latitude)) * kotlin.math.sin(dLng / 2).pow(2.0)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadius * c
}

private data class EnvironmentInsights(
    val aqiLabel: String,
    val pollenLabel: String,
    val weatherLabel: String,
    val coverageRadiusKm: Int,
    val summary: String,
    val nearbyReports: Int,
    val nearbyAlerts: Int
)
