package com.bimarihaunter.data.api

import com.bimarihaunter.data.models.ArticleDto
import com.bimarihaunter.data.models.InsightsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BimarihaunterApi {
    @GET("api/v1/feed")
    suspend fun getFeed(
        @Query("region") region: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): List<ArticleDto>

    @GET("api/v1/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("region") region: String? = null,
        @Query("category") category: String? = null
    ): List<ArticleDto>

    @GET("api/v1/insights/{region}")
    suspend fun getRegionInsights(
        @Path("region") region: String
    ): InsightsDto
}
