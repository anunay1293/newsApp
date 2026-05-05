package com.example.news.presentation.bookmarks

import androidx.paging.PagingData
import com.example.news.domain.repository.AuthRepository
import com.example.news.domain.usecase.GetPagedBookmarkedArticlesUseCase
import com.example.news.domain.usecase.ToggleBookmarkUseCase
import com.example.news.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPagedBookmarkedArticlesUseCase: GetPagedBookmarkedArticlesUseCase = mockk()
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk(relaxUnitFun = true)
    private val authRepository: AuthRepository = mockk()

    private fun createViewModel(): BookmarksViewModel {
        every { getPagedBookmarkedArticlesUseCase() } returns flowOf(PagingData.empty())
        return BookmarksViewModel(
            getPagedBookmarkedArticlesUseCase,
            toggleBookmarkUseCase,
            authRepository
        )
    }

    @Test
    fun `given user is signed in, when VM initializes, then isSignedIn is true`() = runTest {
        coEvery { authRepository.checkSession() } returns true

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.isSignedIn.value)
    }

    @Test
    fun `given user is signed out, when VM initializes, then isSignedIn is false`() = runTest {
        coEvery { authRepository.checkSession() } returns false

        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.isSignedIn.value)
    }

    @Test
    fun `given user just signed in, when rechecking auth, then isSignedIn flips to true`() = runTest {
        coEvery { authRepository.checkSession() } returns false
        val vm = createViewModel()
        advanceUntilIdle()
        assertFalse(vm.isSignedIn.value)

        coEvery { authRepository.checkSession() } returns true
        vm.onRecheckAuth()
        advanceUntilIdle()

        assertTrue(vm.isSignedIn.value)
    }

    @Test
    fun `given user taps remove on a bookmark, when toggling, then use case is invoked`() = runTest {
        coEvery { authRepository.checkSession() } returns true
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onBookmarkToggle("article-1")
        advanceUntilIdle()

        coVerify { toggleBookmarkUseCase("article-1") }
    }
}
