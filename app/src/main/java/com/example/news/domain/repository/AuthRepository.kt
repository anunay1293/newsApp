package com.example.news.domain.repository

import com.example.news.domain.model.AuthResult

/**
 * Repository contract for authentication operations.
 *
 * Abstracts the underlying auth provider (AWS Amplify / Cognito) so that the domain
 * and presentation layers are decoupled from any specific SDK. Concrete implementations
 * live in the data layer.
 */
interface AuthRepository {

    /**
     * Checks whether the user has an active, valid session.
     *
     * @return `true` if the user is signed in with a valid session, `false` otherwise.
     */
    suspend fun checkSession(): Boolean

    /**
     * Registers a new user account with the given credentials.
     *
     * @param email    The user's email address (also used as the username).
     * @param password The desired password; must meet the provider's policy.
     * @return [AuthResult.NeedsConfirmation] when email verification is required,
     *         [AuthResult.Success] if sign-up is immediately complete, or
     *         [AuthResult.Error] on failure.
     */
    suspend fun signUp(email: String, password: String): AuthResult

    /**
     * Confirms a newly registered account by submitting the verification code.
     *
     * @param email The email address associated with the account.
     * @param code  The verification code from the confirmation email.
     * @return [AuthResult.Success] on successful confirmation, or [AuthResult.Error] on failure.
     */
    suspend fun confirmSignUp(email: String, code: String): AuthResult

    /**
     * Requests the auth provider to resend the sign-up confirmation code.
     *
     * @param email The email address to which the code should be sent.
     * @return [AuthResult.Success] if the code was resent, or [AuthResult.Error] on failure.
     */
    suspend fun resendCode(email: String): AuthResult

    /**
     * Authenticates an existing user with the given credentials.
     *
     * @param email    The user's email / username.
     * @param password The user's password.
     * @return [AuthResult.Success] on successful sign-in, or [AuthResult.Error] on failure.
     */
    suspend fun signIn(email: String, password: String): AuthResult

    /**
     * Signs the current user out of their session.
     *
     * @return [AuthResult.Success] on successful sign-out, or [AuthResult.Error] on failure.
     */
    suspend fun signOut(): AuthResult
}
