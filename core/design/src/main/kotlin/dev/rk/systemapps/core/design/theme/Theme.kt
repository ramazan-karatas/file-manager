package dev.rk.systemapps.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = FallbackPrimaryLight,
    secondary = FallbackSecondaryLight,
    tertiary = FallbackTertiaryLight,
    error = FallbackErrorLight,
)

private val DarkScheme = darkColorScheme(
    primary = FallbackPrimaryDark,
    secondary = FallbackSecondaryDark,
    tertiary = FallbackTertiaryDark,
    error = FallbackErrorDark,
)

/**
 * Repodaki tüm uygulamaların ortak teması. Her uygulama kendi kökünde bunu sarar.
 */
@Composable
fun SystemAppsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
