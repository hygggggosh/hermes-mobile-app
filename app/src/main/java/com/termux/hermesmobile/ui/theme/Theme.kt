package com.termux.hermesmobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HermesDarkColorScheme = darkColorScheme(
    primary = HermesPrimary,
    onPrimary = HermesSurface,
    primaryContainer = HermesPrimaryContainer,
    secondary = HermesSecondary,
    background = HermesBackground,
    surface = HermesSurface,
    surfaceVariant = HermesSurfaceVariant,
    onBackground = HermesOnBackground,
    onSurface = HermesOnSurface,
    error = HermesError,
    outline = HermesSurfaceVariant
)

@Composable
fun HermesMobileTheme(content: @Composable () -> Unit) {
    val colorScheme = HermesDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
