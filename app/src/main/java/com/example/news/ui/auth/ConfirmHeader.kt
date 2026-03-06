package com.example.news.ui.auth

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.news.R
import com.example.news.ui.theme.NewsTheme

@Composable
fun ConfirmHeader(
    email: String
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
}

@Preview(showBackground = true)
@Composable
private fun ConfirmHeaderPreview() {
    NewsTheme {
        ConfirmHeader(email = "user@example.com")
    }
}
