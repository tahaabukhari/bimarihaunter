package com.bimarihaunter.repository

import com.bimarihaunter.db.BimarihaunterDatabase
import com.bimarihaunter.db.OutbreakReportEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class FeedRepository(private val database: BimarihaunterDatabase) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dao = database.outbreakReportDao()

    // Get cached feed as Flow (offline-first)
    fun getCachedFeed(): Flow<List<OutbreakReportEntity>> {
        return dao.getAllReports()
    }

    // Sync feed from Firestore user feed collection
    suspend fun syncFeed(city: String, latitude: Double, longitude: Double) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Timber.w("Feed sync skipped because user is not authenticated.")
            return
        }

        try {
            updateUserLocation(uid, city, latitude, longitude)

            var entities = fetchUserFeed(uid)
            if (entities.isEmpty()) {
                Timber.d("No user-specific Firestore feed found for $uid, trying root feed collection.")
                entities = fetchRootFeed()
            }

            if (entities.isEmpty()) {
                Timber.d("Root feed collection empty, trying news fallback.")
                entities = fetchNewsFallback()
            }

            if (entities.isEmpty()) {
                Timber.d("No Firestore feed items found for user $uid, seeding default feed...")
                
                // Seed mock personalized feed for this user
                val mockReport1 = mapOf(
                    "title" to "Dengue Outbreak Alert in $city",
                    "source" to "Local Health Dept",
                    "url" to "https://example.com/dengue",
                    "raw_text" to "Multiple cases of dengue have been reported in the $city area. Health departments are advising citizens to clear standing water.",
                    "published_at" to com.google.firebase.Timestamp.now(),
                    "scraped_at" to com.google.firebase.Timestamp.now(),
                    "ai_analysis" to mapOf(
                        "disease" to "Dengue",
                        "severity" to "CRITICAL",
                        "summary" to listOf("High mosquito breeding observed", "Hospitals on alert in $city"),
                        "locations" to listOf(city),
                        "coordinates" to GeoPoint(latitude, longitude),
                        "confidence_score" to 0.95
                    )
                )
                
                val mockReport2 = mapOf(
                    "title" to "Routine Checkup Advisory",
                    "source" to "Ministry of Health",
                    "url" to "https://example.com/checkup",
                    "raw_text" to "Routine vaccinations are available at all major hospitals in $city.",
                    "published_at" to com.google.firebase.Timestamp.now(),
                    "scraped_at" to com.google.firebase.Timestamp.now(),
                    "ai_analysis" to mapOf(
                        "disease" to "General",
                        "severity" to "INFO",
                        "summary" to listOf("Vaccination drives active", "Free checkups in $city clinics"),
                        "locations" to listOf(city),
                        "coordinates" to GeoPoint(latitude + 0.01, longitude + 0.01),
                        "confidence_score" to 0.99
                    )
                )

                val feedRef = firestore.collection("users").document(uid).collection("feed")
                feedRef.add(mockReport1).await()
                feedRef.add(mockReport2).await()
                
                // Fetch the newly seeded feed
                val newSnapshot = feedRef
                    .orderBy("published_at", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                    
                entities = newSnapshot.documents.mapNotNull { documentToEntity(it) }
                
                if (entities.isEmpty()) {
                    return
                }
            }

            withContext(Dispatchers.IO) {
                dao.insertAll(entities)
                val count = dao.getReportCount()
                if (count > 50) {
                    dao.trimReports(50)
                }
            }

            Timber.d("Firestore feed synced successfully: ${entities.size} items cached")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync Firestore feed")
            // Gracefully fallback to cached data
        }
    }

    private suspend fun updateUserLocation(uid: String, city: String, latitude: Double, longitude: Double) {
        firestore.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "live_location" to mapOf(
                        "city" to city,
                        "coordinates" to GeoPoint(latitude, longitude)
                    ),
                    "updated_at" to com.google.firebase.Timestamp.now()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun fetchUserFeed(uid: String): List<OutbreakReportEntity> {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("feed")
            .orderBy("published_at", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        return snapshot.documents.mapNotNull { documentToEntity(it) }
    }

    private suspend fun fetchRootFeed(): List<OutbreakReportEntity> {
        val snapshot = firestore.collection("feed")
            .orderBy("published_at", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        return snapshot.documents.mapNotNull { documentToEntity(it) }
    }

    private suspend fun fetchNewsFallback(): List<OutbreakReportEntity> {
        val snapshot = firestore.collection("news")
            .limit(50)
            .get()
            .await()

        return snapshot.documents.mapNotNull { newsDocumentToEntity(it) }
    }

    private fun newsDocumentToEntity(document: DocumentSnapshot): OutbreakReportEntity? {
        val data = document.data ?: return null
        val title = data["title"] as? String ?: return null
        val source = data["source"] as? String ?: "Unknown"
        val rawText = data["content"] as? String ?: data["description"] as? String ?: ""
        val publishedAt = data["time"] as? String ?: data["published_at"] as? String ?: ""
        val scrapedAt = data["time"] as? String ?: ""
        val disease = (data["category"] as? String ?: "Health").lowercase()
        val severity = (data["urgency"] as? String ?: "medium").lowercase()
        val summaryText = if (rawText.isNotBlank()) listOf(rawText.take(120)) else emptyList()
        val locations = listOfNotNull((data["location"] as? String)?.takeIf { it.isNotBlank() })
        val url = data["url"] as? String ?: ""

        return OutbreakReportEntity(
            id = document.id,
            title = title,
            source = source,
            url = url,
            raw_text = rawText,
            published_at = publishedAt,
            scraped_at = scrapedAt,
            disease = disease,
            severity = severity,
            summary = summaryText,
            locations = locations,
            latitude = 0.0,
            longitude = 0.0,
            confidence_score = 0.0
        )
    }

    // Get reports by disease
    fun getReportsByDisease(disease: String): Flow<List<OutbreakReportEntity>> {
        return dao.getReportsByDisease(disease)
    }

    private fun documentToEntity(document: DocumentSnapshot): OutbreakReportEntity? {
        val data = document.data ?: return null

        val title = data["title"] as? String ?: return null
        val source = data["source"] as? String ?: "Unknown"
        val url = data["url"] as? String ?: ""
        val rawText = data["raw_text"] as? String ?: ""
        val publishedAt = when (val value = data["published_at"]) {
            is com.google.firebase.Timestamp -> value.toDate().toInstant().toString()
            is String -> value
            else -> ""
        }
        val scrapedAt = when (val value = data["scraped_at"]) {
            is com.google.firebase.Timestamp -> value.toDate().toInstant().toString()
            is String -> value
            else -> ""
        }

        val aiAnalysis = data["ai_analysis"] as? Map<*, *>
        val disease = aiAnalysis?.get("disease") as? String ?: "Unknown"
        val severity = aiAnalysis?.get("severity") as? String ?: "medium"
        val summary = (aiAnalysis?.get("summary") as? List<*>)
            ?.mapNotNull { it as? String } ?: emptyList()
        val locations = (aiAnalysis?.get("locations") as? List<*>)
            ?.mapNotNull { it as? String } ?: emptyList()
        val coordinates = geoPointFromValue(aiAnalysis?.get("coordinates"))

        return OutbreakReportEntity(
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
            confidence_score = (aiAnalysis?.get("confidence_score") as? Number)?.toDouble() ?: 0.0
        )
    }

    private fun geoPointFromValue(value: Any?): GeoPoint? {
        return when (value) {
            is GeoPoint -> value
            is Map<*, *> -> {
                val lat = (value["latitude"] ?: value["_latitude"]) as? Number
                val lon = (value["longitude"] ?: value["_longitude"]) as? Number
                if (lat != null && lon != null) {
                    GeoPoint(lat.toDouble(), lon.toDouble())
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
