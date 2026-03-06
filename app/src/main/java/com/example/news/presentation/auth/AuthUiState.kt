package com.example.news.presentation.auth

/**
 * Sealed class representing the possible authentication states of the user.
 *
 * Used by [AuthViewModel] and observed by the UI (Settings screen, sign-in flow) to
 * decide which controls to display. The states form a simple finite-state machine:
 *
 * ```
 * CheckingSession ──► SignedIn
 *        │                ▲
 *        ▼                │  (auto sign-in after confirmation)
 *    SignedOut ──► NeedsConfirmation ──► SignedIn
 *                         │
 *                         ▼  (fallback if auto sign-in fails)
 *                      SignedOut
 * ```
 */
sealed class AuthUiState {

    /** Initial transient state while the app queries Cognito for an existing session. */
    object CheckingSession : AuthUiState()

    /** The user has no active session and should see the sign-in / sign-up screens. */
    object SignedOut : AuthUiState()

    /**
     * The user has registered but has not yet confirmed their email address.
     *
     * @property email The email that requires verification; passed to the confirmation screen
     *                 so the correct code can be validated and the resend-code API can be called.
     */
    data class NeedsConfirmation(val email: String) : AuthUiState()

    /** The user has a valid, active Cognito session and should see the main news content. */
    object SignedIn : AuthUiState()
}

