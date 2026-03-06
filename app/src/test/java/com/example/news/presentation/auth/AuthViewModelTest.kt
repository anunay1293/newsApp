package com.example.news.presentation.auth

import com.example.news.domain.model.AuthResult
import com.example.news.domain.usecase.CheckAuthSessionUseCase
import com.example.news.domain.usecase.ClearBookmarksOnSignOutUseCase
import com.example.news.domain.usecase.ConfirmSignUpUseCase
import com.example.news.domain.usecase.GetCurrentUserEmailUseCase
import com.example.news.domain.usecase.ResendCodeUseCase
import com.example.news.domain.usecase.SignInUseCase
import com.example.news.domain.usecase.SignOutUseCase
import com.example.news.domain.usecase.SignUpUseCase
import com.example.news.domain.usecase.SyncBookmarksOnSignInUseCase
import com.example.news.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val checkAuthSessionUseCase: CheckAuthSessionUseCase = mockk()
    private val signUpUseCase: SignUpUseCase = mockk()
    private val confirmSignUpUseCase: ConfirmSignUpUseCase = mockk()
    private val resendCodeUseCase: ResendCodeUseCase = mockk()
    private val signInUseCase: SignInUseCase = mockk()
    private val signOutUseCase: SignOutUseCase = mockk()
    private val syncBookmarksOnSignInUseCase: SyncBookmarksOnSignInUseCase = mockk(relaxUnitFun = true)
    private val clearBookmarksOnSignOutUseCase: ClearBookmarksOnSignOutUseCase = mockk(relaxUnitFun = true)
    private val getCurrentUserEmailUseCase: GetCurrentUserEmailUseCase = mockk()

    private fun createViewModel(): AuthViewModel {
        return AuthViewModel(
            checkAuthSessionUseCase,
            signUpUseCase,
            confirmSignUpUseCase,
            resendCodeUseCase,
            signInUseCase,
            signOutUseCase,
            syncBookmarksOnSignInUseCase,
            clearBookmarksOnSignOutUseCase,
            getCurrentUserEmailUseCase
        )
    }

    // ── Session Checking ────────────────────────────────────────────────

    @Test
    fun `given user has an active session, when cold start, then state is SignedIn and bookmarks synced`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns true
        coEvery { getCurrentUserEmailUseCase() } returns "user@example.com"

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedIn)
        assertEquals("user@example.com", vm.userEmail.value)
        coVerify { syncBookmarksOnSignInUseCase() }
    }

    @Test
    fun `given no active session, when cold start, then state is SignedOut`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedOut)
    }

    @Test
    fun `given session check throws, when cold start, then state is SignedOut with error`() = runTest {
        coEvery { checkAuthSessionUseCase() } throws RuntimeException("Network error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedOut)
        assertEquals("Failed to check session: Network error", vm.errorMessage.value)
    }

    @Test
    fun `given state is NeedsConfirmation, when checkAuthSession called, then state is not overwritten`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        val vm = createViewModel()
        advanceUntilIdle()

        coEvery { signUpUseCase("a@b.com", "pass") } returns AuthResult.NeedsConfirmation("a@b.com")
        vm.onSignUp("a@b.com", "pass")
        advanceUntilIdle()
        assertTrue(vm.authState.value is AuthUiState.NeedsConfirmation)

        coEvery { checkAuthSessionUseCase() } returns true
        coEvery { getCurrentUserEmailUseCase() } returns "a@b.com"
        vm.checkAuthSession()
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.NeedsConfirmation)
    }

    // ── Sign Up ─────────────────────────────────────────────────────────

    @Test
    fun `given backend requires confirmation, when signing up, then state is NeedsConfirmation`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signUpUseCase("a@b.com", "pass123") } returns AuthResult.NeedsConfirmation("a@b.com")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignUp("a@b.com", "pass123")
        advanceUntilIdle()

        val state = vm.authState.value
        assertTrue(state is AuthUiState.NeedsConfirmation)
        assertEquals("a@b.com", (state as AuthUiState.NeedsConfirmation).email)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `given backend returns error, when signing up, then error message is set`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signUpUseCase("a@b.com", "weak") } returns AuthResult.Error("Password too weak")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignUp("a@b.com", "weak")
        advanceUntilIdle()

        assertEquals("Password too weak", vm.errorMessage.value)
        assertFalse(vm.isLoading.value)
    }

    // ── Confirm Sign Up ─────────────────────────────────────────────────

    @Test
    fun `given confirmation succeeds and pending password exists, when confirming, then auto sign-in fires`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signUpUseCase("a@b.com", "pass123") } returns AuthResult.NeedsConfirmation("a@b.com")
        coEvery { confirmSignUpUseCase("a@b.com", "123456") } returns AuthResult.Success
        coEvery { signInUseCase("a@b.com", "pass123") } returns AuthResult.Success
        coEvery { getCurrentUserEmailUseCase() } returns "a@b.com"

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignUp("a@b.com", "pass123")
        advanceUntilIdle()

        vm.onConfirmSignUp("a@b.com", "123456")
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedIn)
        coVerify { syncBookmarksOnSignInUseCase() }
        assertEquals("a@b.com", vm.userEmail.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `given confirmation succeeds but auto sign-in fails, when confirming, then state falls back to SignedOut`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signUpUseCase("a@b.com", "pass123") } returns AuthResult.NeedsConfirmation("a@b.com")
        coEvery { confirmSignUpUseCase("a@b.com", "123456") } returns AuthResult.Success
        coEvery { signInUseCase("a@b.com", "pass123") } returns AuthResult.Error("Network error")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignUp("a@b.com", "pass123")
        advanceUntilIdle()

        vm.onConfirmSignUp("a@b.com", "123456")
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedOut)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `given wrong confirmation code, when confirming, then error message is set`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { confirmSignUpUseCase("a@b.com", "000000") } returns AuthResult.Error("Invalid code")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onConfirmSignUp("a@b.com", "000000")
        advanceUntilIdle()

        assertEquals("Invalid code", vm.errorMessage.value)
        assertFalse(vm.isLoading.value)
    }

    // ── Sign In ─────────────────────────────────────────────────────────

    @Test
    fun `given valid credentials, when signing in, then state is SignedIn and bookmarks synced`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signInUseCase("a@b.com", "pass123") } returns AuthResult.Success
        coEvery { getCurrentUserEmailUseCase() } returns "a@b.com"

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignIn("a@b.com", "pass123")
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedIn)
        coVerify { syncBookmarksOnSignInUseCase() }
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `given unconfirmed account, when signing in, then state is NeedsConfirmation`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signInUseCase("a@b.com", "pass123") } returns AuthResult.NeedsConfirmation("a@b.com")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignIn("a@b.com", "pass123")
        advanceUntilIdle()

        val state = vm.authState.value
        assertTrue(state is AuthUiState.NeedsConfirmation)
        assertEquals("a@b.com", (state as AuthUiState.NeedsConfirmation).email)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `given wrong password, when signing in, then error message is set`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signInUseCase("a@b.com", "wrong") } returns AuthResult.Error("Incorrect password")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignIn("a@b.com", "wrong")
        advanceUntilIdle()

        assertEquals("Incorrect password", vm.errorMessage.value)
        assertFalse(vm.isLoading.value)
    }

    // ── Sign Out ────────────────────────────────────────────────────────

    @Test
    fun `given sign-out succeeds, when signing out, then state is SignedOut and bookmarks cleared`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns true
        coEvery { getCurrentUserEmailUseCase() } returns "a@b.com"
        coEvery { signOutUseCase() } returns AuthResult.Success

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignOut()
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedOut)
        assertNull(vm.userEmail.value)
        coVerify { clearBookmarksOnSignOutUseCase() }
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `given sign-out fails, when signing out, then state is still SignedOut and bookmarks cleared`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns true
        coEvery { getCurrentUserEmailUseCase() } returns "a@b.com"
        coEvery { signOutUseCase() } returns AuthResult.Error("Token revocation failed")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignOut()
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthUiState.SignedOut)
        assertEquals("Token revocation failed", vm.errorMessage.value)
        coVerify { clearBookmarksOnSignOutUseCase() }
        assertFalse(vm.isLoading.value)
    }

    // ── Resend Code ─────────────────────────────────────────────────────

    @Test
    fun `given resend succeeds, when requesting new code, then success message shown`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { resendCodeUseCase("a@b.com") } returns AuthResult.Success

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onResendCode("a@b.com")
        advanceUntilIdle()

        assertEquals("Confirmation code sent to your email", vm.errorMessage.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `given resend fails, when requesting new code, then error message shown`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { resendCodeUseCase("a@b.com") } returns AuthResult.Error("Rate limited")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onResendCode("a@b.com")
        advanceUntilIdle()

        assertEquals("Rate limited", vm.errorMessage.value)
        assertFalse(vm.isLoading.value)
    }

    // ── Misc ────────────────────────────────────────────────────────────

    @Test
    fun `given an error message exists, when clearing error, then error message is null`() = runTest {
        coEvery { checkAuthSessionUseCase() } returns false
        coEvery { signInUseCase("a@b.com", "wrong") } returns AuthResult.Error("Bad password")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSignIn("a@b.com", "wrong")
        advanceUntilIdle()
        assertEquals("Bad password", vm.errorMessage.value)

        vm.clearError()

        assertNull(vm.errorMessage.value)
    }
}
