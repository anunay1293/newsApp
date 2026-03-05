package com.example.news.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.news.ui.bookmarks.BookmarksScreen
import com.example.news.R
import com.example.news.ui.home.HomeScreen
import com.example.news.ui.home.SettingsScreen

/**
 * Sealed class defining the top-level destinations available in the main news navigation graph.
 *
 * Each subclass represents one tab in the bottom navigation bar and carries both its
 * [route] (used by Navigation Compose) and a human-readable [title] shown beneath the icon.
 *
 * @property route  The unique route string registered with the [NavHost].
 * @property title  The display label for the bottom navigation item.
 */
sealed class Screen(val route: String, @StringRes val titleRes: Int) {
    /** The main news feed tab showing categorised articles. */
    object Feed : Screen("feed", R.string.tab_feed)

    /** The bookmarks tab listing all articles the user has saved. */
    object Bookmarks : Screen("bookmarks", R.string.tab_bookmarks)

    /** The settings tab providing account management (e.g., sign-out). */
    object Settings : Screen("settings", R.string.tab_settings)
}

/** Ordered list of bottom-navigation destinations, controlling tab display order. */
val bottomNavItems = listOf(
    Screen.Feed,
    Screen.Bookmarks,
    Screen.Settings
)

/**
 * Root composable for the authenticated portion of the app.
 *
 * Sets up a [Scaffold] with a Material 3 [NavigationBar] containing three tabs (Feed,
 * Bookmarks, Settings) and a [NavHost] that renders the corresponding screen composable
 * for the selected tab.
 *
 * Navigation behaviour:
 * - Tapping a tab pops back to the start destination (Feed) while saving and restoring
 *   per-tab state, ensuring a single back-stack entry per tab.
 * - [launchSingleTop] prevents duplicate destinations when re-selecting the current tab.
 */
@Composable
fun NewsNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    is Screen.Feed -> Icons.Filled.Home
                                    is Screen.Bookmarks -> Icons.Filled.Favorite
                                    is Screen.Settings -> Icons.Filled.Settings
                                },
                                contentDescription = stringResource(screen.titleRes)
                            )
                        },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Feed.route) {
                HomeScreen()
            }
            composable(Screen.Bookmarks.route) {
                BookmarksScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

