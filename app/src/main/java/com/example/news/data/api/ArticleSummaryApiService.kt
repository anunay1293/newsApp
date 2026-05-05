package com.example.news.data.api

import com.example.news.data.dto.ArticleSummaryDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ArticleSummaryApiService {
    @GET("article-summary")
    suspend fun getArticleSummary(
        @Query("articleId") articleId: String,
        @Query("articleUrl") articleUrl: String,
        @Query("title") title: String
    ): ArticleSummaryDto
}
