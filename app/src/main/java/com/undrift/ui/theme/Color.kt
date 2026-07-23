package com.undrift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme Colors (Coffee-inspired creamy palette)
val CoffeeLightBackground = Color(0xFFF7F0E8) // Oat milk
val CoffeeLightSurface = Color(0xFFECE0D4) // Latte foam
val CoffeeLightSurfaceVariant = Color(0xFFDED0C2) // Caramel cream
val CoffeeLightOutline = Color(0xFFC4AD97) // Hazelnut
val CoffeeLightTextPrimary = Color(0xFF3D2E26) // Espresso
val CoffeeLightTextSecondary = Color(0xFF6E574A) // Roasted

// Dark Theme Colors (Coffee-inspired deep non-black palette)
val CoffeeDarkBackground = Color(0xFF231C17) // Deep espresso
val CoffeeDarkSurface = Color(0xFF2E2420) // Mocha
val CoffeeDarkSurfaceVariant = Color(0xFF3A302A) // Dark latte
val CoffeeDarkOutline = Color(0xFF4D3F37) // Cocoa
val CoffeeDarkTextPrimary = Color(0xFFE6D5C3) // Cream
val CoffeeDarkTextSecondary = Color(0xFFA08977) // Warm stone

// Brand colors
val BrandPrimaryColor = Color(0xFFB8845A) // Warm caramel
val BrandSecondaryColor = Color(0xFF7FA47A) // Sage leaf

// Proxies for existing hardcoded colors
val DarkBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceColor: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceVariantColor: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val BorderColor: Color @Composable get() = MaterialTheme.colorScheme.outline

val BrandPrimary: Color @Composable get() = MaterialTheme.colorScheme.primary
val BrandSecondary: Color @Composable get() = MaterialTheme.colorScheme.secondary

val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
