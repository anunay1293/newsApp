package com.example.news.data.repository

import android.util.Log
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import com.example.news.domain.model.AuthResult
import com.example.news.domain.repository.AuthRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AuthRepositoryImpl"

/**
 * Concrete implementation of [AuthRepository] backed by AWS Amplify / Cognito.
 *
 * Each method wraps the callback-based Amplify SDK in [suspendCancellableCoroutine] and
 * returns a domain-layer [AuthResult]. Amplify-specific exceptions are caught here and
 * translated into user-facing error messages within the [AuthResult.Error] type.
 *
 * Provided to the dependency graph by Hilt via [RepositoryModule].
 */
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    override suspend fun checkSession(): Boolean {
        return try {
            suspendCancellableCoroutine { continuation ->
                Amplify.Auth.fetchAuthSession(
                    { result -> continuation.resume(result.isSignedIn) },
                    { error -> continuation.resumeWithException(error) }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session check failed", e)
            false
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            val options = AuthSignUpOptions.builder()
                .userAttribute(AuthUserAttributeKey.email(), email)
                .build()

            suspendCancellableCoroutine<Boolean> { continuation ->
                Amplify.Auth.signUp(
                    email,
                    password,
                    options,
                    { result -> continuation.resume(result.isSignUpComplete) },
                    { error -> continuation.resumeWithException(error) }
                )
            }

            Log.d(TAG, "Sign up successful for: $email")
            AuthResult.NeedsConfirmation(email)
        } catch (e: AuthException) {
            Log.e(TAG, "Sign up failed", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error(
                when {
                    errorMsg.contains("already exists", ignoreCase = true) ||
                    errorMsg.contains("UsernameExistsException", ignoreCase = true) ->
                        "An account with this email already exists"
                    errorMsg.contains("invalid", ignoreCase = true) ->
                        "Invalid email or password format"
                    errorMsg.contains("password", ignoreCase = true) ||
                    errorMsg.contains("Password", ignoreCase = true) ->
                        "Password does not meet requirements (must be at least 8 characters)"
                    errorMsg.contains("InvalidPasswordException", ignoreCase = true) ->
                        "Password does not meet requirements (must be at least 8 characters)"
                    errorMsg.contains("InvalidParameterException", ignoreCase = true) ->
                        "Invalid email or password format"
                    else -> "Sign up failed: $errorMsg"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed with exception", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error("Sign up failed: $errorMsg")
        }
    }

    override suspend fun confirmSignUp(email: String, code: String): AuthResult {
        return try {
            val isComplete = suspendCancellableCoroutine<Boolean> { continuation ->
                Amplify.Auth.confirmSignUp(
                    email,
                    code,
                    { result -> continuation.resume(result.isSignUpComplete) },
                    { error -> continuation.resumeWithException(error) }
                )
            }

            if (isComplete) {
                Log.d(TAG, "Email confirmation successful for: $email")
                AuthResult.Success
            } else {
                Log.w(TAG, "Confirmation incomplete for: $email")
                AuthResult.Error("Confirmation incomplete. Please try again.")
            }
        } catch (e: AuthException) {
            Log.e(TAG, "Confirm sign up failed", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error(
                when {
                    errorMsg.contains("invalid", ignoreCase = true) ||
                    errorMsg.contains("InvalidCodeException", ignoreCase = true) ||
                    errorMsg.contains("CodeMismatchException", ignoreCase = true) ->
                        "Invalid confirmation code. Please check your email and try again."
                    errorMsg.contains("ExpiredCodeException", ignoreCase = true) ->
                        "Confirmation code has expired. Please request a new code."
                    errorMsg.contains("NotAuthorizedException", ignoreCase = true) ->
                        "Confirmation failed. The code may be invalid or expired."
                    else -> "Confirmation failed: $errorMsg"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Confirm sign up failed with exception", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error("Confirmation failed: $errorMsg")
        }
    }

    override suspend fun resendCode(email: String): AuthResult {
        return try {
            suspendCancellableCoroutine<Unit> { continuation ->
                Amplify.Auth.resendSignUpCode(
                    email,
                    { _ -> continuation.resume(Unit) },
                    { error -> continuation.resumeWithException(error) }
                )
            }

            Log.d(TAG, "Confirmation code resent to: $email")
            AuthResult.Success
        } catch (e: AuthException) {
            Log.e(TAG, "Resend code failed", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error(
                when {
                    errorMsg.contains("invalid", ignoreCase = true) ->
                        "Invalid email address"
                    errorMsg.contains("NotFound", ignoreCase = true) ->
                        "No account found with this email"
                    else -> "Failed to resend code: $errorMsg"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Resend code failed with exception", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error("Failed to resend code: $errorMsg")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            suspendCancellableCoroutine<Unit> { continuation ->
                Amplify.Auth.signOut { continuation.resume(Unit) }
            }

            suspendCancellableCoroutine<Unit> { continuation ->
                Amplify.Auth.signIn(
                    email,
                    password,
                    { result ->
                        if (result.isSignedIn) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(Exception("Sign in incomplete"))
                        }
                    },
                    { error -> continuation.resumeWithException(error) }
                )
            }

            Log.d(TAG, "Sign in successful for: $email")
            AuthResult.Success
        } catch (e: AuthException) {
            Log.e(TAG, "Sign in failed", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error(
                when {
                    errorMsg.contains("not confirmed", ignoreCase = true) ||
                    errorMsg.contains("UserNotConfirmedException", ignoreCase = true) ||
                    errorMsg.contains("UserNotFoundException", ignoreCase = true) &&
                    errorMsg.contains("not confirmed", ignoreCase = true) ->
                        "Please confirm your email first. Check your inbox for the confirmation code."
                    errorMsg.contains("incorrect", ignoreCase = true) ||
                    errorMsg.contains("NotAuthorizedException", ignoreCase = true) ->
                        "Incorrect email or password"
                    errorMsg.contains("UserNotFoundException", ignoreCase = true) ->
                        "No account found with this email. Please sign up first."
                    else -> "Sign in failed: $errorMsg"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed with exception", e)
            val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
            AuthResult.Error("Sign in failed: $errorMsg")
        }
    }

    override suspend fun signOut(): AuthResult {
        return try {
            suspendCancellableCoroutine<Unit> { continuation ->
                Amplify.Auth.signOut { _ -> continuation.resume(Unit) }
            }
            AuthResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            AuthResult.Error("Sign out failed: ${e.message}")
        }
    }

    override suspend fun getIdToken(): String? {
        return try {
            suspendCancellableCoroutine { cont ->
                Amplify.Auth.fetchAuthSession(
                    { session ->
                        val cognitoSession = session as AWSCognitoAuthSession
                        cont.resume(cognitoSession.userPoolTokensResult.value?.idToken)
                    },
                    { cont.resume(null) }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ID token", e)
            null
        }
    }

    override suspend fun getUserId(): String? {
        return try {
            suspendCancellableCoroutine { cont ->
                Amplify.Auth.getCurrentUser(
                    { user -> cont.resume(user.userId) },
                    { cont.resume(null) }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user ID", e)
            null
        }
    }

    override suspend fun getCurrentUserEmail(): String? {
        return try {
            val attributes = suspendCancellableCoroutine { cont ->
                Amplify.Auth.fetchUserAttributes(
                    { attrs -> cont.resume(attrs) },
                    { cont.resume(emptyList()) }
                )
            }
            attributes
                .firstOrNull { it.key == AuthUserAttributeKey.email() }
                ?.value
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user email", e)
            null
        }
    }
}
