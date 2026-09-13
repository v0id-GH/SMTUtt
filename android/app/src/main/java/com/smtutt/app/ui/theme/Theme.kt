package com.smtutt.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VSCodeLightColorScheme = lightColorScheme(
    primary = Color(0xFF007ACC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFA),
    onPrimaryContainer = Color(0xFF00447A),
    secondary = Color(0xFF107C10),
    onSecondary = Color.White,
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1E1E1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFFF8F8F8),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFE5E5E5)
)

private val VSCodeDarkColorScheme = darkColorScheme(
    primary = Color(0xFF3794FF),
    onPrimary = Color(0xFF002244),
    primaryContainer = Color(0xFF0E639C),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF4EC9B0),
    onSecondary = Color.Black,
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF252526),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFF9E9E9E),
    outline = Color(0xFF383838)
)

@Composable
fun SmtuTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) VSCodeDarkColorScheme else VSCodeLightColorScheme
    val appColors = if (isDark) VSCodeDarkAppColors else VSCodeLightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

