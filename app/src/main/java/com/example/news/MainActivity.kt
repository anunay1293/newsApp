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

class MainActivity : ComponentActivity() {
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

@Composable
fun AuthGate() {
    val application = LocalContext.current.applicationContext as Application
    val activity = (androidx.compose.ui.platform.LocalContext.current as? androidx.activity.ComponentActivity)
        ?: return
    // Use Activity's ViewModelStoreOwner to share ViewModel instance across all screens
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
                onAuthSuccess = {
                    // Navigation will be handled by state change
                }
            )
        }
        is AuthUiState.NeedsConfirmation -> {
            // Show auth navigation - individual screens will handle their own navigation
            AuthNavigation(
                onAuthSuccess = {}
            )
        }
        is AuthUiState.SignedIn -> {
            NewsNavigation()
        }
    }
}
