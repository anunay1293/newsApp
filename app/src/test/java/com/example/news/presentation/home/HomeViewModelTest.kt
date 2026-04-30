package com.example.news.presentation.home

import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.news.domain.model.BookmarkToggleResult
import com.example.news.domain.usecase.AuthAwareToggleBookmarkUseCase
import com.example.news.domain.usecase.AuthAwareToggleFollowCategoryUseCase
import com.example.news.domain.usecase.CheckAuthSessionUseCase
import com.example.news.domain.usecase.GetFollowedCategoriesUseCase
import com.example.news.domain.usecase.GetPagedArticlesUseCase
import com.example.news.domain.usecase.GetPagedFollowedArticlesUseCase
import com.example.news.domain.usecase.RefreshArticlesUseCase
import com.example.news.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPagedArticlesUseCase: GetPagedArticlesUseCase = mockk()
    private val refreshArticlesUseCase: RefreshArticlesUseCase = mockk(relaxUnitFun = true)
    private val authAwareToggleBookmarkUseCase: AuthAwareToggleBookmarkUseCase = mockk()
    private val authAwareToggleFollowCategoryUseCase: AuthAwareToggleFollowCategoryUseCase = mockk()
    private val getPagedFollowedArticlesUseCase: GetPagedFollowedArticlesUseCase = mockk()
    private val getFollowedCategoriesUseCase: GetFollowedCategoriesUseCase = mockk()
    private val checkAuthSessionUseCase: CheckAuthSessionUseCase = mockk()

    private fun createViewModel(): HomeViewModel {
        every { getPagedArticlesUseCase(any(), any()) } returns flowOf(PagingData.empty())
        every { getPagedFollowedArticlesUseCase(any()) } returns flowOf(PagingData.empty())
        every { getFollowedCategoriesUseCase() } returns flowOf(emptySet())
        return HomeViewModel(
            getPagedArticlesUseCase,
            refreshArticlesUseCase,
            authAwareToggleBookmarkUseCase,
            authAwareToggleFollowCategoryUseCase,
            getPagedFollowedArticlesUseCase,
            getFollowedCategoriesUseCase,
            checkAuthSessionUseCase
        )
    }

    @Test
    fun `given fresh launch, when VM initializes, then default category is general and no error`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("general", state.selectedCategory)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
    }

    @Test
    fun `given user is on general feed, when switching to technology, then category updates and refresh fires`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onCategorySelected("technology")
        advanceUntilIdle()

        assertEquals("technology", vm.uiState.value.selectedCategory)
        coVerify { refreshArticlesUseCase("technology") }
    }

    @Test
    fun `given user is on home screen, when typing a search query, then search query updates`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSearchQueryChanged("kotlin")

        assertEquals("kotlin", vm.uiState.value.searchQuery)
    }

    @Test
    fun `given network is down, when category refresh fails, then error message is set`() = runTest {
        coEvery { refreshArticlesUseCase(any()) } throws RuntimeException("No internet")
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("No internet", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `given an error occurred, when user taps retry, then refresh fires again`() = runTest {
        var callCount = 0
        coEvery { refreshArticlesUseCase(any()) } coAnswers {
            callCount++
            if (callCount == 1) throw RuntimeException("No internet")
        }
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage != null)

        vm.onRetryClicked()
        advanceUntilIdle()

        coVerify(atLeast = 2) { refreshArticlesUseCase("general") }
    }

    @Test
    fun `given user swipes down, when pull to refresh succeeds, then isPullRefreshing resets`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onPullToRefresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPullRefreshing)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `given network is down, when pull to refresh fails, then error message is set`() = runTest {
        var callCount = 0
        coEvery { refreshArticlesUseCase(any()) } coAnswers {
            callCount++
            if (callCount > 1) throw RuntimeException("Network error")
        }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onPullToRefresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPullRefreshing)
        assertEquals("Network error", vm.uiState.value.errorMessage)
    }

    @Test
    fun `given user is signed in, when toggling bookmark, then use case is called and no auth event emitted`() = runTest {
        coEvery { authAwareToggleBookmarkUseCase(any(), any()) } returns BookmarkToggleResult.Success
        val vm = createViewModel()
        advanceUntilIdle()

        vm.authRequiredForBookmark.test {
            vm.onBookmarkToggle("article-1", false)
            advanceUntilIdle()

            coVerify { authAwareToggleBookmarkUseCase("article-1", false) }
            expectNoEvents()
        }
    }

    @Test
    fun `given user is signed out, when toggling bookmark, then auth required event is emitted`() = runTest {
        coEvery { authAwareToggleBookmarkUseCase(any(), any()) } returns BookmarkToggleResult.AuthRequired("article-1")
        val vm = createViewModel()
        advanceUntilIdle()

        vm.authRequiredForBookmark.test {
            vm.onBookmarkToggle("article-1", false)
            advanceUntilIdle()

            assertEquals("article-1", awaitItem())
        }
    }
}
