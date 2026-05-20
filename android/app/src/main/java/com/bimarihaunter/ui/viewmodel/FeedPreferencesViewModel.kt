package com.bimarihaunter.ui.viewmodel

/*
 * FeedPreferencesViewModel.kt
 *
 * Responsibilities:
 *   1. Load the user's existing tag preferences from Firestore on init
 *   2. Expose a mutable selectedTags set for the UI
 *   3. On savePreferences():
 *        a. Write tags to Firestore users/{uid}.feed_tags
 *        b. POST to backend /api/v1/users/{uid}/preferences so the scraper
 *           knows which RSS queries to run for this user
 *        c. Emit SaveStatus.Success so the screen navigates away
 *
 * The backend /preferences endpoint already exists (users.py) and accepts
 * a UserPreferencesRequest with a `diseases` list. We extend the request
 * to also carry `feed_tags` (all selected tag IDs). The backend scheduler
 * will use these tags to build targeted Google News RSS queries.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.bimarihaunter.network.RetrofitClient
import com.bimarihaunter.network.UserPreferencesRequest

class FeedPreferencesViewModel : ViewModel() {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    init {
        loadExistingTags()
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    private fun loadExistingTags() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val doc = firestore.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                val saved = doc.get("feed_tags") as? List<String> ?: emptyList()
                _selectedTags.value = saved.toSet()
            } catch (e: Exception) {
                Timber.w(e, "Could not load existing feed tags")
            }
        }
    }

    // ── Toggle ─────────────────────────────────────────────────────────────────

    fun toggleTag(tagId: String) {
        val current = _selectedTags.value.toMutableSet()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        _selectedTags.value = current
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    fun savePreferences() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _saveStatus.value = SaveStatus.Error("Not signed in. Please log in again.")
            return
        }
        val tags = _selectedTags.value.toList()
        if (tags.isEmpty()) {
            _saveStatus.value = SaveStatus.Error("Please select at least one topic.")
            return
        }

        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving

            // 1. Write to Firestore immediately (offline-safe)
            try {
                firestore.collection("users")
                    .document(uid)
                    .set(mapOf("feed_tags" to tags), SetOptions.merge())
                    .await()
                Timber.d("Feed tags saved to Firestore: $tags")
            } catch (e: Exception) {
                Timber.e(e, "Failed to write feed_tags to Firestore")
                _saveStatus.value = SaveStatus.Error("Could not save preferences. Check your connection.")
                return@launch
            }

            // 2. Notify backend so it can trigger a targeted scrape
            //    We derive disease-specific tags from the full tag list
            try {
                val diseaseTagIds = setOf(
                    "dengue", "malaria", "covid", "influenza", "mpox", "measles",
                    "polio", "hepatitis", "tuberculosis", "cholera", "typhoid", "rabies",
                    "outbreak", "pandemic", "zoonotic", "antimicrobial",
                )
                val diseaseTags = tags.filter { it in diseaseTagIds }

                // Get last known city/coords from Firestore user profile
                val userDoc = firestore.collection("users").document(uid).get().await()
                val city    = (userDoc.get("live_location.city") as? String) ?: "Karachi"
                val lat     = (userDoc.get("live_location.coordinates.latitude") as? Number)?.toDouble() ?: 24.8607
                val lon     = (userDoc.get("live_location.coordinates.longitude") as? Number)?.toDouble() ?: 67.0011

                RetrofitClient.apiService.updatePreferences(
                    uid = uid,
                    body = UserPreferencesRequest(
                        diseases  = diseaseTags.ifEmpty { listOf("outbreak") },
                        feed_tags = tags,
                        city      = city,
                        latitude  = lat,
                        longitude = lon,
                    )
                )
                Timber.d("Backend preferences updated successfully")
            } catch (e: Exception) {
                // Non-fatal — Firestore write already succeeded, feed will update on next sync
                Timber.w(e, "Backend preferences call failed (non-fatal)")
            }

            _saveStatus.value = SaveStatus.Success
        }
    }

    // ── Status ─────────────────────────────────────────────────────────────────

    sealed class SaveStatus {
        object Idle    : SaveStatus()
        object Saving  : SaveStatus()
        object Success : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }
}

class FeedPreferencesViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FeedPreferencesViewModel() as T
    }
}
