package com.example.news.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ViewModel responsible for managing all authentication operations via AWS Amplify / Cognito.
 *
 * Exposes three reactive state flows consumed by the UI:
 * - [authState] – the current authentication lifecycle state (checking, signed-out, needs-confirmation, signed-in)
 * - [isLoading] – whether an auth operation is currently in progress
 * - [errorMessage] – a user-facing error string, or `null` when there is no error
 *
 * This ViewModel is scoped to the Activity so a single instance is shared across all
 * auth-related screens (SignIn, SignUp, Confirm) and the settings screen for sign-out.
 *
 * @param application The [Application] context required by [AndroidViewModel] and used
 *                    internally by Amplify for configuration lookups.
 */
class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    /** Mutable backing field for the current authentication state. */
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)

    /** Observable authentication state consumed by the UI layer. */
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()
    
    /** Mutable backing field for the loading indicator. */
    private val _isLoading = MutableStateFlow(false)

    /** Whether an authentication operation (sign-in, sign-up, confirm, sign-out) is in flight. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /** Mutable backing field for the error message. */
    private val _errorMessage = MutableStateFlow<String?>(null)

    /** User-facing error message from the most recent failed operation, or `null` if none. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        checkAuthSession()
    }
    
    /**
     * Checks whether the user has an active Cognito session.
     *
     * Fetches the current auth session from Amplify. If the session is valid the state
     * transitions to [AuthUiState.SignedIn]; otherwise it falls back to [AuthUiState.SignedOut].
     * The check is skipped if the current state is [AuthUiState.NeedsConfirmation] to avoid
     * overriding a pending email-confirmation flow.
     */
    fun checkAuthSession() {
        viewModelScope.launch {
            if (_authState.value !is AuthUiState.NeedsConfirmation) {
                _authState.value = AuthUiState.CheckingSession
            }
            _errorMessage.value = null
            
            try {
                val session = suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.fetchAuthSession(
                        { result ->
                            if (result.isSignedIn) {
                                continuation.resume(true)
                            } else {
                                continuation.resume(false)
                            }
                        },
                        { error ->
                            continuation.resumeWithException(error)
                        }
                    )
                }
                
                // Only update state if not already NeedsConfirmation
                if (_authState.value !is AuthUiState.NeedsConfirmation) {
                    _authState.value = if (session) {
                        AuthUiState.SignedIn
                    } else {
                        AuthUiState.SignedOut
                    }
                }
            } catch (e: Exception) {
                // Only update state if not already NeedsConfirmation
                if (_authState.value !is AuthUiState.NeedsConfirmation) {
                    _authState.value = AuthUiState.SignedOut
                    _errorMessage.value = "Failed to check session: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Registers a new user account with the given email and password via AWS Cognito.
     *
     * On success the state transitions to [AuthUiState.NeedsConfirmation] so the UI can
     * navigate to the email-confirmation screen. Common Cognito errors (duplicate account,
     * weak password, invalid parameters) are mapped to user-friendly messages exposed
     * through [errorMessage].
     *
     * @param email    The user's email address (also used as the Cognito username).
     * @param password The desired password; must meet the Cognito user-pool policy.
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val options = AuthSignUpOptions.builder()
                    .userAttribute(AuthUserAttributeKey.email(), email)
                    .build()
                
                val result = suspendCancellableCoroutine<Boolean> { continuation ->
                    Amplify.Auth.signUp(
                        email,
                        password,
                        options,
                        { result ->
                            continuation.resume(result.isSignUpComplete)
                        },
                        { error ->
                            continuation.resumeWithException(error)
                        }
                    )
                }
                
                // For Cognito, sign up almost always requires confirmation
                // Navigate to confirmation screen regardless of isSignUpComplete
                Log.d("AuthViewModel", "Sign up successful, setting state to NeedsConfirmation for: $email")
                _authState.value = AuthUiState.NeedsConfirmation(email)
                Log.d("AuthViewModel", "State updated to NeedsConfirmation")
            } catch (e: AuthException) {
                // Log the full error for debugging
                Log.e("AuthViewModel", "Sign up failed", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                Log.e("AuthViewModel", "Error message: $errorMsg")
                Log.e("AuthViewModel", "Error cause: ${e.cause}")
                _errorMessage.value = when {
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
            } catch (e: Exception) {
                // Log the full error for debugging
                Log.e("AuthViewModel", "Sign up failed with exception", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                _errorMessage.value = "Sign up failed: $errorMsg"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Confirms a newly registered account by submitting the verification code sent to the
     * user's email.
     *
     * On successful confirmation the state transitions to [AuthUiState.SignedOut] so the
     * user can sign in with their new credentials. Invalid or expired codes produce a
     * descriptive error in [errorMessage].
     *
     * @param email The email address associated with the account being confirmed.
     * @param code  The 6-digit verification code from the confirmation email.
     */
    fun confirmSignUp(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = suspendCancellableCoroutine<Boolean> { continuation ->
                    Amplify.Auth.confirmSignUp(
                        email,
                        code,
                        { result ->
                            continuation.resume(result.isSignUpComplete)
                        },
                        { error ->
                            continuation.resumeWithException(error)
                        }
                    )
                }
                
                if (result) {
                    // Confirmation successful, user can now sign in
                    Log.d("AuthViewModel", "Email confirmation successful for: $email")
                    _authState.value = AuthUiState.SignedOut
                    _errorMessage.value = null
                } else {
                    Log.w("AuthViewModel", "Confirmation incomplete for: $email")
                    _errorMessage.value = "Confirmation incomplete. Please try again."
                }
            } catch (e: AuthException) {
                // Log the full error for debugging
                Log.e("AuthViewModel", "Confirm sign up failed", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                Log.e("AuthViewModel", "Error message: $errorMsg")
                _errorMessage.value = when {
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
            } catch (e: Exception) {
                // Log the full error for debugging
                Log.e("AuthViewModel", "Confirm sign up failed with exception", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                _errorMessage.value = "Confirmation failed: $errorMsg"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Requests Cognito to resend the sign-up confirmation code to the specified email.
     *
     * On success, a status message is placed in [errorMessage] to inform the user that
     * the code was sent. On failure, a descriptive error message is provided instead.
     *
     * @param email The email address to which the new confirmation code should be sent.
     */
    fun resendCode(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                suspendCancellableCoroutine<Unit> { continuation ->
                    Amplify.Auth.resendSignUpCode(
                        email,
                        { result ->
                            continuation.resume(Unit)
                        },
                        { error ->
                            continuation.resumeWithException(error)
                        }
                    )
                }
                
                Log.d("AuthViewModel", "Confirmation code resent to: $email")
                _errorMessage.value = "Confirmation code sent to your email"
            } catch (e: AuthException) {
                // Log the full error for debugging
                Log.e("AuthViewModel", "Resend code failed", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                _errorMessage.value = when {
                    errorMsg.contains("invalid", ignoreCase = true) ->
                        "Invalid email address"
                    errorMsg.contains("NotFound", ignoreCase = true) ->
                        "No account found with this email"
                    else -> "Failed to resend code: $errorMsg"
                }
            } catch (e: Exception) {
                // Log the full error for debugging
                Log.e("AuthViewModel", "Resend code failed with exception", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                _errorMessage.value = "Failed to resend code: $errorMsg"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Authenticates an existing user with the given email and password.
     *
     * Before attempting sign-in, any stale or partial Cognito session is explicitly
     * cleared to avoid "already signed in" conflicts. On success the state transitions
     * to [AuthUiState.SignedIn], causing [AuthGate] to render the news feed. Cognito-specific
     * errors (unconfirmed account, wrong password, unknown user) are mapped to user-friendly
     * messages.
     *
     * @param email    The user's email / Cognito username.
     * @param password The user's password.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
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
                        { error ->
                            continuation.resumeWithException(error)
                        }
                    )
                }
                
                Log.d("AuthViewModel", "Sign in successful for: $email")
                _authState.value = AuthUiState.SignedIn
                _errorMessage.value = null
            } catch (e: AuthException) {
                Log.e("AuthViewModel", "Sign in failed", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                Log.e("AuthViewModel", "Error message: $errorMsg")
                _errorMessage.value = when {
                    errorMsg.contains("not confirmed", ignoreCase = true) ||
                    errorMsg.contains("UserNotConfirmedException", ignoreCase = true) ||
                    errorMsg.contains("UserNotFoundException", ignoreCase = true) && errorMsg.contains("not confirmed", ignoreCase = true) -> 
                        "Please confirm your email first. Check your inbox for the confirmation code."
                    errorMsg.contains("incorrect", ignoreCase = true) ||
                    errorMsg.contains("NotAuthorizedException", ignoreCase = true) -> 
                        "Incorrect email or password"
                    errorMsg.contains("UserNotFoundException", ignoreCase = true) ->
                        "No account found with this email. Please sign up first."
                    else -> "Sign in failed: $errorMsg"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in failed with exception", e)
                val errorMsg = e.message ?: e.cause?.message ?: "Unknown error"
                _errorMessage.value = "Sign in failed: $errorMsg"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Signs the current user out of their Cognito session.
     *
     * Regardless of whether the Amplify sign-out call succeeds or fails, the local state
     * is always set to [AuthUiState.SignedOut] so the user is returned to the sign-in screen.
     * This prevents the user from getting stuck in a signed-in state with an invalid token.
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                suspendCancellableCoroutine<Unit> { continuation ->
                    Amplify.Auth.signOut(
                        { result ->
                            continuation.resume(Unit)
                        }
                    )
                }
                
                _authState.value = AuthUiState.SignedOut
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Sign out failed: ${e.message}"
                _authState.value = AuthUiState.SignedOut // Still sign out on error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clears the current error message, typically called when the user dismisses an error
     * banner or navigates away from a screen that displayed the error.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

