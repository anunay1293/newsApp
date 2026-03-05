package com.example.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.news.presentation.auth.AuthUiState
import com.example.news.presentation.auth.AuthViewModel
import com.example.news.ui.navigation.AuthNavigation
import com.example.news.ui.navigation.NewsNavigation
import com.example.news.ui.theme.NewsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity for this application, following the single-Activity architecture pattern.
 * Annotated with [AndroidEntryPoint] to enable Hilt dependency injection.
 *
 * Sets up edge-to-edge rendering and delegates all UI composition to [AuthGate], which decides
 * whether to show the authentication flow or the main news content based on the current
 * [AuthUiState].
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
                AuthGate()
            }
        }
    }
}

/**
 * Top-level composable that acts as an authentication gate for the entire app.
 *
 * Observes the [AuthUiState] from a shared [AuthViewModel] (scoped to the Activity via
 * [hiltViewModel]) and conditionally renders one of:
 * - A loading spinner while the current session is being checked ([AuthUiState.CheckingSession])
 * - The authentication navigation graph for sign-in/sign-up flows ([AuthUiState.SignedOut] or
 *   [AuthUiState.NeedsConfirmation])
 * - The main news navigation graph once the user is authenticated ([AuthUiState.SignedIn])
 *
 * The [AuthViewModel] is scoped to the Activity so that the same instance is shared across
 * all composables (auth screens, settings, etc.), ensuring a single source of truth for
 * authentication state.
 */
@Composable
fun AuthGate() {
    val activity = (LocalActivity.current as? ComponentActivity) ?: return
    val viewModel: AuthViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val authState by viewModel.authState.collectAsState()

    when (authState) {
        is AuthUiState.CheckingSession -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is AuthUiState.SignedOut -> {
            AuthNavigation(
                onAuthSuccess = {}
            )
        }
        is AuthUiState.NeedsConfirmation -> {
            AuthNavigation(
                onAuthSuccess = {}
            )
        }
        is AuthUiState.SignedIn -> {
            NewsNavigation()
        }
    }
}
