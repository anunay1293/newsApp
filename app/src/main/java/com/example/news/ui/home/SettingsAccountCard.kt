package com.example.news.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.news.R
import com.example.news.presentation.auth.AuthUiState
import com.example.news.presentation.auth.SettingsScreenEvents
import com.example.news.ui.theme.NewsTheme

@Composable
fun SettingsAccountCard(
    authState: AuthUiState,
    userEmail: String?,
    isLoading: Boolean,
    errorMessage: String?,
    events: SettingsScreenEvents,
    onNavigateToSignIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_account),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (authState) {
                is AuthUiState.CheckingSession -> {
                    SettingsCheckingSession()
                }
                is AuthUiState.SignedIn -> {
                    SettingsSignedInContent(
                        userEmail = userEmail,
                        isLoading = isLoading,
                        events = events
                    )
                }
                is AuthUiState.SignedOut,
                is AuthUiState.NeedsConfirmation -> {
                    SettingsSignedOutContent(onNavigateToSignIn = onNavigateToSignIn)
                }
            }
        }
    }
}

@Composable
private fun SettingsCheckingSession() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun SettingsSignedInContent(
    userEmail: String?,
    isLoading: Boolean,
    events: SettingsScreenEvents
) {
    userEmail?.let { email ->
        Text(
            text = stringResource(R.string.settings_signed_in_as, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }

    Button(
        onClick = { events.onSignOut() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(R.string.cd_sign_out),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                stringResource(R.string.action_sign_out),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SettingsSignedOutContent(
    onNavigateToSignIn: () -> Unit
) {
    Button(
        onClick = onNavigateToSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = stringResource(R.string.cd_sign_in),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            stringResource(R.string.action_sign_in),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsAccountCardSignedInPreview() {
    NewsTheme {
        SettingsAccountCard(
            authState = AuthUiState.SignedIn,
            userEmail = "user@example.com",
            isLoading = false,
            errorMessage = null,
            events = SettingsScreenEvents,
            onNavigateToSignIn = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsAccountCardSignedOutPreview() {
    NewsTheme {
        SettingsAccountCard(
            authState = AuthUiState.SignedOut,
            userEmail = null,
            isLoading = false,
            errorMessage = null,
            events = SettingsScreenEvents,
            onNavigateToSignIn = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsAccountCardCheckingPreview() {
    NewsTheme {
        SettingsAccountCard(
            authState = AuthUiState.CheckingSession,
            userEmail = null,
            isLoading = false,
            errorMessage = null,
            events = SettingsScreenEvents,
            onNavigateToSignIn = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsAccountCardErrorPreview() {
    NewsTheme {
        SettingsAccountCard(
            authState = AuthUiState.SignedIn,
            userEmail = "user@example.com",
            isLoading = false,
            errorMessage = "Sign out failed. Please try again.",
            events = SettingsScreenEvents,
            onNavigateToSignIn = {}
        )
    }
}
