package com.example.news.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import com.example.news.R
import com.example.news.ui.theme.NewsTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AuthNavigationLink(
    @StringRes promptRes: Int,
    @StringRes actionRes: Int,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            stringResource(promptRes),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(actionRes),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthNavigationLinkPreview() {
    NewsTheme {
        AuthNavigationLink(
            promptRes = R.string.sign_in_prompt,
            actionRes = R.string.action_sign_up,
            onClick = {}
        )
    }
}
