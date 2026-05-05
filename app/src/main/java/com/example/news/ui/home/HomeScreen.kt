package com.example.news.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.news.R
import com.example.news.presentation.home.HomeScreenEvents
import com.example.news.presentation.home.HomeUiState
import com.example.news.presentation.home.HomeViewModel
import com.example.news.ui.theme.NewsTheme
import kotlin.math.roundToInt

private val NEWS_CATEGORIES = listOf(
    "general",
    "technology",
    "business",
    "sports",
    "health",
    "science",
    "entertainment"
)

private const val FOLLOWED_CATEGORY = HomeViewModel.FOLLOWED_CATEGORY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onArticleClick: (String) -> Unit,
    onSignInRequired: (String) -> Unit = {},
    onFollowSignInRequired: (String) -> Unit = {},
    pendingBookmarkArticleId: String? = null,
    onPendingBookmarkConsumed: () -> Unit = {},
    pendingFollowCategoryId: String? = null,
    onPendingFollowCategoryConsumed: () -> Unit = {}
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val events: HomeScreenEvents = viewModel
    val uiState by viewModel.uiState.collectAsState()
    val pagedArticles = viewModel.pagedArticles.collectAsLazyPagingItems()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.authRequiredForBookmark.collect { articleId ->
            onSignInRequired(articleId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.authRequiredForFollowCategory.collect { categoryId ->
            onFollowSignInRequired(categoryId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.followSnackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(pendingBookmarkArticleId) {
        val id = pendingBookmarkArticleId ?: return@LaunchedEffect
        events.onBookmarkToggle(id, false)
        onPendingBookmarkConsumed()
    }

    LaunchedEffect(pendingFollowCategoryId) {
        val id = pendingFollowCategoryId ?: return@LaunchedEffect
        if (id == FOLLOWED_CATEGORY) {
            events.onCategorySelected(FOLLOWED_CATEGORY)
        } else {
            events.onFollowCategoryToggle(id, false)
        }
        onPendingFollowCategoryConsumed()
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isPullRefreshing,
        onRefresh = { events.onPullToRefresh() },
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
                    events = events,
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.selectedCategory == FOLLOWED_CATEGORY
                    && pagedArticles.itemCount == 0
                    && pagedArticles.loadState.refresh is LoadState.NotLoading
                ) {
                    HomeFollowedEmptySection()
                } else if (uiState.errorMessage != null && pagedArticles.itemCount == 0) {
                    HomeErrorSection(
                        errorMessage = uiState.errorMessage ?: stringResource(R.string.error_unknown),
                        events = events
                    )
                } else {
                    HomeFeedSection(
                        pagedArticles = pagedArticles,
                        searchQuery = uiState.searchQuery,
                        onArticleClick = onArticleClick,
                        events = events
                    )

                    if (pagedArticles.loadState.refresh is LoadState.Loading) {
                        HomeLoadingSection()
                    }
                }

                if (uiState.isRefreshing && !uiState.isPullRefreshing && pagedArticles.itemCount > 0) {
                    Box(modifier = Modifier.align(Alignment.TopCenter)) {
                        HomeRefreshingChip()
                    }
                }
            }
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCollapsibleHeader(
    uiState: HomeUiState,
    isDropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    events: HomeScreenEvents,
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
                // Pinned section — always visible
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.selectedCategory != FOLLOWED_CATEGORY) {
                        IconButton(
                            onClick = {
                                events.onFollowCategoryToggle(
                                    uiState.selectedCategory,
                                    uiState.isCurrentCategoryFollowed
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isCurrentCategoryFollowed) {
                                    Icons.Default.Done
                                } else {
                                    Icons.Default.Add
                                },
                                contentDescription = if (uiState.isCurrentCategoryFollowed) {
                                    stringResource(R.string.cd_unfollow_category)
                                } else {
                                    stringResource(R.string.cd_follow_category)
                                },
                                tint = if (uiState.isCurrentCategoryFollowed) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }

                // Collapsible section
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
                                text = if (uiState.selectedCategory == FOLLOWED_CATEGORY) {
                                    stringResource(R.string.category_followed)
                                } else {
                                    uiState.selectedCategory.replaceFirstChar { it.uppercase() }
                                },
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
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.category_followed),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    events.onCategorySelected(FOLLOWED_CATEGORY)
                                    onDropdownExpandedChange(false)
                                }
                            )
                            HorizontalDivider()
                            NEWS_CATEGORIES.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = category.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    onClick = {
                                        events.onCategorySelected(category)
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
                            events.onSearchQueryChanged(query)
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
                                        events.onSearchQueryChanged("")
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NewsTheme {
        HomeScreen(onArticleClick = {})
    }
}
