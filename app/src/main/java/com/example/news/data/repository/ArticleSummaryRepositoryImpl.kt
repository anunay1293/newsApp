package com.example.news.data.repository

import com.example.news.data.api.ArticleSummaryApiService
import com.example.news.domain.repository.ArticleSummaryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleSummaryRepositoryImpl @Inject constructor(
    private val apiService: ArticleSummaryApiService
) : ArticleSummaryRepository {

    override suspend fun getArticleSummary(articleId: String, articleUrl: String, title: String): List<String> {
        return apiService.getArticleSummary(articleId, articleUrl, title).summaryPoints
    }
}
