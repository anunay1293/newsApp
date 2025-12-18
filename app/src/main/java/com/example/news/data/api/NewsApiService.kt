package com.example.news.data.api

import com.example.news.data.dto.FeedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for AWS API Gateway news feed endpoint.
 */
interface NewsApiService {
    @GET("feed")
    suspend fun getFeed(
        @Query("category") category: String
    ): FeedResponseDto
}

