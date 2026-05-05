package com.example.news.presentation.articledetail

import com.example.news.domain.model.Article

data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isCategoryFollowed: Boolean = false,
    val summaryState: ArticleSummaryState = ArticleSummaryState.Idle
)
