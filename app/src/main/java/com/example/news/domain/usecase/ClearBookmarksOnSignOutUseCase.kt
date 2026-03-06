package com.example.news.domain.usecase

import com.example.news.domain.repository.NewsRepository
import javax.inject.Inject

/**
 * Clears all local bookmark data on sign-out.
 *
 * Counterpart to [SyncBookmarksOnSignInUseCase]: while that use case populates local
 * bookmarks from the remote store after sign-in, this use case wipes them so the feed
 * does not show a previous user's bookmarks in the signed-out state.
 */
class ClearBookmarksOnSignOutUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke() {
        newsRepository.clearLocalBookmarks()
    }
}
