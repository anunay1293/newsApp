package com.example.news.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.news.R
import com.example.news.presentation.home.HomeUiEvent
import com.example.news.presentation.home.HomeUiState
import com.example.news.presentation.home.HomeViewModel
import com.example.news.ui.model.ArticleUiModel
import com.example.news.ui.theme.NewsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** All available news category slugs shown in the category dropdown selector. */
private val NEWS_CATEGORIES = listOf(
    "general",
    "technology",
    "business",
    "sports",
    "health",
    "science",
    "entertainment"
)

/**
 * Main composable for the news feed screen with a collapsible header.
 *
 * Uses a [Scaffold] with a custom [HomeCollapsibleHeader] as the top bar. The header
 * displays the app title (always pinned), subtitle, category dropdown, and search field
 * (collapsible). As the user scrolls through the article list, the collapsible portion
 * fades out and the header shrinks to show only the pinned title bar. Scrolling back to
 * the top re-expands the full header.
 *
 * The collapse behavior is driven by [TopAppBarDefaults.exitUntilCollapsedScrollBehavior]:
 * the header collapses when scrolling down and re-expands once the list is scrolled back
 * to the top. A [nestedScroll] modifier on the [Scaffold] bridges scroll events from the
 * [LazyColumn] to the top bar's [TopAppBarScrollBehavior].
 *
 * The article list is backed by Paging 3 and handles all load states (initial loading,
 * appending, errors, empty results) with appropriate visual feedback.
 *
 * Pull-to-refresh is implemented via Material 3's [PullToRefreshBox] wrapping the **entire
 * Scaffold**. Placing it at the outermost level of the nested-scroll chain ensures the
 * collapsible header's [exitUntilCollapsedScrollBehavior] consumes overscroll first (to
 * re-expand the header); only after the header is fully expanded **and** the list is at the
 * top does remaining overscroll reach [PullToRefreshBox] to start the pull indicator. The
 * gesture triggers a fresh API call whose results are upserted into Room; the UI continues
 * to observe Room exclusively (SSOT). A custom indicator shows "Pull to refresh" with a
 * downward arrow during the drag, switching to a spinner once the refresh begins.
 *
 * The [HomeViewModel] is provided by Hilt via [hiltViewModel]. User interactions are
 * forwarded to the ViewModel via [HomeUiEvent] instances.
 *
 * @param onArticleClick Callback invoked with the article's ID when the user taps an article
 *                       card, used to navigate to the in-app article detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onArticleClick: (String) -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val pagedArticles = viewModel.pagedArticles.collectAsLazyPagingItems()
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isPullRefreshing,
        onRefresh = { viewModel.handleEvent(HomeUiEvent.OnPullToRefresh) },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshIndicator(
                isPullRefreshing = uiState.isPullRefreshing,
                distanceFraction = pullToRefreshState.distanceFraction,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                HomeCollapsibleHeader(
                    uiState = uiState,
                    isDropdownExpanded = isDropdownExpanded,
                    onDropdownExpandedChange = { isDropdownExpanded = it },
                    onEvent = viewModel::handleEvent,
                    scrollBehavior = scrollBehavior
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            if (uiState.errorMessage != null && pagedArticles.itemCount == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.error_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = uiState.errorMessage ?: stringResource(R.string.error_unknown),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Button(
                                onClick = { viewModel.handleEvent(HomeUiEvent.OnRetryClicked) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.action_retry),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    items(
                        count = pagedArticles.itemCount,
                        key = pagedArticles.itemKey { it.id }
                    ) { index ->
                        val article = pagedArticles[index]
                        if (article != null) {
                            ArticleCard(
                                article = article,
                                onArticleClick = onArticleClick,
                                onBookmarkToggle = { articleId ->
                                    viewModel.handleEvent(HomeUiEvent.OnBookmarkToggle(articleId))
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

                    if (pagedArticles.loadState.append is LoadState.Error) {
                        item {
                            val error = (pagedArticles.loadState.append as LoadState.Error).error
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.error_loading_more, error.message ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    if (pagedArticles.loadState.refresh is LoadState.NotLoading &&
                        pagedArticles.itemCount == 0
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.empty_no_articles),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (uiState.searchQuery.isNotEmpty()) {
                                            stringResource(R.string.empty_adjust_search)
                                        } else {
                                            stringResource(R.string.empty_check_later)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                if (pagedArticles.loadState.refresh is LoadState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.loading_articles),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (uiState.isRefreshing && !uiState.isPullRefreshing && pagedArticles.itemCount > 0) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    Surface(
                        modifier = Modifier.padding(top = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.refreshing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

/**
 * Custom pull-to-refresh indicator that replaces Material 3's default circular spinner.
 *
 * During the pull gesture (before the refresh threshold is reached) the indicator shows a
 * downward arrow icon alongside a "Pull to refresh" label. Once the refresh begins, the
 * arrow is replaced with a small [CircularProgressIndicator] and the label changes to
 * "Refreshing…". When the user releases without pulling past the threshold,
 * [distanceFraction] animates back to 0 and the indicator naturally disappears.
 *
 * @param isPullRefreshing  `true` while the network refresh triggered by the pull gesture
 *                          is in progress.
 * @param distanceFraction  Current pull distance as a fraction of the threshold (0 = idle,
 *                          1 = threshold reached). Provided by [PullToRefreshState].
 * @param modifier          Modifier forwarded to the root layout (typically aligned to
 *                          [Alignment.TopCenter] inside the [PullToRefreshBox]).
 */
@Composable
private fun PullToRefreshIndicator(
    isPullRefreshing: Boolean,
    distanceFraction: Float,
    modifier: Modifier = Modifier
) {
    val isVisible = distanceFraction > 0f || isPullRefreshing

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier.padding(top = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPullRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.refreshing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_pull_to_refresh_arrow),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.pull_to_refresh),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Custom collapsible header for the home screen.
 *
 * Renders a **pinned title bar** that remains visible at all times, and a **collapsible
 * section** containing the subtitle, category dropdown, and search field. The collapsible
 * section fades out (via [graphicsLayer] alpha) and is progressively clipped from the
 * bottom as the user scrolls down through the article list.
 *
 * Uses a custom [Layout] to measure the pinned and collapsible sections independently,
 * set the [TopAppBarScrollBehavior]'s height offset limit to the collapsible section's
 * measured height, and dynamically reduce the overall layout height based on the current
 * scroll offset. The wrapping [Surface] clips content that extends beyond the shrinking
 * bounds.
 *
 * @param uiState                  Current UI state containing selected category and search query.
 * @param isDropdownExpanded       Whether the category dropdown menu is currently open.
 * @param onDropdownExpandedChange Callback to toggle the dropdown open/closed state.
 * @param onEvent                  Callback to forward [HomeUiEvent] instances to the ViewModel.
 * @param scrollBehavior           The [TopAppBarScrollBehavior] that drives the collapse animation,
 *                                 providing [TopAppBarScrollBehavior.state.heightOffset] and
 *                                 [TopAppBarScrollBehavior.state.collapsedFraction].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCollapsibleHeader(
    uiState: HomeUiState,
    isDropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    onEvent: (HomeUiEvent) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = if (collapsedFraction > 0.5f) 3.dp else 1.dp
    ) {
        Layout(
            content = {
                // Measurable 0: Pinned title section (always visible)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Measurable 1: Collapsible controls (subtitle, category, search)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = 1f - collapsedFraction }
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.label_category),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box {
                        FilledTonalButton(
                            onClick = { onDropdownExpandedChange(!isDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = uiState.selectedCategory.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.dropdown_arrow),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { onDropdownExpandedChange(false) },
                            modifier = Modifier.widthIn(min = 200.dp)
                        ) {
                            NEWS_CATEGORIES.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = category.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    onClick = {
                                        onEvent(HomeUiEvent.OnCategorySelected(category))
                                        onDropdownExpandedChange(false)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.label_search_articles),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { query ->
                            onEvent(HomeUiEvent.OnSearchQueryChanged(query))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = uiState.searchQuery.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(
                                    onClick = {
                                        onEvent(HomeUiEvent.OnSearchQueryChanged(""))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cd_clear_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            modifier = Modifier.clipToBounds()
        ) { measurables, constraints ->
            val pinnedPlaceable = measurables[0].measure(constraints)
            val collapsiblePlaceable = measurables[1].measure(constraints)

            val fullHeight = pinnedPlaceable.height + collapsiblePlaceable.height
            val collapsibleHeight = collapsiblePlaceable.height

            if (scrollBehavior.state.heightOffsetLimit != -collapsibleHeight.toFloat()) {
                scrollBehavior.state.heightOffsetLimit = -collapsibleHeight.toFloat()
            }

            val scrollOffset = scrollBehavior.state.heightOffset
            val currentHeight = (fullHeight + scrollOffset)
                .roundToInt()
                .coerceAtLeast(pinnedPlaceable.height)

            layout(constraints.maxWidth, currentHeight) {
                pinnedPlaceable.place(0, 0)
                collapsiblePlaceable.place(0, pinnedPlaceable.height)
            }
        }
    }
}

/**
 * Displays a single article card with image, title, author, and date.
 * Clicking the card navigates to the in-app article detail screen.
 * Includes a bookmark icon to toggle bookmark state.
 *
 * @param article          The [ArticleUiModel] data to render.
 * @param onArticleClick   Callback invoked with the article's ID when the card is tapped.
 * @param onBookmarkToggle Callback invoked with the article's ID when the bookmark icon is tapped.
 */
@Composable
private fun ArticleCard(
    article: ArticleUiModel,
    onArticleClick: (String) -> Unit,
    onBookmarkToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArticleClick(article.id) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            article.imageUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = article.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.titleLarge.lineHeight
                    )
                    IconButton(
                        onClick = { onBookmarkToggle(article.id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (article.isBookmarked) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (article.isBookmarked) {
                                stringResource(R.string.cd_remove_bookmark)
                            } else {
                                stringResource(R.string.cd_add_bookmark)
                            },
                            tint = if (article.isBookmarked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = article.author ?: stringResource(R.string.author_unknown),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = formatDate(article.publishedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Converts a Unix-epoch millisecond timestamp into a human-readable date string.
 *
 * @param timestamp The publication date expressed as milliseconds since the Unix epoch.
 * @return A formatted date string in the pattern "MMM dd, yyyy" (e.g., "Jan 15, 2025"),
 *         using the device's default locale.
 */
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

/** Android Studio preview for the [HomeScreen] composable. */
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NewsTheme {
        HomeScreen(onArticleClick = {})
    }
}
