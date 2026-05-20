package com.bimarihaunter.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String = "",
    val initials: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class NewsArticle(
    @DocumentId val id: String = "",
    val category: String = "",
    val source: String = "",
    val timestamp: String = "",
    val title: String = "",
    val content: String = "",
    val location: String = "",
    val severity: String = "INFO" // INFO, WARNING, CRITICAL
)

data class OutbreakPoint(
    @DocumentId val id: String = "",
    val name: String = "",
    val severity: String = "LOW", // LOW, MEDIUM, HIGH, CRITICAL
    val cases: String = "0",
    val newToday: String = "0",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val affectedAreas: List<MapArea> = emptyList(),
    val relatedNews: List<String> = emptyList()
)

data class MapArea(
    val area: String = "",
    val severity: Float = 0.0f,
    val riskLevel: String = "Low"
)

data class AlertData(
    @DocumentId val id: String = "",
    val severity: String = "INFO", // INFO, WARNING, CRITICAL
    val title: String = "",
    val description: String = "",
    val timestamp: String = "",
    val read: Boolean = false
)

data class ChatGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val category: String = "",
    val severity: String = "INFO", // INFO, WARNING, CRITICAL
    val participants: List<String> = emptyList(),
    val names: Map<String, String> = emptyMap()
)

data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val sharedPostId: String? = null,
    val sharedPostTitle: String? = null,
    val sharedPostDisease: String? = null,
    val sharedPostSeverity: String? = null,
    val sharedPostUrl: String? = null
)
