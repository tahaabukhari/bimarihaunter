package com.bimarihaunter.data.repository

import com.bimarihaunter.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * Wraps all Firestore read/write operations for the community and auth layers.
 */
class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    // ─────────────────────────── User Profile ────────────────────────────────

    fun getUserProfile(uid: String, callback: (User?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                callback(if (doc.exists()) doc.toObject(User::class.java) else null)
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Failed to fetch user profile for $uid")
                callback(null)
            }
    }

    fun saveUserProfile(user: User) {
        db.collection("users").document(user.uid)
            .set(user)
            .addOnFailureListener { e -> Timber.e(e, "Failed to save user profile") }
    }

    // ─────────────────────────── News / Articles ──────────────────────────────

    fun getNews(): Flow<List<NewsArticle>> = callbackFlow {
        val listener = db.collection("news")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) { Timber.e(err, "getNews error"); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(NewsArticle::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ─────────────────────────── Alerts ──────────────────────────────────────

    fun getAlerts(): Flow<List<AlertData>> = callbackFlow {
        val listener = db.collection("alerts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, err ->
                if (err != null) { Timber.e(err, "getAlerts error"); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(AlertData::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ─────────────────────────── Community Chat ───────────────────────────────

    fun getChatGroups(): Flow<List<ChatGroup>> = callbackFlow {
        val listener = db.collection("chat_groups")
            .addSnapshotListener { snap, err ->
                if (err != null) { Timber.e(err, "getChatGroups error"); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toObject(ChatGroup::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(chatGroupId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chat_groups")
            .document(chatGroupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { Timber.e(err, "getMessages error"); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun sendMessage(chatGroupId: String, message: Message) {
        db.collection("chat_groups")
            .document(chatGroupId)
            .collection("messages")
            .add(message)
            .addOnFailureListener { e -> Timber.e(e, "Failed to send message") }
    }
}
