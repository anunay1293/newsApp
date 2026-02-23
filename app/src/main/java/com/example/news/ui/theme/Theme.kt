package com.example.news.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Dark color scheme using the purple/pink palette defined in Color.kt. */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/** Light color scheme using the purple/pink palette defined in Color.kt. */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Application-wide Material 3 theme wrapper.
 *
 * Chooses the appropriate color scheme based on the following priority:
 * 1. **Dynamic colors** (Android 12+) – derives colors from the user's wallpaper.
 * 2. **Dark / light fallback** – uses the manually defined [DarkColorScheme] or
 *    [LightColorScheme] when dynamic colors are unavailable or disabled.
 *
 * The [Typography] set defined in `Type.kt` is applied globally.
 *
 * @param darkTheme    Whether dark mode is active; defaults to the system setting.
 * @param dynamicColor Whether to use Material You dynamic colors on Android 12+.
 * @param content      The composable content to render within this theme.
 */
@Composable
fun NewsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}