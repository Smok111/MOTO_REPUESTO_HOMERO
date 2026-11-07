package com.khaory.taller.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = SurfaceVariant,
    secondary = OrangePrimary,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF4B2A1B),
    onSecondary = TextWhite,
    onSecondaryContainer = TextWhite,
    outline = androidx.compose.ui.graphics.Color(0xFF596168),
    onPrimary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextGray,
    error = ErrorRed
)

// We force dark mode for this specific design
private val LightColorScheme = DarkColorScheme

@Composable
fun AltokeyaaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Forced to false to keep our specific branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Even if dynamic color is requested, we stick to our dark theme
            DarkColorScheme
        }
        else -> DarkColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
