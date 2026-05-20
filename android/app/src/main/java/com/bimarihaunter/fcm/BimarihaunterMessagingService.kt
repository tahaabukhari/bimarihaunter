package com.bimarihaunter.fcm

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.bimarihaunter.BimarihaunterApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.android.gms.location.LocationServices
import timber.log.Timber
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Handles all FCM push notifications for BimariHaunter:
 *  - type = "outbreak"        → show only when user is within 15 km
 *  - type = "message"         → new chat message from a user/group
 *  - type = "friend_request"  → someone sent a friend request
 *  - type = "location_alert"  → backend triggered a travel-distance alert
 */
class BimarihaunterMessagingService : FirebaseMessagingService() {

    @android.annotation.SuppressLint("MissingPermission")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM received: ${remoteMessage.data}")

        val data = remoteMessage.data
        val type = data["type"] ?: "outbreak"  // Default to outbreak for backwards compat

        when (type) {
            "message" -> handleMessageNotification(data)
            "friend_request" -> handleFriendRequestNotification(data)
            "location_alert" -> handleLocationAlertNotification(data)
            else -> handleOutbreakNotification(data)  // "outbreak" and legacy payloads
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: $token")
        // TODO: send updated token to backend via RetrofitClient
    }

    // ─────────────────────────── outbreak ────────────────────────────────────

    @android.annotation.SuppressLint("MissingPermission")
    private fun handleOutbreakNotification(data: Map<String, String>) {
        val latitude  = data["latitude"]?.toDoubleOrNull()  ?: return
        val longitude = data["longitude"]?.toDoubleOrNull() ?: return
        val severity  = data["severity"]  ?: "medium"
        val title     = data["title"]     ?: "Disease Alert"
        val summary   = data["summary"]   ?: ""

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val distance = calculateDistanceKm(
                    location.latitude, location.longitude, latitude, longitude
                )
                // Alert only within 15 km (extended from original 10 km)
                if (distance <= 15.0) {
                    val priority = when (severity.lowercase()) {
                        "high", "critical" -> NotificationCompat.PRIORITY_MAX
                        "medium"           -> NotificationCompat.PRIORITY_HIGH
                        else               -> NotificationCompat.PRIORITY_DEFAULT
                    }
                    showNotification(
                        channelId  = BimarihaunterApplication.CHANNEL_OUTBREAK,
                        notifId    = NOTIF_ID_OUTBREAK,
                        title      = title,
                        body       = summary,
                        priority   = priority
                    )
                }
            } else {
                // Location unavailable — show notification anyway (safe default)
                showNotification(
                    channelId = BimarihaunterApplication.CHANNEL_OUTBREAK,
                    notifId   = NOTIF_ID_OUTBREAK,
                    title     = title,
                    body      = summary
                )
            }
        }
    }

    // ─────────────────────────── message ─────────────────────────────────────

    private fun handleMessageNotification(data: Map<String, String>) {
        val senderName = data["sender_name"] ?: "Someone"
        val text       = data["text"]        ?: "Sent you a message"
        val chatId     = data["chat_id"]     ?: ""

        showNotification(
            channelId = BimarihaunterApplication.CHANNEL_MESSAGES,
            notifId   = NOTIF_ID_MESSAGE,
            title     = "Message from $senderName",
            body      = text,
            priority  = NotificationCompat.PRIORITY_HIGH
        )
    }

    // ─────────────────────────── friend request ───────────────────────────────

    private fun handleFriendRequestNotification(data: Map<String, String>) {
        val fromName = data["from_name"] ?: "A user"

        showNotification(
            channelId = BimarihaunterApplication.CHANNEL_FRIEND_REQUESTS,
            notifId   = NOTIF_ID_FRIEND,
            title     = "New Friend Request",
            body      = "$fromName wants to connect with you",
            priority  = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    // ─────────────────────────── location alert ───────────────────────────────

    private fun handleLocationAlertNotification(data: Map<String, String>) {
        val distanceKm = data["distance_km"] ?: "15+"
        val newCity    = data["new_city"]    ?: "a new area"

        showNotification(
            channelId = BimarihaunterApplication.CHANNEL_LOCATION,
            notifId   = NOTIF_ID_LOCATION,
            title     = "Location Change Detected",
            body      = "You've moved ${distanceKm}km to $newCity. Local outbreak feed updated.",
            priority  = NotificationCompat.PRIORITY_HIGH
        )
    }

    // ─────────────────────────── helpers ─────────────────────────────────────

    private fun showNotification(
        channelId: String,
        notifId: Int,
        title: String,
        body: String,
        priority: Int = NotificationCompat.PRIORITY_HIGH
    ) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(priority)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId, notification)
        Timber.d("Notification shown [$channelId]: $title")
    }

    private fun calculateDistanceKm(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val R    = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2) * sin(dLat / 2) +
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                   sin(dLon / 2) * sin(dLon / 2)
        val c    = 2 * acos(kotlin.math.sqrt(a.coerceIn(0.0, 1.0)))
        return R * c
    }

    companion object {
        private const val NOTIF_ID_OUTBREAK = 1001
        private const val NOTIF_ID_MESSAGE  = 2001
        private const val NOTIF_ID_FRIEND   = 3001
        private const val NOTIF_ID_LOCATION = 4001
    }
}
