package com.example.news.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.news.domain.model.Article
import com.example.news.presentation.bookmarks.BookmarksScreenEvents
import com.example.news.ui.components.ArticleCard
import com.example.news.ui.mapper.toUiModel

@Composable
fun BookmarksFeedSection(
    pagedArticles: LazyPagingItems<Article>,
    onArticleClick: (String) -> Unit,
    events: BookmarksScreenEvents
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            count = pagedArticles.itemCount,
            key = pagedArticles.itemKey { it.id }
        ) { index ->
            val article = pagedArticles[index]
            if (article != null) {
                ArticleCard(
                    article = article.toUiModel(),
                    onArticleClick = onArticleClick,
                    onBookmarkToggle = { articleId ->
                        events.onBookmarkToggle(articleId)
                    }
                )
            }
        }

        if (pagedArticles.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}
