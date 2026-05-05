package com.example.news.domain.repository

interface ArticleSummaryRepository {
    suspend fun getArticleSummary(articleId: String, articleUrl: String, title: String): List<String>
}
