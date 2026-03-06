package com.example.news.ui.auth

import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.news.R
import com.example.news.presentation.auth.ConfirmScreenEvents
import com.example.news.ui.theme.NewsTheme

@Composable
fun ConfirmFormSection(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    events: ConfirmScreenEvents
) {
    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
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
        onClick = { events.onConfirmSignUp(email, code) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading && code.isNotBlank(),
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
        onClick = { events.onResendCode(email) },
        enabled = !isLoading
    ) {
        Text(stringResource(R.string.action_resend_code))
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmFormSectionPreview() {
    NewsTheme {
        ConfirmFormSection(
            email = "user@example.com",
            code = "123456",
            onCodeChange = {},
            isLoading = false,
            errorMessage = null,
            events = ConfirmScreenEvents
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmFormSectionErrorPreview() {
    NewsTheme {
        ConfirmFormSection(
            email = "user@example.com",
            code = "000000",
            onCodeChange = {},
            isLoading = false,
            errorMessage = "Invalid confirmation code.",
            events = ConfirmScreenEvents
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmFormSectionLoadingPreview() {
    NewsTheme {
        ConfirmFormSection(
            email = "user@example.com",
            code = "123456",
            onCodeChange = {},
            isLoading = true,
            errorMessage = null,
            events = ConfirmScreenEvents
        )
    }
}
