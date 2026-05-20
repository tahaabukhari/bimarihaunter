package com.bimarihaunter.data.repository

import com.bimarihaunter.data.model.Report
import com.bimarihaunter.data.network.NetworkClient

class FeedRepository {
    
    suspend fun getFeed(limit: Int = 50): Result<List<Report>> {
        return try {
            val reports = NetworkClient.apiService.getFeed(limit)
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchFeed(query: String, limit: Int = 50): Result<List<Report>> {
        return try {
            val reports = NetworkClient.apiService.searchFeed(query, limit)
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun filterFeed(
        disease: String? = null,
        severity: String? = null,
        limit: Int = 50
    ): Result<List<Report>> {
        return try {
            val reports = NetworkClient.apiService.filterFeed(disease, severity, limit)
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
