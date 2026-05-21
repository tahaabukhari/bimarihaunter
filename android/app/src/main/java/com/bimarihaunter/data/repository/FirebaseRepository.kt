package com.bimarihaunter.data.repository

import com.bimarihaunter.data.model.*
import com.bimarihaunter.network.RetrofitClient
import com.bimarihaunter.network.UserRegisterRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    // ═══════════════════════════════════════════════════════════════════════════
    // NEWS FEED — BULLETPROOF: NO INDEX REQUIRED
    // Reads /reports directly. No orderBy (avoids index requirement).
    // Sorts client-side. Falls back to /news seeds if /reports is empty.
    // ═══════════════════════════════════════════════════════════════════════════
    fun getNews(): Flow<List<NewsArticle>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        fun mapDoc(doc: DocumentSnapshot): NewsArticle? {
            val data = doc.data ?: return null
            val title = data["title"] as? String ?: return null
            if (title.isBlank()) return null
            val source = data["source"] as? String ?: "BimariHaunter"
            val rawText = data["raw_text"] as? String ?: data["content"] as? String ?: ""
            val ai = data["ai_analysis"] as? Map<*, *>
            val disease = (ai?.get("disease") as? String ?: data["category"] as? String ?: "Health")
                .replaceFirstChar { it.uppercase() }
            val severityRaw = (ai?.get("severity") as? String ?: data["severity"] as? String ?: "medium").lowercase()
            val severity = when (severityRaw) {
                "critical", "high" -> "CRITICAL"
                "medium", "moderate" -> "WARNING"
                else -> "INFO"
            }
            val location = (ai?.get("locations") as? List<*>)
                ?.mapNotNull { it as? String }?.firstOrNull()
                ?: data["location"] as? String
                ?: "Pakistan"
            val publishedAt = when (val v = data["published_at"]) {
                is Timestamp -> {
                    val diff = System.currentTimeMillis() - v.toDate().time
                    when {
                        diff < 3_600_000 -> "${diff / 60_000}m ago"
                        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                        else -> "${diff / 86_400_000}d ago"
                    }
                }
                is String -> v
                else -> data["timestamp"] as? String ?: ""
            }
            // For sorting: extract raw millis
            val sortKey = when (val v = data["published_at"]) {
                is Timestamp -> v.toDate().time
                else -> 0L
            }
            return NewsArticle(
                id = doc.id, category = disease, source = source,
                timestamp = publishedAt, title = title,
                content = rawText.take(200), location = location, severity = severity
            )
        }

        // Helper to get sort key from a document
        fun getSortKey(doc: DocumentSnapshot): Long {
            val data = doc.data ?: return 0L
            return when (val v = data["published_at"]) {
                is Timestamp -> v.toDate().time
                else -> 0L
            }
        }

        var fallbackListener: com.google.firebase.firestore.ListenerRegistration? = null

        // ── Strategy: Try /reports with NO orderBy (no index needed) ──
        // Use .limit(100) to cap the read, then sort client-side.
        val reportsListener = db.collection("reports")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "getNews /reports listener failed: ${error.message}")
                    // Even if /reports fails, try /news seeds
                    db.collection("news").get()
                        .addOnSuccessListener { ns ->
                            val seeds = ns.toObjects(NewsArticle::class.java)
                            trySend(seeds)
                        }
                        .addOnFailureListener { trySend(emptyList()) }
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                // Sort client-side by published_at descending
                val sorted = docs.sortedByDescending { getSortKey(it) }
                val articles = sorted.mapNotNull { mapDoc(it) }.take(50)

                if (articles.isNotEmpty()) {
                    trySend(articles)
                } else {
                    // /reports is empty — fall back to /news seed collection
                    Timber.d("getNews: /reports empty, falling back to /news seeds")
                    db.collection("news").get()
                        .addOnSuccessListener { ns ->
                            val seeds = ns.toObjects(NewsArticle::class.java)
                            trySend(seeds.ifEmpty { emptyList() })
                        }
                        .addOnFailureListener { trySend(emptyList()) }
                }
            }

        awaitClose {
            reportsListener.remove()
            fallbackListener?.remove()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OUTBREAKS
    // ═══════════════════════════════════════════════════════════════════════════
    fun getOutbreaks(): Flow<List<OutbreakPoint>> = callbackFlow {
        val listener = db.collection("outbreaks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "getOutbreaks failed — emitting empty list")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val points = snapshot?.toObjects(OutbreakPoint::class.java) ?: emptyList()
                trySend(points)
            }
        awaitClose { listener.remove() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALERTS
    // ═══════════════════════════════════════════════════════════════════════════
    fun getAlerts(): Flow<List<AlertData>> = callbackFlow {
        val listener = db.collection("alerts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "getAlerts failed — emitting empty list")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val alerts = snapshot?.toObjects(AlertData::class.java) ?: emptyList()
                trySend(alerts)
            }
        awaitClose { listener.remove() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHAT GROUPS — NO INDEX REQUIRED
    // ═══════════════════════════════════════════════════════════════════════════
    fun getChatGroups(): Flow<List<ChatGroup>> = callbackFlow {
        val listener = db.collection("chats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "getChatGroups failed — emitting empty list")
                    trySend(emptyList())
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "getMessages failed — emitting empty list")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = (snapshot?.toObjects(Message::class.java) ?: emptyList())
                    .sortedBy { it.timestamp }
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
        }.addOnFailureListener { e ->
            Timber.e(e, "sendMessage transaction failed")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER PROFILES
    // ═══════════════════════════════════════════════════════════════════════════
    suspend fun saveUserProfile(user: User) {
        val userMap = hashMapOf(
            "uid" to user.uid,
            "name" to user.name,
            "email" to user.email,
            "phoneNumber" to user.phoneNumber,
            "avatarUrl" to user.avatarUrl,
            "initials" to user.initials,
            "createdAt" to user.createdAt
        )
        try {
            db.collection("users").document(user.uid).set(userMap).await()
            Timber.d("User profile written to Firestore: ${user.uid}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to write user profile to Firestore: ${user.uid}")
        }
        try {
            val request = UserRegisterRequest(
                uid = user.uid,
                email = user.email,
                name = user.name,
                phone_number = user.phoneNumber.ifBlank { null },
                avatar_url = user.avatarUrl.ifBlank { null }
            )
            RetrofitClient.apiService.registerUser(request)
            Timber.d("User registered with backend successfully: ${user.uid}")
        } catch (e: Exception) {
            Timber.w(e, "Failed to register user with backend: ${user.uid}")
        }
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

    // ═══════════════════════════════════════════════════════════════════════════
    // DIRECT CHATS — NO INDEX REQUIRED (reads all, filters client-side)
    // ═══════════════════════════════════════════════════════════════════════════
    fun getDirectChats(currentUid: String): Flow<List<ChatGroup>> = callbackFlow {
        if (currentUid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = db.collection("direct_chats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "getDirectChats failed — emitting empty list")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Filter client-side to avoid needing array-contains index
                val chats = (snapshot?.toObjects(ChatGroup::class.java) ?: emptyList())
                    .filter { it.participants.contains(currentUid) }
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Messages inside a Direct Chat
    fun getDirectMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("direct_chats").document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "getDirectMessages failed — emitting empty list")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = (snapshot?.toObjects(Message::class.java) ?: emptyList())
                    .sortedBy { it.timestamp }
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // Send a Direct Message
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
            val chatData = mapOf(
                "id" to chatId,
                "category" to "Direct",
                "lastMessage" to if (message.deleted) "[Message deleted]" else (message.text.ifEmpty { "Shared an outbreak report" }),
                "lastMessageTime" to "Just now",
                "participants" to listOf(senderId, recipientId),
                "names" to mapOf(senderId to senderName, recipientId to recipientName),
                "severity" to "INFO"
            )
            transaction.set(chatRef, chatData, SetOptions.merge())
            transaction.set(messageRef, finalMessage)
        }.addOnFailureListener { e ->
            Timber.e(e, "sendDirectMessage transaction failed")
        }
    }

    // Edit / Delete messages
    fun editGroupMessage(chatId: String, messageId: String, newText: String) {
        db.collection("chats").document(chatId).collection("messages").document(messageId)
            .update(mapOf("text" to newText, "edited" to true))
    }

    fun deleteGroupMessage(chatId: String, messageId: String) {
        db.collection("chats").document(chatId).collection("messages").document(messageId)
            .update(mapOf("text" to "[Message deleted]", "deleted" to true))
    }

    fun editDirectMessage(chatId: String, messageId: String, newText: String) {
        db.collection("direct_chats").document(chatId).collection("messages").document(messageId)
            .update(mapOf("text" to newText, "edited" to true))
    }

    fun deleteDirectMessage(chatId: String, messageId: String) {
        db.collection("direct_chats").document(chatId).collection("messages").document(messageId)
            .update(mapOf("text" to "[Message deleted]", "deleted" to true))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FRIENDS — FIRESTORE NATIVE (NO BACKEND API NEEDED)
    // ═══════════════════════════════════════════════════════════════════════════

    // ─── searchUsers — FIXED: NO INDEX REQUIRED ──────────────────────────────
    // Previous version used .orderBy("name") which requires a Firestore
    // composite index. Without the index, Firestore throws an exception that
    // is caught silently, returning zero results every time.
    //
    // Fix: Use whereGreaterThanOrEqualTo / whereLessThanOrEqualTo range query
    // on the "name" field. Single-field range queries work without any
    // composite index. Sorting is done client-side after fetching.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun searchUsers(query: String): List<User> {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        val results = mutableListOf<User>()

        // 1. Exact UID match — if the query looks like a Firebase UID (≥20 chars,
        //    alphanumeric) try a direct document lookup first.
        if (trimmed.length >= 20 && trimmed.all { it.isLetterOrDigit() }) {
            try {
                val doc = db.collection("users").document(trimmed).get().await()
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    if (user != null && user.uid != currentUid) {
                        // Return immediately — UID is unambiguous
                        return listOf(user)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "searchUsers: UID lookup failed, continuing with name search")
            }
        }

        // 2. Name prefix range query — NO index required.
        //    Firestore range queries on a single field work without a composite
        //    index. We query both lowercase and title-case to handle mixed-case
        //    names stored in Firestore.
        val lower = trimmed.lowercase()
        val lowerUpper = lower + "\uF8FF"

        // 2a. Lowercase range (covers names stored in lowercase)
        try {
            val snap = db.collection("users")
                .whereGreaterThanOrEqualTo("name", lower)
                .whereLessThanOrEqualTo("name", lowerUpper)
                .limit(20)
                .get()
                .await()
            for (doc in snap.documents) {
                val user = doc.toObject(User::class.java) ?: continue
                if (user.uid == currentUid) continue
                if (!results.any { it.uid == user.uid }) results.add(user)
            }
        } catch (e: Exception) {
            Timber.w(e, "searchUsers: lowercase range query failed")
        }

        // 2b. Title-case range (covers names stored as "Ali Khan")
        val titleCase = trimmed.replaceFirstChar { it.uppercase() }
        val titleUpper = titleCase + "\uF8FF"
        try {
            val snap = db.collection("users")
                .whereGreaterThanOrEqualTo("name", titleCase)
                .whereLessThanOrEqualTo("name", titleUpper)
                .limit(20)
                .get()
                .await()
            for (doc in snap.documents) {
                val user = doc.toObject(User::class.java) ?: continue
                if (user.uid == currentUid) continue
                if (!results.any { it.uid == user.uid }) results.add(user)
            }
        } catch (e: Exception) {
            Timber.w(e, "searchUsers: title-case range query failed")
        }

        // 3. Email exact match — only if query contains '@'
        if (trimmed.contains("@")) {
            try {
                val emailSnap = db.collection("users")
                    .whereEqualTo("email", trimmed)
                    .limit(5)
                    .get()
                    .await()
                for (doc in emailSnap.documents) {
                    val user = doc.toObject(User::class.java) ?: continue
                    if (user.uid == currentUid) continue
                    if (!results.any { it.uid == user.uid }) results.add(user)
                }
            } catch (e: Exception) {
                Timber.w(e, "searchUsers: email query failed")
            }
        }

        // 4. Phone number exact match
        if (trimmed.startsWith("+") || trimmed.all { it.isDigit() || it == '-' || it == ' ' }) {
            try {
                val phoneSnap = db.collection("users")
                    .whereEqualTo("phoneNumber", trimmed)
                    .limit(5)
                    .get()
                    .await()
                for (doc in phoneSnap.documents) {
                    val user = doc.toObject(User::class.java) ?: continue
                    if (user.uid == currentUid) continue
                    if (!results.any { it.uid == user.uid }) results.add(user)
                }
            } catch (e: Exception) {
                Timber.w(e, "searchUsers: phone query failed")
            }
        }

        // Sort results client-side by name for consistent ordering
        results.sortBy { it.name?.lowercase() ?: "" }
        return results
    }

    suspend fun addFriend(friendId: String): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            val batch = db.batch()
            batch.set(
                db.collection("users").document(currentUid).collection("friends").document(friendId),
                mapOf("addedAt" to Timestamp.now())
            )
            batch.set(
                db.collection("users").document(friendId).collection("friends").document(currentUid),
                mapOf("addedAt" to Timestamp.now())
            )
            batch.commit().await()
            true
        } catch (e: Exception) {
            Timber.e(e, "addFriend failed")
            false
        }
    }

    suspend fun removeFriend(friendId: String): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            val batch = db.batch()
            batch.delete(db.collection("users").document(currentUid).collection("friends").document(friendId))
            batch.delete(db.collection("users").document(friendId).collection("friends").document(currentUid))
            batch.commit().await()
            true
        } catch (e: Exception) {
            Timber.e(e, "removeFriend failed")
            false
        }
    }

    suspend fun getFriends(): List<User> {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        return try {
            val friendDocs = db.collection("users").document(currentUid)
                .collection("friends").get().await()
            val friendIds = friendDocs.documents.map { it.id }
            if (friendIds.isEmpty()) return emptyList()
            // Batch fetch friend profiles (max 30 per whereIn)
            val users = mutableListOf<User>()
            friendIds.chunked(30).forEach { chunk ->
                val snap = db.collection("users").whereIn("uid", chunk).get().await()
                users.addAll(snap.toObjects(User::class.java))
            }
            users
        } catch (e: Exception) {
            Timber.e(e, "getFriends failed")
            emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BLOCKING
    // ═══════════════════════════════════════════════════════════════════════════
    suspend fun blockUser(blockedId: String): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            db.collection("users").document(currentUid).collection("blocked").document(blockedId)
                .set(mapOf("blockedAt" to Timestamp.now())).await()
            // Also remove from friends
            removeFriend(blockedId)
            true
        } catch (e: Exception) {
            Timber.e(e, "blockUser failed")
            false
        }
    }

    suspend fun unblockUser(blockedId: String): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            db.collection("users").document(currentUid).collection("blocked").document(blockedId)
                .delete().await()
            true
        } catch (e: Exception) {
            Timber.e(e, "unblockUser failed")
            false
        }
    }

    suspend fun getBlockedUsers(): List<String> {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        return try {
            val docs = db.collection("users").document(currentUid)
                .collection("blocked").get().await()
            docs.documents.map { it.id }
        } catch (e: Exception) {
            Timber.e(e, "getBlockedUsers failed")
            emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATABASE SEEDING — only runs once when collections are empty
    // ═══════════════════════════════════════════════════════════════════════════
    fun seedDatabaseIfEmpty() {
        // Seed News
        db.collection("news").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val seedNews = listOf(
                    NewsArticle("", "Dengue", "Dawn News", "2h ago", "Dengue Cases Surge in Punjab", "Health authorities report a 40% increase in dengue cases across Punjab province.", "Lahore, Punjab", "CRITICAL"),
                    NewsArticle("", "COVID-19", "Geo News", "4h ago", "New COVID Variant Detected in Karachi", "Genomic sequencing confirms presence of new sub-variant in Sindh province.", "Karachi, Sindh", "WARNING"),
                    NewsArticle("", "Flood", "ARY News", "6h ago", "Flood Warning Issued for Southern Sindh", "NDMA issues high alert for districts along the Indus River.", "Hyderabad, Sindh", "WARNING"),
                    NewsArticle("", "Polio", "The News", "8h ago", "Polio Vaccination Drive Begins", "Government launches nationwide immunization campaign targeting children under 5.", "Islamabad", "INFO"),
                    NewsArticle("", "Economy", "Express Tribune", "12h ago", "Medicine Prices Rise by 15% Nationwide", "Pharmaceutical companies cite raw material cost increase as primary factor.", "Karachi, Sindh", "INFO")
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
        // Clean up legacy filler/seed chats that were auto-created in earlier builds.
        // These 3 IDs were never real user-created groups — remove them permanently.
        listOf("dengue_lahore", "flood_sindh", "bimari_ai").forEach { chatId ->
            val chatRef = db.collection("chats").document(chatId)
            chatRef.get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    chatRef.collection("messages").get().addOnSuccessListener { msgs ->
                        msgs.documents.forEach { it.reference.delete() }
                    }
                    chatRef.delete()
                    Timber.d("Removed legacy seed chat: $chatId")
                }
            }
        }
    }
}
