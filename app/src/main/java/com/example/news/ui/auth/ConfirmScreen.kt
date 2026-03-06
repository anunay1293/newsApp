package com.example.news.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.news.R
import com.example.news.presentation.auth.AuthUiState
import com.example.news.presentation.auth.AuthViewModel

/**
 * Email confirmation screen composable shown after a new user registers or when a
 * sign-in attempt reveals an unconfirmed account.
 *
 * Displays the target [email] and an input field for the 6-digit confirmation code.
 * On submission, [AuthViewModel.confirmSignUp] is called via the shared [AuthViewModel]
 * (scoped to the Activity via [hiltViewModel]). A [LaunchedEffect] watches
 * [AuthViewModel.authState] for two possible outcomes:
 *
 * - [AuthUiState.SignedIn] -- auto sign-in after confirmation succeeded; [onAuthSuccess]
 *   is invoked to complete the auth flow and return the user to the main app.
 * - [AuthUiState.SignedOut] -- auto sign-in failed (e.g. network error); [onNavigateToSignIn]
 *   is invoked as a fallback so the user can sign in manually.
 *
 * A "Resend Code" button allows the user to request a fresh verification code via
 * [AuthViewModel.resendCode].
 *
 * @param email              The email address that requires verification, passed as a
 *                           navigation argument.
 * @param onAuthSuccess      Callback invoked when confirmation and auto sign-in both
 *                           succeed, completing the authentication flow.
 * @param onNavigateToSignIn Fallback callback invoked when confirmation succeeds but
 *                           auto sign-in fails, navigating to the sign-in screen.
 */
@Composable
fun ConfirmScreen(
    email: String,
    onAuthSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val activity = LocalActivity.current as androidx.activity.ComponentActivity
    val viewModel: AuthViewModel = hiltViewModel(viewModelStoreOwner = activity)
    var confirmationCode by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.SignedIn -> onAuthSuccess()
            is AuthUiState.SignedOut -> onNavigateToSignIn()
            else -> { /* NeedsConfirmation or CheckingSession -- stay on this screen */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.confirm_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = stringResource(R.string.confirm_instruction),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = confirmationCode,
            onValueChange = { confirmationCode = it },
            label = { Text(stringResource(R.string.label_confirmation_code)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.confirmSignUp(email, confirmationCode) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && confirmationCode.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.action_confirm))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { viewModel.resendCode(email) },
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.action_resend_code))
        }
    }
}

