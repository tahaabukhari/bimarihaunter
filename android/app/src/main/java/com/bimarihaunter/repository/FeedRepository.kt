package com.bimarihaunter.repository

import com.bimarihaunter.db.BimarihaunterDatabase
import com.bimarihaunter.db.OutbreakReportEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.bimarihaunter.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

// ═══════════════════════════════════════════════════════════════════════════
// FeedRepository — BULLETPROOF VERSION v3
//
// CRITICAL FIXES:
// 1. ALL orderBy() calls removed — they require Firestore composite indexes.
// 2. Reads /reports FIRST (where the backend scraper writes).
// 3. triggerJob() has a 5-second timeout so it never blocks the sync.
// 4. syncFeed() does NOT require authentication — if user is null, it still
//    fetches from /reports (the global collection).
// 5. Every Firestore call is individually try/caught.
// 6. Client-side sorting after fetch.
// ═══════════════════════════════════════════════════════════════════════════
class FeedRepository(private val database: BimarihaunterDatabase) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dao = database.outbreakReportDao()

    // ─── Public API ─────────────────────────────────────────────────────────

    /** Returns the Room-cached feed as a live Flow (offline-first). */
    fun getCachedFeed(): Flow<List<OutbreakReportEntity>> = dao.getAllReports()

    /** Returns reports filtered by disease from Room cache. */
    fun getReportsByDisease(disease: String): Flow<List<OutbreakReportEntity>> =
        dao.getReportsByDisease(disease)

    /**
     * Syncs Firestore → Room.
     * Priority: /reports (global) → users/{uid}/feed → /news seeds → mock
     * Never throws — all errors are caught and logged.
     * Does NOT require authentication — fetches /reports even if user is null.
     */
    suspend fun syncFeed(city: String, latitude: Double, longitude: Double) {
        val uid = auth.currentUser?.uid

        // Trigger backend scraper with a short timeout — truly non-blocking
        try {
            withTimeoutOrNull(5000L) {
                RetrofitClient.apiService.triggerJob()
            }
            Timber.d("Backend scraper triggered.")
        } catch (e: Exception) {
            Timber.w(e, "Backend trigger failed — continuing sync.")
        }

        // Update user location in Firestore (non-fatal, skip if not logged in)
        if (!uid.isNullOrBlank()) {
            try {
                updateUserLocation(uid, city, latitude, longitude)
            } catch (e: Exception) {
                Timber.w(e, "updateUserLocation failed — continuing sync.")
            }
        }

        var entities: List<OutbreakReportEntity> = emptyList()

        // 1. Try /reports FIRST — this is where the scraper writes (global, no auth needed)
        if (entities.isEmpty()) {
            try {
                entities = fetchReports()
                Timber.d("fetchReports returned ${entities.size} items.")
            } catch (e: Exception) {
                Timber.w(e, "fetchReports failed.")
            }
        }

        // 2. Try users/{uid}/feed — personal feed
        if (entities.isEmpty() && !uid.isNullOrBlank()) {
            try {
                entities = fetchUserFeed(uid)
                Timber.d("fetchUserFeed returned ${entities.size} items.")
            } catch (e: Exception) {
                Timber.w(e, "fetchUserFeed failed.")
            }
        }

        // 3. Try /news seed collection
        if (entities.isEmpty()) {
            try {
                entities = fetchNewsFallback()
                Timber.d("fetchNewsFallback returned ${entities.size} items.")
            } catch (e: Exception) {
                Timber.w(e, "fetchNewsFallback failed.")
            }
        }

        // 4. If still empty and user is logged in, seed mock items
        if (entities.isEmpty() && !uid.isNullOrBlank()) {
            Timber.d("All Firestore sources empty — seeding mock feed for $uid.")
            try {
                val feedRef = firestore.collection("users").document(uid).collection("feed")
                feedRef.add(mockReport(city, latitude, longitude, "Dengue", "CRITICAL")).await()
                feedRef.add(mockReport(city, latitude + 0.01, longitude + 0.01, "General", "INFO")).await()
                val snap = feedRef.limit(10).get().await()
                entities = snap.documents.mapNotNull { documentToEntity(it) }
            } catch (e: Exception) {
                Timber.e(e, "Mock seed failed.")
            }
        }

        if (entities.isEmpty()) {
            Timber.w("syncFeed: all sources exhausted, nothing to cache.")
            return
        }

        // Sort client-side by published_at descending
        val sorted = entities.sortedByDescending { it.published_at }

        withContext(Dispatchers.IO) {
            dao.insertAll(sorted)
            val count = dao.getReportCount()
            if (count > 100) dao.trimReports(100)
        }
        Timber.d("Feed synced: ${sorted.size} items cached in Room.")
    }

    // ─── Private fetch helpers ───────────────────────────────────────────────

    private suspend fun updateUserLocation(uid: String, city: String, lat: Double, lon: Double) {
        firestore.collection("users").document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "live_location" to mapOf(
                        "city" to city,
                        "coordinates" to GeoPoint(lat, lon)
                    ),
                    "updated_at" to com.google.firebase.Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
    }

    /**
     * Reads users/{uid}/feed — NO orderBy, NO index required.
     */
    private suspend fun fetchUserFeed(uid: String): List<OutbreakReportEntity> {
        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("feed")
            .limit(100)   // ← NO orderBy
            .get()
            .await()
        return snapshot.documents.mapNotNull { documentToEntity(it) }
    }

    /**
     * Reads /reports — the collection the backend scraper writes to.
     * NO orderBy, NO index required.
     */
    private suspend fun fetchReports(): List<OutbreakReportEntity> {
        val snapshot = firestore
            .collection("reports")
            .limit(100)   // ← NO orderBy
            .get()
            .await()
        return snapshot.documents.mapNotNull { documentToEntity(it) }
    }

    /**
     * Reads /news — the seed collection used when no scraped data exists.
     */
    private suspend fun fetchNewsFallback(): List<OutbreakReportEntity> {
        val snapshot = firestore
            .collection("news")
            .limit(50)
            .get()
            .await()
        return snapshot.documents.mapNotNull { newsDocumentToEntity(it) }
    }

    // ─── Document → Entity mappers ───────────────────────────────────────────

    /**
     * Maps a /reports or users/{uid}/feed document to OutbreakReportEntity.
     * Handles the ai_analysis nested map as shown in the Firestore screenshot.
     */
    private fun documentToEntity(document: DocumentSnapshot): OutbreakReportEntity? {
        return try {
            val data = document.data ?: return null
            val title = data["title"] as? String ?: return null
            if (title.isBlank()) return null

            val source = data["source"] as? String ?: "BimariHaunter"
            val url = data["url"] as? String ?: ""
            val rawText = data["raw_text"] as? String ?: data["content"] as? String ?: ""

            val publishedAt = when (val v = data["published_at"]) {
                is com.google.firebase.Timestamp -> v.toDate().toInstant().toString()
                is String -> v
                else -> ""
            }
            val scrapedAt = when (val v = data["scraped_at"]) {
                is com.google.firebase.Timestamp -> v.toDate().toInstant().toString()
                is String -> v
                else -> publishedAt
            }

            // ai_analysis nested map — matches the Firestore screenshot exactly
            val ai = data["ai_analysis"] as? Map<*, *>

            val disease = (ai?.get("disease") as? String
                ?: data["category"] as? String
                ?: "Health")

            val severity = (ai?.get("severity") as? String
                ?: data["severity"] as? String
                ?: "medium")

            val summary: List<String> = when (val s = ai?.get("summary")) {
                is List<*> -> s.mapNotNull { it as? String }
                is String -> if (s.isNotBlank()) listOf(s) else emptyList()
                else -> if (rawText.isNotBlank()) listOf(rawText.take(150)) else emptyList()
            }

            val locations: List<String> = when (val l = ai?.get("locations")) {
                is List<*> -> l.mapNotNull { it as? String }
                is String -> if (l.isNotBlank()) listOf(l) else listOf("Pakistan")
                else -> listOf("Pakistan")
            }

            val coordinates = geoPointFromValue(ai?.get("coordinates"))

            OutbreakReportEntity(
                id = document.id,
                title = title,
                source = source,
                url = url,
                raw_text = rawText,
                published_at = publishedAt,
                scraped_at = scrapedAt,
                disease = disease,
                severity = severity,
                summary = summary,
                locations = locations,
                latitude = coordinates?.latitude ?: 0.0,
                longitude = coordinates?.longitude ?: 0.0,
                confidence_score = (ai?.get("confidence_score") as? Number)?.toDouble() ?: 0.0
            )
        } catch (e: Exception) {
            Timber.e(e, "documentToEntity failed for doc ${document.id}")
            null
        }
    }

    private fun newsDocumentToEntity(document: DocumentSnapshot): OutbreakReportEntity? {
        return try {
            val data = document.data ?: return null
            val title = data["title"] as? String ?: return null
            if (title.isBlank()) return null

            val source = data["source"] as? String ?: "Unknown"
            val rawText = data["content"] as? String ?: data["description"] as? String ?: ""
            val publishedAt = data["time"] as? String ?: data["published_at"] as? String ?: ""
            val disease = (data["category"] as? String ?: "Health").lowercase()
            val severity = (data["urgency"] as? String ?: "medium").lowercase()
            val summaryText = if (rawText.isNotBlank()) listOf(rawText.take(120)) else emptyList()
            val locations = listOfNotNull((data["location"] as? String)?.takeIf { it.isNotBlank() })
            val url = data["url"] as? String ?: ""

            OutbreakReportEntity(
                id = document.id,
                title = title,
                source = source,
                url = url,
                raw_text = rawText,
                published_at = publishedAt,
                scraped_at = publishedAt,
                disease = disease,
                severity = severity,
                summary = summaryText,
                locations = locations,
                latitude = 0.0,
                longitude = 0.0,
                confidence_score = 0.0
            )
        } catch (e: Exception) {
            Timber.e(e, "newsDocumentToEntity failed for doc ${document.id}")
            null
        }
    }

    private fun geoPointFromValue(value: Any?): GeoPoint? {
        return when (value) {
            is GeoPoint -> value
            is Map<*, *> -> {
                val lat = (value["latitude"] ?: value["_latitude"]) as? Number
                val lon = (value["longitude"] ?: value["_longitude"]) as? Number
                if (lat != null && lon != null) GeoPoint(lat.toDouble(), lon.toDouble()) else null
            }
            else -> null
        }
    }

    private fun mockReport(
        city: String, lat: Double, lon: Double, disease: String, severity: String
    ) = mapOf(
        "title" to if (disease == "General") "Routine Health Advisory — $city"
                   else "$disease Alert — $city",
        "source" to "BimariHaunter",
        "url" to "",
        "raw_text" to "Health advisory for $city residents. Stay informed and take precautions.",
        "published_at" to com.google.firebase.Timestamp.now(),
        "scraped_at" to com.google.firebase.Timestamp.now(),
        "ai_analysis" to mapOf(
            "disease" to disease,
            "severity" to severity,
            "summary" to listOf("Advisory issued for $city"),
            "locations" to listOf(city),
            "coordinates" to GeoPoint(lat, lon),
            "confidence_score" to 0.9,
            "model_used" to "mock"
        )
    )
}
