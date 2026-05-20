package com.bimarihaunter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import timber.log.Timber

class BimarihaunterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return

            // 1. Outbreak / location-proximity alerts (from FCM)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_OUTBREAK,
                    "Disease Outbreak Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when a disease outbreak is detected near you"
                    enableVibration(true)
                }
            )

            // 2. Location-travel alerts (>15 km from last position)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_LOCATION,
                    "Location Change Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when you travel more than 15 km from your last known location"
                }
            )

            // 3. Direct-message / group-chat notifications
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "New messages from community chats and direct conversations"
                    enableVibration(true)
                }
            )

            // 4. Friend-request notifications
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_FRIEND_REQUESTS,
                    "Friend Requests",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications when someone sends you a friend request"
                }
            )

            Timber.d("All notification channels created")
        }
    }

    companion object {
        const val CHANNEL_OUTBREAK        = "outbreak_alerts"
        const val CHANNEL_LOCATION        = "location_alerts"
        const val CHANNEL_MESSAGES        = "messages"
        const val CHANNEL_FRIEND_REQUESTS = "friend_requests"
    }
}
