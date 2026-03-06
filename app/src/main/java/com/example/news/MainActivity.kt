package com.example.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.example.news.ui.navigation.NewsNavigation
import com.example.news.ui.theme.NewsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity for this application, following the single-Activity architecture pattern.
 * Annotated with [AndroidEntryPoint] to enable Hilt dependency injection.
 *
 * Sets up edge-to-edge rendering and delegates all UI composition to [NewsNavigation].
 * Authentication is optional — the main news content is always accessible. Users can
 * sign in or out via the Settings tab.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Initializes the Activity, enables edge-to-edge display, and sets the Compose content
     * wrapped in the app's [NewsTheme].
     *
     * @param savedInstanceState Bundle containing the Activity's previously saved state, if any.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsTheme {
                NewsNavigation()
            }
        }
    }
}
