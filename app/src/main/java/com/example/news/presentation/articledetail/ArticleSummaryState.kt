package com.example.news.presentation.articledetail

sealed class ArticleSummaryState {
    object Idle : ArticleSummaryState()
    object Loading : ArticleSummaryState()
    data class Success(val points: List<String>) : ArticleSummaryState()
    object Error : ArticleSummaryState()
}
