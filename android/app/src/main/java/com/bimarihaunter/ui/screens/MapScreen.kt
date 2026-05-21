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
import androidx.compose.material.icons.filled.Refresh
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
    val nearbyAlert by viewModel.nearbyAlert.collectAsState()
    var showNearbyBanner by remember { mutableStateOf(false) }
    var nearbyBannerData by remember { mutableStateOf<Pair<com.bimarihaunter.ui.viewmodel.MapMarker, Double>?>(null) }

    LaunchedEffect(nearbyAlert) {
        if (nearbyAlert != null) {
            nearbyBannerData = nearbyAlert
            showNearbyBanner = true
            kotlinx.coroutines.delay(6000)
            showNearbyBanner = false
        }
    }
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
        userLocation?.let { loc ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(loc, 12f)
            )
            viewModel.checkProximityNotifications(context, loc)
        }
    }

    // Dynamic pin rendering: only show markers within the current camera viewport
    // This gives the "pins appear as you scroll" effect
    val visibleMarkers by remember {
        derivedStateOf {
            val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            if (bounds == null) {
                markers // Show all if projection not ready
            } else {
                // Expand bounds slightly so pins near edges are visible
                val latPad = (bounds.northeast.latitude - bounds.southwest.latitude) * 0.1
                val lngPad = (bounds.northeast.longitude - bounds.southwest.longitude) * 0.1
                markers.filter { m ->
                    m.latitude  >= bounds.southwest.latitude  - latPad &&
                    m.latitude  <= bounds.northeast.latitude  + latPad &&
                    m.longitude >= bounds.southwest.longitude - lngPad &&
                    m.longitude <= bounds.northeast.longitude + lngPad
                }
            }
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
            visibleMarkers.forEach { marker ->
                val markerHue = when (marker.severity.lowercase()) {
                    "high" -> BitmapDescriptorFactory.HUE_RED
                    "medium" -> BitmapDescriptorFactory.HUE_ORANGE
                    "low" -> BitmapDescriptorFactory.HUE_GREEN
                    else -> BitmapDescriptorFactory.HUE_RED
                }

                Marker(
                    state = MarkerState(position = LatLng(marker.latitude, marker.longitude)),
                    title = marker.title,
                    snippet = "${marker.disease.replaceFirstChar { it.uppercase() }} · ${marker.severity.replaceFirstChar { it.uppercase() }} · ${marker.lastUpdatedLabel()}",
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${visibleMarkers.size}/${markers.size} reports",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = LimeGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = LimeGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    viewModel.syncFeed(
                                        context = context,
                                        latitude = userLocation?.latitude,
                                        longitude = userLocation?.longitude
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh news",
                                    tint = LimeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real stat boxes from actual data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        value = insights.nearbyCount.toString(),
                        label = "Nearby (25km)",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = insights.highSeverityCount.toString(),
                        label = "High Severity",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = insights.uniqueDiseases.toString(),
                        label = "Disease Types",
                        modifier = Modifier.weight(1f)
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

        // Ghost alert banner — appears when user enters a 15km outbreak zone
        androidx.compose.animation.AnimatedVisibility(
            visible = showNearbyBanner,
            enter = androidx.compose.animation.slideInVertically { -it } +
                    androidx.compose.animation.fadeIn(),
            exit  = androidx.compose.animation.slideOutVertically { -it } +
                    androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        ) {
            nearbyBannerData?.let { (marker, dist) ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(
                            when (marker.severity.lowercase()) {
                                "high", "critical" -> com.bimarihaunter.ui.theme.EmberRed
                                "medium"           -> com.bimarihaunter.ui.theme.GoldWarning
                                else               -> com.bimarihaunter.ui.theme.LimeGreen
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(com.bimarihaunter.R.drawable.ghost_alert),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Outbreak ${if (dist < 1.0) "< 1 km" else "~${dist.toInt()} km"} away!",
                            color = com.bimarihaunter.ui.theme.MidnightBlack,
                            fontFamily = com.bimarihaunter.ui.theme.SpaceGroteskFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            marker.disease.replaceFirstChar { it.uppercase() } + " — " + marker.title.take(50),
                            color = com.bimarihaunter.ui.theme.MidnightBlack.copy(alpha = 0.8f),
                            fontFamily = com.bimarihaunter.ui.theme.InterFamily,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
