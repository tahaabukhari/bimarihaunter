package com.bimarihaunter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromEmail: String = "",
    val toUid: String = "",
    val status: String = "pending",  // "pending" | "accepted" | "rejected"
    val createdAt: Long = System.currentTimeMillis()
)

class AddFriendsViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUid get() = auth.currentUser?.uid ?: ""

    private val _searchResults    = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _myFriends        = MutableStateFlow<List<User>>(emptyList())
    val myFriends: StateFlow<List<User>> = _myFriends.asStateFlow()

    private val _pendingRequests  = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests.asStateFlow()

    private val _isLoading        = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackMessage     = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    init {
        loadMyFriends()
        loadPendingRequests()
    }

    // ─────────────────────────── search ──────────────────────────────────────

    fun searchUsers(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = mutableListOf<User>()
                val trimmed = query.trim()

                // Strategy 1: lowercase range query (NO orderBy, NO index needed)
                val lower = trimmed.lowercase()
                try {
                    val snap = db.collection("users")
                        .whereGreaterThanOrEqualTo("name", lower)
                        .whereLessThanOrEqualTo("name", lower + "\uf8ff")
                        .limit(20)
                        .get()
                        .await()
                    for (doc in snap.documents) {
                        val user = doc.toObject(User::class.java) ?: continue
                        if (user.uid != currentUid && results.none { it.uid == user.uid })
                            results.add(user)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "searchUsers: lowercase range failed")
                }

                // Strategy 2: title-case range (covers "Ali Khan" style names)
                val titleCase = trimmed.replaceFirstChar { it.uppercase() }
                try {
                    val snap = db.collection("users")
                        .whereGreaterThanOrEqualTo("name", titleCase)
                        .whereLessThanOrEqualTo("name", titleCase + "\uf8ff")
                        .limit(20)
                        .get()
                        .await()
                    for (doc in snap.documents) {
                        val user = doc.toObject(User::class.java) ?: continue
                        if (user.uid != currentUid && results.none { it.uid == user.uid })
                            results.add(user)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "searchUsers: title-case range failed")
                }

                // Strategy 3: email exact match
                if (trimmed.contains("@")) {
                    try {
                        val snap = db.collection("users")
                            .whereEqualTo("email", trimmed)
                            .limit(5)
                            .get()
                            .await()
                        for (doc in snap.documents) {
                            val user = doc.toObject(User::class.java) ?: continue
                            if (user.uid != currentUid && results.none { it.uid == user.uid })
                                results.add(user)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "searchUsers: email query failed")
                    }
                }

                // Strategy 4: phone number exact match
                if (trimmed.startsWith("+") || trimmed.all { it.isDigit() || it == '-' || it == ' ' }) {
                    try {
                        val snap = db.collection("users")
                            .whereEqualTo("phoneNumber", trimmed)
                            .limit(5)
                            .get()
                            .await()
                        for (doc in snap.documents) {
                            val user = doc.toObject(User::class.java) ?: continue
                            if (user.uid != currentUid && results.none { it.uid == user.uid })
                                results.add(user)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "searchUsers: phone query failed")
                    }
                }

                // Sort results client-side
                results.sortBy { it.name.lowercase() }
                _searchResults.value = results
            } catch (e: Exception) {
                Timber.e(e, "User search failed")
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─────────────────────────── friends list ────────────────────────────────

    private fun loadMyFriends() {
        if (currentUid.isBlank()) return
        viewModelScope.launch {
            try {
                val snap = db.collection("users")
                    .document(currentUid)
                    .collection("friends")
                    .get()
                    .await()

                val friendUids = snap.documents.map { it.id }
                if (friendUids.isEmpty()) { _myFriends.value = emptyList(); return@launch }

                // Batch fetch user profiles (Firestore IN supports up to 10)
                val users = mutableListOf<User>()
                friendUids.chunked(10).forEach { chunk ->
                    val usersSnap = db.collection("users")
                        .whereIn("uid", chunk)
                        .get()
                        .await()
                    users += usersSnap.documents.mapNotNull { it.toObject(User::class.java) }
                }
                _myFriends.value = users
            } catch (e: Exception) {
                Timber.e(e, "Failed to load friends")
            }
        }
    }

    // ─────────────────────────── pending requests ────────────────────────────

    private fun loadPendingRequests() {
        if (currentUid.isBlank()) return
        viewModelScope.launch {
            try {
                val snap = db.collection("friend_requests")
                    .whereEqualTo("toUid", currentUid)
                    .whereEqualTo("status", "pending")
                    .get()
                    .await()

                _pendingRequests.value = snap.documents.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load friend requests")
            }
        }
    }

    // ─────────────────────────── actions ─────────────────────────────────────

    fun sendFriendRequest(targetUser: User) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val request = hashMapOf(
                    "fromUid"   to currentUid,
                    "fromName"  to (currentUser.displayName ?: "User"),
                    "fromEmail" to (currentUser.email ?: ""),
                    "toUid"     to targetUser.uid,
                    "status"    to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("friend_requests").add(request).await()
                _snackMessage.value = "Friend request sent to ${targetUser.name}!"
            } catch (e: Exception) {
                Timber.e(e, "sendFriendRequest failed")
                _snackMessage.value = "Failed to send request. Please try again."
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                // Mark request accepted
                db.collection("friend_requests").document(request.id)
                    .update("status", "accepted").await()

                // Write bi-directional friend links
                db.collection("users").document(currentUid)
                    .collection("friends").document(request.fromUid)
                    .set(mapOf("since" to System.currentTimeMillis())).await()
                db.collection("users").document(request.fromUid)
                    .collection("friends").document(currentUid)
                    .set(mapOf("since" to System.currentTimeMillis())).await()

                _snackMessage.value = "You are now friends with ${request.fromName}!"
                loadPendingRequests()
                loadMyFriends()
            } catch (e: Exception) {
                Timber.e(e, "acceptFriendRequest failed")
                _snackMessage.value = "Failed to accept request."
            }
        }
    }

    fun rejectFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                db.collection("friend_requests").document(request.id)
                    .update("status", "rejected").await()
                _snackMessage.value = "Request declined."
                loadPendingRequests()
            } catch (e: Exception) {
                Timber.e(e, "rejectFriendRequest failed")
            }
        }
    }

    fun clearSnack() { _snackMessage.value = null }
}
