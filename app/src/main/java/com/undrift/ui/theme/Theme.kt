package com.undrift.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryColor,
    secondary = BrandSecondaryColor,
    onPrimary = CoffeeDarkTextPrimary,
    background = CoffeeDarkBackground,
    surface = CoffeeDarkSurface,
    surfaceVariant = CoffeeDarkSurfaceVariant,
    outline = CoffeeDarkOutline,
    onBackground = CoffeeDarkTextPrimary,
    onSurface = CoffeeDarkTextPrimary,
    onSurfaceVariant = CoffeeDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryColor,
    secondary = BrandSecondaryColor,
    onPrimary = CoffeeLightTextPrimary,
    background = CoffeeLightBackground,
    surface = CoffeeLightSurface,
    surfaceVariant = CoffeeLightSurfaceVariant,
    outline = CoffeeLightOutline,
    onBackground = CoffeeLightTextPrimary,
    onSurface = CoffeeLightTextPrimary,
    onSurfaceVariant = CoffeeLightTextSecondary
)

@Composable
fun UnDriftTheme(
    themeColor: Color = Color.Unspecified,
    themeMode: Int = 0, // 0 = System, 1 = Light, 2 = Dark
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
