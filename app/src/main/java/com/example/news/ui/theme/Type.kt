package com.example.news.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Application-wide Material 3 typography scale.
 *
 * Currently overrides only [bodyLarge] with the default system font, standard weight,
 * 16 sp size, and 24 sp line height. All other type roles (headline, title, label, etc.)
 * fall back to the Material 3 defaults.
 *
 * To customise additional roles, uncomment and adjust the commented-out styles below.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)