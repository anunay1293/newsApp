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
import com.example.news.domain.usecase.ClearBookmarksOnSignOutUseCase
import com.example.news.domain.usecase.ClearFollowedCategoriesOnSignOutUseCase
import com.example.news.domain.usecase.GetCurrentUserEmailUseCase
import com.example.news.domain.usecase.SyncBookmarksOnSignInUseCase
import com.example.news.domain.usecase.SyncFollowedCategoriesOnSignInUseCase
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
 * auth-related screens (SignIn, SignUp, Confirm) and the Settings screen, which uses
 * the [authState] to decide whether to show a sign-in or sign-out button.
 *
 * @param checkAuthSessionUseCase Use case for checking the current session validity.
 * @param signUpUseCase           Use case for registering a new account.
 * @param confirmSignUpUseCase    Use case for confirming an account with a verification code.
 * @param resendCodeUseCase       Use case for resending the confirmation code.
 * @param signInUseCase           Use case for authenticating an existing user.
 * @param signOutUseCase                Use case for signing the user out.
 * @param syncBookmarksOnSignInUseCase  Use case for pulling remote bookmarks after sign-in.
 * @param clearBookmarksOnSignOutUseCase Use case for wiping local bookmarks on sign-out.
 * @param getCurrentUserEmailUseCase    Use case for retrieving the signed-in user's email.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val checkAuthSessionUseCase: CheckAuthSessionUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val confirmSignUpUseCase: ConfirmSignUpUseCase,
    private val resendCodeUseCase: ResendCodeUseCase,
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val syncBookmarksOnSignInUseCase: SyncBookmarksOnSignInUseCase,
    private val clearBookmarksOnSignOutUseCase: ClearBookmarksOnSignOutUseCase,
    private val syncFollowedCategoriesOnSignInUseCase: SyncFollowedCategoriesOnSignInUseCase,
    private val clearFollowedCategoriesOnSignOutUseCase: ClearFollowedCategoriesOnSignOutUseCase,
    private val getCurrentUserEmailUseCase: GetCurrentUserEmailUseCase
) : ViewModel(), SignInScreenEvents, SignUpScreenEvents, ConfirmScreenEvents, SettingsScreenEvents {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)

    /** Observable authentication state consumed by the UI layer. */
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    /**
     * Temporarily holds the user's password between sign-up (or sign-in that returns
     * [AuthResult.NeedsConfirmation]) and the end of the email-confirmation flow.
     *
     * Cleared immediately after the auto sign-in attempt in [confirmSignUp], on a
     * successful [signIn], and on [signOut] -- whichever comes first.
     */
    private var pendingPassword: String? = null

    private val _isLoading = MutableStateFlow(false)

    /** Whether an authentication operation is in flight. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    /** User-facing error message from the most recent failed operation, or `null` if none. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)

    /** Email address of the currently signed-in user, or `null` when signed out. */
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    init {
        checkAuthSession()
    }

    /**
     * Checks whether the user has an active session.
     *
     * The check is skipped if the current state is [AuthUiState.NeedsConfirmation] to avoid
     * overriding a pending email-confirmation flow.
     *
     * When an existing signed-in session is detected (e.g. on cold start), bookmarks are
     * synced from the remote source so that previously bookmarked articles reappear without
     * requiring a manual sign-out/sign-in cycle.
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
                        launch {
                            syncBookmarksOnSignInUseCase()
                            syncFollowedCategoriesOnSignInUseCase()
                        }
                        fetchUserEmail()
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
     * navigate to the email-confirmation screen. The password is retained in
     * [pendingPassword] so that [confirmSignUp] can auto sign-in after verification.
     */
    override fun onSignUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            pendingPassword = password

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
     * On successful confirmation the method automatically signs the user in using the
     * password stored in [pendingPassword] (set during [signUp] or [signIn]). This
     * avoids forcing the user back to the sign-in screen after registration.
     *
     * If the auto sign-in fails (e.g. network error) the state falls back to
     * [AuthUiState.SignedOut] so the user can sign in manually.
     * [pendingPassword] is always cleared after the attempt.
     */
    override fun onConfirmSignUp(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = confirmSignUpUseCase(email, code)
            when (result) {
                is AuthResult.Success -> {
                    val password = pendingPassword
                    pendingPassword = null

                    if (password != null) {
                        val signInResult = signInUseCase(email, password)
                        when (signInResult) {
                            is AuthResult.Success -> {
                                _authState.value = AuthUiState.SignedIn
                                _errorMessage.value = null
                                launch {
                            syncBookmarksOnSignInUseCase()
                            syncFollowedCategoriesOnSignInUseCase()
                        }
                                fetchUserEmail()
                            }
                            else -> {
                                _authState.value = AuthUiState.SignedOut
                                _errorMessage.value = null
                            }
                        }
                    } else {
                        _authState.value = AuthUiState.SignedOut
                        _errorMessage.value = null
                    }
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
    override fun onResendCode(email: String) {
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
     * On success the state transitions to [AuthUiState.SignedIn] and any stale
     * [pendingPassword] is cleared. If the account still requires email verification
     * the password is stored in [pendingPassword] so [confirmSignUp] can auto sign-in
     * after the user confirms.
     */
    override fun onSignIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = signInUseCase(email, password)
            when (result) {
                is AuthResult.Success -> {
                    pendingPassword = null
                    _authState.value = AuthUiState.SignedIn
                    _errorMessage.value = null
                    launch {
                            syncBookmarksOnSignInUseCase()
                            syncFollowedCategoriesOnSignInUseCase()
                        }
                    fetchUserEmail()
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
                is AuthResult.NeedsConfirmation -> {
                    pendingPassword = password
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
     * state with an invalid token. Any stale [pendingPassword] is also cleared.
     *
     * Local bookmarks are wiped unconditionally (both success and error paths) so the
     * feed does not display a previous user's bookmarks in the signed-out state.
     * Remote bookmarks in DynamoDB are unaffected and will be re-synced on the next
     * sign-in via [syncBookmarksOnSignInUseCase].
     */
    override fun onSignOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            pendingPassword = null
            _userEmail.value = null

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

            clearBookmarksOnSignOutUseCase()
            clearFollowedCategoriesOnSignOutUseCase()

            _isLoading.value = false
        }
    }

    /**
     * Fetches the signed-in user's email from the auth provider and updates [_userEmail].
     *
     * Launched as a fire-and-forget coroutine; failures are silently ignored since the
     * email label is non-critical UI.
     */
    private fun fetchUserEmail() {
        viewModelScope.launch {
            _userEmail.value = getCurrentUserEmailUseCase()
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
