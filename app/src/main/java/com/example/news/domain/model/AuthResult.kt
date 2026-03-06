package com.example.news.domain.model

/**
 * Sealed class representing the outcome of an authentication operation.
 *
 * Abstracts away the underlying auth provider (AWS Amplify / Cognito) so that the
 * domain and presentation layers never depend on provider-specific types.
 */
sealed class AuthResult {

    /** The authentication operation completed successfully. */
    object Success : AuthResult()

    /**
     * The user's account requires email confirmation before sign-in is allowed.
     *
     * @property email The email address that needs verification.
     */
    data class NeedsConfirmation(val email: String) : AuthResult()

    /**
     * The authentication operation failed.
     *
     * @property message A user-facing description of what went wrong.
     */
    data class Error(val message: String) : AuthResult()
}
