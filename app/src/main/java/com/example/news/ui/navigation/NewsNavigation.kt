package com.example.news.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.news.ui.bookmarks.BookmarksScreen
import com.example.news.ui.home.HomeScreen

sealed class Screen(val route: String) {
    object Feed : Screen("feed")
    object Bookmarks : Screen("bookmarks")
}

@Composable
fun NewsNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Feed.route
    ) {
        composable(Screen.Feed.route) {
            HomeScreen()
        }
        composable(Screen.Bookmarks.route) {
            BookmarksScreen()
        }
    }
}

