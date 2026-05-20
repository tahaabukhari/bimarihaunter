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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
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
    val defaultLocation = LatLng(30.3753, 69.3451) // Pakistan center
    val currentLocation = userLocation ?: defaultLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation,
            if (userLocation != null) 12f else 5.5f
        )
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

    // Build real insights from actual marker data
    val insights = remember(currentLocation, markers) {
        buildRealInsights(currentLocation, markers)
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
                    snippet = "${marker.disease.replaceFirstChar { it.uppercase() }} · ${marker.severity.replaceFirstChar { it.uppercase() }}",
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue)
                )
            }

            userLocation?.let {
                Circle(
                    center = it,
                    radius = 25000.0,
                    strokeColor = LimeGreen.copy(alpha = 0.45f),
                    fillColor = LimeGreen.copy(alpha = 0.08f)
                )
            }
        }

        // Top: location permission prompt
        if (!hasLocationPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightBlack.copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enable location to see outbreak alerts near you.",
                        color = OffWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        colors = ButtonDefaults.buttonColors(containerColor = LimeGreen, contentColor = MidnightBlack)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Allow location", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bottom: real data insights panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CharcoalGrey.copy(alpha = 0.97f))
                .padding(16.dp),
            horizontalAlignment = if (markers.isEmpty()) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (markers.isEmpty()) {
                Text(
                    text = "Sync Feed First",
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OffWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Map markers are empty. Pull recent outbreak data to localize alerts.",
                    color = MediumGrey,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = LimeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            viewModel.syncFeed(
                                context = context,
                                latitude = userLocation?.latitude,
                                longitude = userLocation?.longitude
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeGreen,
                            contentColor = MidnightBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sync Live Outbreaks", fontWeight = FontWeight.Bold)
                    }
                }
                syncError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = EmberRed,
                        fontSize = 12.sp,
                        fontFamily = InterFamily,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Outbreak Insights",
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = OffWhite
                    )
                    Text(
                        text = "${markers.size} reports",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = LimeGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real stat boxes from actual data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        value = insights.nearbyCount.toString(),
                        label = "Nearby (25km)"
                    )
                    StatBox(
                        value = insights.highSeverityCount.toString(),
                        label = "High Severity"
                    )
                    StatBox(
                        value = insights.uniqueDiseases.toString(),
                        label = "Disease Types"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Disease breakdown chips
                if (insights.diseaseBreakdown.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        insights.diseaseBreakdown.forEach { (disease, count) ->
                            DiseaseChip(disease = disease, count = count)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Summary text from real data
                Text(
                    text = insights.summary,
                    color = MediumGrey,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                if (insights.closestAlert != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = EmberRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nearest: ${insights.closestAlert}",
                            color = EmberRed,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiseaseChip(disease: String, count: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightBlack.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Place, contentDescription = null, tint = LimeGreen, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${disease.replaceFirstChar { it.uppercase() }} ($count)",
            color = OffWhite,
            fontFamily = InterFamily,
            fontSize = 12.sp
        )
    }
}

private data class RealInsights(
    val nearbyCount: Int,
    val highSeverityCount: Int,
    val uniqueDiseases: Int,
    val diseaseBreakdown: Map<String, Int>,
    val summary: String,
    val closestAlert: String?
)

private fun buildRealInsights(currentLocation: LatLng, markers: List<MapMarker>): RealInsights {
    if (markers.isEmpty()) {
        return RealInsights(
            nearbyCount = 0,
            highSeverityCount = 0,
            uniqueDiseases = 0,
            diseaseBreakdown = emptyMap(),
            summary = "No outbreak reports loaded yet. Pull down to refresh.",
            closestAlert = null
        )
    }

    val nearbyMarkers = markers.filter {
        distanceBetween(currentLocation, LatLng(it.latitude, it.longitude)) <= 25000.0
    }
    val highSeverity = markers.count { it.severity.lowercase() == "high" }
    val diseaseBreakdown = markers.groupBy { it.disease.lowercase() }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(4)
        .toMap()

    // Find closest marker
    val closest = markers.minByOrNull {
        distanceBetween(currentLocation, LatLng(it.latitude, it.longitude))
    }
    val closestLabel = closest?.let {
        val distKm = (distanceBetween(currentLocation, LatLng(it.latitude, it.longitude)) / 1000).toInt()
        "${it.disease.replaceFirstChar { c -> c.uppercase() }} alert ~${distKm}km away"
    }

    val summaryText = buildString {
        append("${markers.size} outbreak report${if (markers.size != 1) "s" else ""} tracked across Pakistan. ")
        if (nearbyMarkers.isNotEmpty()) {
            append("${nearbyMarkers.size} within 25 km of your location. ")
        }
        if (highSeverity > 0) {
            append("$highSeverity high-severity alert${if (highSeverity != 1) "s" else ""} require attention.")
        } else {
            append("No high-severity alerts in current data.")
        }
    }

    return RealInsights(
        nearbyCount = nearbyMarkers.size,
        highSeverityCount = highSeverity,
        uniqueDiseases = diseaseBreakdown.size,
        diseaseBreakdown = diseaseBreakdown,
        summary = summaryText,
        closestAlert = closestLabel
    )
}

private fun distanceBetween(from: LatLng, to: LatLng): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val a = kotlin.math.sin(dLat / 2).pow(2.0) +
            kotlin.math.cos(Math.toRadians(from.latitude)) *
            kotlin.math.cos(Math.toRadians(to.latitude)) *
            kotlin.math.sin(dLng / 2).pow(2.0)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadius * c
}
