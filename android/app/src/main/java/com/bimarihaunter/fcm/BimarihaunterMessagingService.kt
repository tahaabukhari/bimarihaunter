package com.bimarihaunter.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.android.gms.location.LocationServices
import timber.log.Timber
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class BimarihaunterMessagingService : FirebaseMessagingService() {
    
    @android.annotation.SuppressLint("MissingPermission")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val data = remoteMessage.data
        val latitude = data["latitude"]?.toDoubleOrNull() ?: return
        val longitude = data["longitude"]?.toDoubleOrNull() ?: return
        val severity = data["severity"] ?: "medium"
        val title = data["title"] ?: "Disease Alert"
        val summary = data["summary"] ?: ""
        
        // Get user's current location
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val distance = calculateDistance(
                    location.latitude, location.longitude,
                    latitude, longitude
                )
                
                // Show notification if within 10km
                if (distance <= 10.0) {
                    showNotification(title, summary, severity)
                }
            }
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Haversine formula
        val R = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * acos(kotlin.math.sqrt(a))
        return R * c
    }
    
    private fun showNotification(title: String, summary: String, severity: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel
        val channel = NotificationChannel(
            "outbreak_alerts",
            "Disease Outbreak Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        
        // Build notification
        val notification = NotificationCompat.Builder(this, "outbreak_alerts")
            .setContentTitle(title)
            .setContentText(summary)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Timber.d("Notification shown: $title")
    }
}
