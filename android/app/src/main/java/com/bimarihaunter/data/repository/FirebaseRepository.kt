package com.bimarihaunter.data.repository

import com.bimarihaunter.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.bimarihaunter.network.RetrofitClient

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    init {
        seedDatabaseIfEmpty()
    }

    // Real-time Flow for News
    fun getNews(): Flow<List<NewsArticle>> = callbackFlow {
        val listener = db.collection("news")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val articles = snapshot?.toObjects(NewsArticle::class.java) ?: emptyList()
                trySend(articles)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Outbreaks
    fun getOutbreaks(): Flow<List<OutbreakPoint>> = callbackFlow {
        val listener = db.collection("outbreaks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val points = snapshot?.toObjects(OutbreakPoint::class.java) ?: emptyList()
                trySend(points)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Alerts
    fun getAlerts(): Flow<List<AlertData>> = callbackFlow {
        val listener = db.collection("alerts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val alerts = snapshot?.toObjects(AlertData::class.java) ?: emptyList()
                trySend(alerts)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for ChatGroups
    fun getChatGroups(): Flow<List<ChatGroup>> = callbackFlow {
        val listener = db.collection("chats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.toObjects(ChatGroup::class.java) ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Messages inside a ChatGroup
    fun getMessages(chatGroupId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats").document(chatGroupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // Send a message
    fun sendMessage(chatGroupId: String, message: Message) {
        val messageRef = db.collection("chats").document(chatGroupId)
            .collection("messages").document()
        val finalMessage = message.copy(id = messageRef.id)
        
        db.runTransaction { transaction ->
            transaction.set(messageRef, finalMessage)
            transaction.update(
                db.collection("chats").document(chatGroupId),
                mapOf(
                    "lastMessage" to message.text,
                    "lastMessageTime" to "Just now"
                )
            )
        }.addOnFailureListener {
            // Log or handle error if transaction fails
        }
    }

    // User profiles
    fun saveUserProfile(user: User) {
        db.collection("users").document(user.uid).set(user)
    }

    fun getUserProfile(uid: String, onComplete: (User?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                onComplete(document.toObject(User::class.java))
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    // --- Direct Chats & Messaging Extension ---

    // Real-time Flow for Direct Chats
    fun getDirectChats(currentUid: String): Flow<List<ChatGroup>> = callbackFlow {
        val listener = db.collection("direct_chats")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(ChatGroup::class.java) ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Messages inside a Direct Chat
    fun getDirectMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("direct_chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // Send a Direct Message (automatically creates the chat parent document if it doesn't exist)
    fun sendDirectMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String,
        message: Message
    ) {
        val chatRef = db.collection("direct_chats").document(chatId)
        val messageRef = chatRef.collection("messages").document()
        val finalMessage = message.copy(id = messageRef.id)
        
        db.runTransaction { transaction ->
            // Create or update chat document
            val chatData = mapOf(
                "id" to chatId,
                "category" to "Direct",
                "lastMessage" to if (message.deleted) "[Message deleted]" else (message.text.ifEmpty { "Shared an outbreak report" }),
                "lastMessageTime" to "Just now",
                "participants" to listOf(senderId, recipientId),
                "names" to mapOf(senderId to senderName, recipientId to recipientName),
                "severity" to "INFO"
            )
            transaction.set(chatRef, chatData, com.google.firebase.firestore.SetOptions.merge())
            transaction.set(messageRef, finalMessage)
        }.addOnFailureListener {
            // Handle error
        }
    }

    // Edit a Direct Message
    fun editDirectMessage(chatId: String, messageId: String, newText: String) {
        val messageRef = db.collection("direct_chats").document(chatId)
            .collection("messages").document(messageId)
        messageRef.update(mapOf("text" to newText, "edited" to true))
    }

    // Delete (soft-delete) a Direct Message
    fun deleteDirectMessage(chatId: String, messageId: String) {
        val messageRef = db.collection("direct_chats").document(chatId)
            .collection("messages").document(messageId)
        messageRef.update(mapOf(
            "text" to "[Message deleted]",
            "deleted" to true,
            "sharedPostId" to null,
            "sharedPostTitle" to null,
            "sharedPostDisease" to null,
            "sharedPostSeverity" to null,
            "sharedPostUrl" to null
        ))
    }

    // Edit a Group Message
    fun editGroupMessage(chatGroupId: String, messageId: String, newText: String) {
        val messageRef = db.collection("chats").document(chatGroupId)
            .collection("messages").document(messageId)
        messageRef.update(mapOf("text" to newText, "edited" to true))
    }

    // Delete (soft-delete) a Group Message
    fun deleteGroupMessage(chatGroupId: String, messageId: String) {
        val messageRef = db.collection("chats").document(chatGroupId)
            .collection("messages").document(messageId)
        messageRef.update(mapOf(
            "text" to "[Message deleted]",
            "deleted" to true,
            "sharedPostId" to null,
            "sharedPostTitle" to null,
            "sharedPostDisease" to null,
            "sharedPostSeverity" to null,
            "sharedPostUrl" to null
        ))
    }

    // --- Friends & Blocking API REST Wrappers ---

    suspend fun searchUsers(query: String): List<User> {
        return try {
            RetrofitClient.apiService.searchUsers(query).users
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addFriend(friendId: String): Boolean {
        return try {
            RetrofitClient.apiService.addFriend(friendId).status == "success"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFriend(friendId: String): Boolean {
        return try {
            RetrofitClient.apiService.removeFriend(friendId).status == "success"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFriends(): List<com.bimarihaunter.network.FriendInfo> {
        return try {
            RetrofitClient.apiService.getFriends().friends
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun blockUser(blockedId: String): Boolean {
        return try {
            RetrofitClient.apiService.blockUser(blockedId).status == "success"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unblockUser(blockedId: String): Boolean {
        return try {
            RetrofitClient.apiService.unblockUser(blockedId).status == "success"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBlockedUsers(): List<String> {
        return try {
            RetrofitClient.apiService.getBlockedUsers().blocked
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Automatically seed data to make the app work immediately out of the box
    private fun seedDatabaseIfEmpty() {
        // Seed News
        db.collection("news").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val seedNews = listOf(
                    NewsArticle("", "Outbreak", "Dawn News", "2h ago", "Dengue Cases Surge in Lahore's Urban Centers", "Health authorities report over 340 new cases in the past week across multiple districts.", "Lahore, Punjab", "CRITICAL"),
                    NewsArticle("", "Disaster", "Geo News", "4h ago", "Flood Warning Issued for Southern Sindh", "Pakistan Meteorological Department warns of heavy rainfall and potential flooding.", "Hyderabad, Sindh", "WARNING"),
                    NewsArticle("", "Research", "The News", "6h ago", "New Malaria Vaccine Trial Begins in Pakistan", "WHO-backed clinical trials commence at major hospitals in Islamabad and Rawalpindi.", "Islamabad", "INFO"),
                    NewsArticle("", "Local", "Express Tribune", "8h ago", "Water Contamination Alert in Peshawar", "Multiple localities report contaminated water supply leading to gastroenteritis cases.", "Peshawar, KPK", "CRITICAL"),
                    NewsArticle("", "Pharmacy", "ARY News", "12h ago", "Essential Medicine Prices Rise by 15% Nationwide", "Pharmaceutical companies cite raw material cost increase as primary factor.", "Karachi, Sindh", "INFO")
                )
                seedNews.forEach { db.collection("news").add(it) }
            }
        }

        // Seed Outbreaks
        db.collection("outbreaks").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val seedOutbreaks = listOf(
                    OutbreakPoint("", "Dengue Outbreak — Lahore", "CRITICAL", "1,240", "18", 31.5204, 74.3587, listOf(
                        MapArea("Gulberg", 0.85f, "High"),
                        MapArea("Model Town", 0.65f, "Medium"),
                        MapArea("DHA Phase 5", 0.45f, "Moderate")
                    ), listOf(
                        "Punjab Govt Activates Emergency Response",
                        "Fumigation Drives Begin Across Lahore"
                    )),
                    OutbreakPoint("", "Cholera Outbreak — Karachi", "HIGH", "856", "32", 24.8607, 67.0011, listOf(
                        MapArea("Lyari", 0.9f, "Critical"),
                        MapArea("Orangi", 0.75f, "High"),
                        MapArea("Clifton", 0.2f, "Low")
                    ), listOf(
                        "Safe drinking water advisories issued",
                        "Emergency clinics set up in Lyari"
                    ))
                )
                seedOutbreaks.forEach { db.collection("outbreaks").add(it) }
            }
        }

        // Seed Alerts
        db.collection("alerts").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val seedAlerts = listOf(
                    AlertData("", "CRITICAL", "Dengue Outbreak — Lahore", "340+ cases reported in urban centers. Emergency response activated.", "12 min ago", false),
                    AlertData("", "WARNING", "Flood Warning — Southern Sindh", "Heavy rainfall expected. Evacuation advisories issued for low-lying areas.", "1h ago", false),
                    AlertData("", "INFO", "COVID-19 Booster Available", "Free booster doses now available at all government hospitals.", "3h ago", true),
                    AlertData("", "CRITICAL", "Water Contamination — Peshawar", "Multiple areas report unsafe drinking water. Boil water advisory in effect.", "5h ago", false),
                    AlertData("", "WARNING", "Heatwave Alert — Karachi", "Temperatures expected to exceed 45°C this week.", "8h ago", true)
                )
                seedAlerts.forEach { db.collection("alerts").add(it) }
            }
        }

        // Seed Chats
        db.collection("chats").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val seedChats = listOf(
                    ChatGroup("dengue_lahore", "Dengue Watch — Lahore", "Fumigation team is active in Gulberg.", "2m ago", "Outbreak", "CRITICAL"),
                    ChatGroup("flood_sindh", "Flood Rescue — Sindh", "Boats deployed to Badin district.", "15m ago", "Disaster", "WARNING"),
                    ChatGroup("bimari_ai", "Bimarihaunter AI", "Ask me anything about symptoms.", "1h ago", "Assistant", "INFO")
                )
                seedChats.forEach { chat ->
                    db.collection("chats").document(chat.id).set(chat).addOnSuccessListener {
                        // Seed default messages for dengue watch
                        if (chat.id == "dengue_lahore") {
                            val seedMessages = listOf(
                                Message("", "system", "System", "Welcome to Dengue Watch group.", 1716120000000, true),
                                Message("", "user1", "Ahmad Khan", "Are there fumigation drives planned in Model Town?", 1716120100000, false),
                                Message("", "user2", "Dr. Fatima Zahra", "Yes, DHA and Model Town are scheduled for tomorrow morning.", 1716120200000, false),
                                Message("", "user1", "Ahmad Khan", "Great, thanks for the update doctor!", 1716120300000, false)
                            )
                            seedMessages.forEach { msg ->
                                db.collection("chats").document(chat.id).collection("messages").add(msg)
                            }
                        } else if (chat.id == "bimari_ai") {
                            val seedMessages = listOf(
                                Message("", "ai", "Bimarihaunter AI", "Hello! I am your AI health assistant. Ask me about symptoms, precautions, or outbreaks.", 1716120000000, false)
                            )
                            seedMessages.forEach { msg ->
                                db.collection("chats").document(chat.id).collection("messages").add(msg)
                            }
                        }
                    }
                }
            }
        }
    }
}
