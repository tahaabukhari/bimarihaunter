package com.bimarihaunter.data.network

import com.bimarihaunter.data.model.Report
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    
    // Fetch all reports
    @GET("api/v1/feed/")
    suspend fun getFeed(
        @Query("limit") limit: Int = 50
    ): List<Report>
    
    // Search reports by keyword
    @GET("api/v1/feed/search")
    suspend fun searchFeed(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50
    ): List<Report>
    
    // Filter reports by disease and severity
    @GET("api/v1/feed/filter")
    suspend fun filterFeed(
        @Query("disease") disease: String? = null,
        @Query("severity") severity: String? = null,
        @Query("limit") limit: Int = 50
    ): List<Report>
}
