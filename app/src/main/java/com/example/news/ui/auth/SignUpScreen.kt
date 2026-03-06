package com.example.news.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.news.R
import com.example.news.presentation.auth.AuthUiState
import com.example.news.presentation.auth.AuthViewModel
import com.example.news.presentation.auth.SignUpScreenEvents
import com.example.news.ui.components.AuthHeader
import com.example.news.ui.components.AuthNavigationLink

@Composable
fun SignUpScreen(
    onNavigateToConfirm: (String) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val activity = LocalActivity.current as androidx.activity.ComponentActivity
    val viewModel: AuthViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val events: SignUpScreenEvents = viewModel
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(key1 = authState) {
        when (val state = authState) {
            is AuthUiState.NeedsConfirmation -> {
                kotlinx.coroutines.delay(100)
                onNavigateToConfirm(state.email)
            }
            is AuthUiState.SignedIn -> {}
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuthHeader(
            titleRes = R.string.sign_up_title,
            subtitleRes = R.string.sign_up_subtitle
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            SignUpFormCard(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                isLoading = isLoading,
                errorMessage = errorMessage,
                events = events
            )

            AuthNavigationLink(
                promptRes = R.string.sign_up_prompt,
                actionRes = R.string.action_sign_in,
                onClick = onNavigateToSignIn
            )
        }
    }
}
