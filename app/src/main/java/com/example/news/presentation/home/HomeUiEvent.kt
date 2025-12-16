package com.example.news.presentation.home

/**
 * UI events sent from HomeScreen to ViewModel.
 */
sealed class HomeUiEvent {
    data class OnCategorySelected(val category: String) : HomeUiEvent()
    object OnRetryClicked : HomeUiEvent()
}

