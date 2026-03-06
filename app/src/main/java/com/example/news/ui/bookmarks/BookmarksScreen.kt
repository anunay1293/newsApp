package com.example.news.ui.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.news.presentation.bookmarks.BookmarksScreenEvents
import com.example.news.presentation.bookmarks.BookmarksViewModel

@Composable
fun BookmarksScreen(
    onArticleClick: (String) -> Unit,
    onSignInRequired: () -> Unit = {}
) {
    val viewModel: BookmarksViewModel = hiltViewModel()
    val events: BookmarksScreenEvents = viewModel
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val pagedArticles = viewModel.pagedArticles.collectAsLazyPagingItems()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        events.onRecheckAuth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        BookmarksHeader()

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (!isSignedIn) {
                BookmarksSignInPrompt(onSignInRequired = onSignInRequired)
            } else {
                if (pagedArticles.loadState.refresh is LoadState.Loading) {
                    BookmarksLoadingSection()
                } else if (pagedArticles.itemCount == 0) {
                    BookmarksEmptySection()
                } else {
                    BookmarksFeedSection(
                        pagedArticles = pagedArticles,
                        onArticleClick = onArticleClick,
                        events = events
                    )
                }
            }
        }
    }
}
