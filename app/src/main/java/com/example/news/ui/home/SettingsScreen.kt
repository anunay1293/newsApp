package com.example.news.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.news.presentation.auth.SettingsScreenEvents
import com.example.news.presentation.auth.AuthViewModel

@Composable
fun SettingsScreen(
    onNavigateToSignIn: () -> Unit
) {
    val activity = LocalActivity.current as androidx.activity.ComponentActivity
    val viewModel: AuthViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val events: SettingsScreenEvents = viewModel
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsHeader()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SettingsAccountCard(
                authState = authState,
                userEmail = userEmail,
                isLoading = isLoading,
                errorMessage = errorMessage,
                events = events,
                onNavigateToSignIn = onNavigateToSignIn
            )
        }
    }
}
