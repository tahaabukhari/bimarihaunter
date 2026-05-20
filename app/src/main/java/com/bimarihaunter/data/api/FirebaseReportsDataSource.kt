package com.bimarihaunter.data.api

import com.bimarihaunter.data.mock.MOCK_OUTBREAK_LOCATIONS
import com.bimarihaunter.data.models.OutbreakLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Isolated Firebase data source for Live Map integration.
 * If Firebase is added to the project, this class will handle real-time listeners.
 */
class FirebaseReportsDataSource {

    // TODO: When Firebase is configured, inject FirebaseFirestore here.
    // private val firestore = Firebase.firestore

    /**
     * Returns a Flow of OutbreakLocations.
     * Uses mock data fallback since Firebase is currently not configured.
     */
    fun getReportsStream(): Flow<List<OutbreakLocation>> = flow {
        // Mocking network delay
        delay(1000)
        
        // Return mock fallback
        emit(MOCK_OUTBREAK_LOCATIONS)
        
        // TODO: Real Firebase Implementation
        // return callbackFlow {
        //     val listener = firestore.collection("reports")
        //         .whereEqualTo("status", "analyzed")
        //         .orderBy("published_at")
        //         .limit(100)
        //         .addSnapshotListener { snapshot, error ->
        //             if (error != null) {
        //                 close(error)
        //                 return@addSnapshotListener
        //             }
        //             if (snapshot != null) {
        //                 val locations = snapshot.documents.mapNotNull { doc ->
        //                     // Parse fields safely with defaults
        //                     OutbreakLocation(
        //                         id = doc.id,
        //                         city = doc.getString("city") ?: "Unknown",
        //                         latitude = doc.getDouble("latitude") ?: 0.0,
        //                         longitude = doc.getDouble("longitude") ?: 0.0,
        //                         disease = doc.getString("disease") ?: "Unknown",
        //                         count = doc.getLong("count")?.toInt() ?: 1,
        //                         severity = doc.getString("severity") ?: "low",
        //                         timestamp = doc.getString("published_at") ?: ""
        //                     )
        //                 }
        //                 trySend(locations)
        //             }
        //         }
        //     awaitClose { listener.remove() }
        // }
    }
}
