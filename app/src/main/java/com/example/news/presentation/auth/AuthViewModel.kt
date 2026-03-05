package com.example.news.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.news.domain.model.AuthResult
import com.example.news.domain.usecase.CheckAuthSessionUseCase
import com.example.news.domain.usecase.ConfirmSignUpUseCase
import com.example.news.domain.usecase.ResendCodeUseCase
import com.example.news.domain.usecase.SignInUseCase
import com.example.news.domain.usecase.SignOutUseCase
import com.example.news.domain.usecase.SignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing authentication UI state.
 *
 * Delegates all authentication operations to domain-layer use cases and maps the
 * resulting [AuthResult] values into observable [AuthUiState], [isLoading], and
 * [errorMessage] flows consumed by the UI.
 *
 * This ViewModel is scoped to the Activity so a single instance is shared across all
 * auth-related screens (SignIn, SignUp, Confirm) and the settings screen for sign-out.
 *
 * @param checkAuthSessionUseCase Use case for checking the current session validity.
 * @param signUpUseCase           Use case for registering a new account.
 * @param confirmSignUpUseCase    Use case for confirming an account with a verification code.
 * @param resendCodeUseCase       Use case for resending the confirmation code.
 * @param signInUseCase           Use case for authenticating an existing user.
 * @param signOutUseCase          Use case for signing the user out.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val checkAuthSessionUseCase: CheckAuthSessionUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val confirmSignUpUseCase: ConfirmSignUpUseCase,
    private val resendCodeUseCase: ResendCodeUseCase,
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)

    /** Observable authentication state consumed by the UI layer. */
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /** Whether an authentication operation is in flight. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    /** User-facing error message from the most recent failed operation, or `null` if none. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkAuthSession()
    }

    /**
     * Checks whether the user has an active session.
     *
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
                val isSignedIn = checkAuthSessionUseCase()

                if (_authState.value !is AuthUiState.NeedsConfirmation) {
                    _authState.value = if (isSignedIn) {
                        AuthUiState.SignedIn
                    } else {
                        AuthUiState.SignedOut
                    }
                }
            } catch (e: Exception) {
                if (_authState.value !is AuthUiState.NeedsConfirmation) {
                    _authState.value = AuthUiState.SignedOut
                    _errorMessage.value = "Failed to check session: ${e.message}"
                }
            }
        }
    }

    /**
     * Registers a new user account with the given email and password.
     *
     * On success the state transitions to [AuthUiState.NeedsConfirmation] so the UI can
     * navigate to the email-confirmation screen.
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = signUpUseCase(email, password)
            when (result) {
                is AuthResult.NeedsConfirmation -> {
                    _authState.value = AuthUiState.NeedsConfirmation(result.email)
                }
                is AuthResult.Success -> {
                    _authState.value = AuthUiState.NeedsConfirmation(email)
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Confirms a newly registered account by submitting the verification code.
     *
     * On successful confirmation the state transitions to [AuthUiState.SignedOut] so the
     * user can sign in with their new credentials.
     */
    fun confirmSignUp(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = confirmSignUpUseCase(email, code)
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = AuthUiState.SignedOut
                    _errorMessage.value = null
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
                is AuthResult.NeedsConfirmation -> {
                    _authState.value = AuthUiState.NeedsConfirmation(result.email)
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Requests the auth provider to resend the sign-up confirmation code.
     *
     * On success a status message is placed in [errorMessage] to inform the user.
     */
    fun resendCode(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = resendCodeUseCase(email)
            when (result) {
                is AuthResult.Success -> {
                    _errorMessage.value = "Confirmation code sent to your email"
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
                is AuthResult.NeedsConfirmation -> { /* not expected for resend */ }
            }

            _isLoading.value = false
        }
    }

    /**
     * Authenticates an existing user with the given email and password.
     *
     * On success the state transitions to [AuthUiState.SignedIn].
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = signInUseCase(email, password)
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = AuthUiState.SignedIn
                    _errorMessage.value = null
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
                is AuthResult.NeedsConfirmation -> {
                    _authState.value = AuthUiState.NeedsConfirmation(result.email)
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Signs the current user out.
     *
     * The local state is always set to [AuthUiState.SignedOut] regardless of whether
     * the operation succeeds, preventing the user from getting stuck in a signed-in
     * state with an invalid token.
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = signOutUseCase()
            when (result) {
                is AuthResult.Success -> {
                    _authState.value = AuthUiState.SignedOut
                    _errorMessage.value = null
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                    _authState.value = AuthUiState.SignedOut
                }
                is AuthResult.NeedsConfirmation -> { /* not expected for sign-out */ }
            }

            _isLoading.value = false
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
