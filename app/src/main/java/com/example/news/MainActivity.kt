package com.example.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.news.presentation.auth.AuthUiState
import com.example.news.presentation.auth.AuthViewModel
import com.example.news.ui.navigation.AuthNavigation
import com.example.news.ui.navigation.NewsNavigation
import com.example.news.ui.theme.NewsTheme

/**
 * The single Activity for this application, following the single-Activity architecture pattern.
 *
 * Sets up edge-to-edge rendering and delegates all UI composition to [AuthGate], which decides
 * whether to show the authentication flow or the main news content based on the current
 * [AuthUiState].
 */
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
 * Observes the [AuthUiState] from a shared [AuthViewModel] (scoped to the Activity) and
 * conditionally renders one of:
 * - A loading spinner while the current session is being checked ([AuthUiState.CheckingSession])
 * - The authentication navigation graph for sign-in/sign-up flows ([AuthUiState.SignedOut] or
 *   [AuthUiState.NeedsConfirmation])
 * - The main news navigation graph once the user is authenticated ([AuthUiState.SignedIn])
 *
 * The [AuthViewModel] is obtained using the Activity's [ViewModelStoreOwner] so that the same
 * instance is shared across all composables (auth screens, settings, etc.), ensuring a single
 * source of truth for authentication state.
 */
@Composable
fun AuthGate() {
    val application = LocalContext.current.applicationContext as Application
    val activity = (androidx.compose.ui.platform.LocalContext.current as? androidx.activity.ComponentActivity)
        ?: return
    val viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = activity,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(application) as T
            }
        }
    )
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
