package com.example.news.data.api

import com.example.news.data.dto.TopHeadlinesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for NewsAPI.
 */
interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("category") category: String,
        @Query("language") language: String = "en"
    ): TopHeadlinesResponseDto
}

