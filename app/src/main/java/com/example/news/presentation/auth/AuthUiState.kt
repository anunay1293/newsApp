package com.example.news.presentation.auth

/**
 * Represents the authentication state of the user.
 */
sealed class AuthUiState {
    object CheckingSession : AuthUiState()
    object SignedOut : AuthUiState()
    data class NeedsConfirmation(val email: String) : AuthUiState()
    object SignedIn : AuthUiState()
}

