package com.example.news.data.api

import com.example.news.data.dto.FeedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface defining the contract for the remote news API
 * hosted on AWS API Gateway.
 *
 * The API is public and does not require authentication headers or API keys.
 * Base URL configuration is handled by the Hilt [NetworkModule].
 */
interface NewsApiService {

    /**
     * Fetches a list of articles for the given news [category].
     *
     * Makes a GET request to the `/feed` endpoint with a `category` query parameter.
     *
     * @param category The news category to retrieve (e.g., "general", "technology", "sports").
     * @return A [FeedResponseDto] containing the echoed category and a list of [ArticleDto]
     *         objects. The list may be empty if no articles are available for the category.
     * @throws retrofit2.HttpException on non-2xx HTTP responses.
     * @throws java.io.IOException on network connectivity failures.
     */
    @GET("feed")
    suspend fun getFeed(
        @Query("category") category: String
    ): FeedResponseDto
}

