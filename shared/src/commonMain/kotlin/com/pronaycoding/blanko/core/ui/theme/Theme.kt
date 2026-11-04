package com.pronaycoding.blanko.core.ui.theme

/**
 * Material Design 3 theme system for the Blanko application.
 *
 * This file defines:
 * - Dark and Light color schemes
 * - Theme composition with Material Design 3
 * - Support for system theme following device settings
 *
 * The theme is designed to evoke calm and relaxation, fitting the app's sleep/ambient sound purpose.
 *
 * Color scheme selection logic:
 * 1. Check user preference (light/dark/system)
 * 2. If system mode, follow device settings
 * 3. Use the custom light/dark/AMOLED color schemes, tinted with the selected accent color
 *
 * System UI integration:
 * - Platform system bars follow the theme (see [SystemBarsEffect])
 *
 * @see androidx.compose.material3.MaterialTheme for Material Design theme
 * @see PreferenceManagerRepository for theme preference storage
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pronaycoding.blanko.core.common.Constants
import com.pronaycoding.blanko.core.datastore.PreferenceManagerRepository
import org.koin.compose.koinInject

// Dark theme - Calm and minimalist like Blanket app
private val DarkColorScheme =
    darkColorScheme(
        primary = Primary, // Soft Blue
        onPrimary = Color.White,
        primaryContainer = PrimaryDark,
        onPrimaryContainer = Color.White,
        secondary = Secondary, // Cyan
        onSecondary = Color.White,
        secondaryContainer = SecondaryDark,
        onSecondaryContainer = Color.White,
        tertiary = Tertiary, // Soft Green
        onTertiary = Color.White,
        tertiaryContainer = TertiaryLight,
        onTertiaryContainer = Color.White,
        error = Error,
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = DarkBackground, // Almost black
        onBackground = LightText,
        surface = DarkSurface, // Dark card background
        onSurface = LightText,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFF49454E),
    )

// AMOLED theme
private val AmoledColorScheme =
    DarkColorScheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF121212),
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color(0xFF0A0A0A),
        surfaceContainerHigh = Color(0xFF121212),
        surfaceContainerHighest = Color(0xFF1E1E1E),
        surfaceTint = Color.Black,
    )

// Light theme
private val LightColorScheme =
    lightColorScheme(
        primary = Primary, // Soft Blue
        onPrimary = Color.White,
        primaryContainer = PrimaryLight,
        onPrimaryContainer = Color(0xFF062D5E),
        secondary = Secondary, // Cyan
        onSecondary = Color.White,
        secondaryContainer = SecondaryLight,
        onSecondaryContainer = Color(0xFF003D47),
        tertiary = Tertiary, // Soft Green
        onTertiary = Color.White,
        tertiaryContainer = TertiaryLight,
        onTertiaryContainer = Color.White,
        error = Error,
        onError = Color.White,
        errorContainer = Color(0xFFFCDEDB),
        onErrorContainer = Color(0xFF370B1E),
        background = LightBackground, // Off-white
        onBackground = DarkText,
        surface = LightSurface, // Pure white
        onSurface = DarkText,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC7D0),
    )

/** The accent used for section titles and tabs; follows the user-selected accent color. */
val LocalAccentColor = staticCompositionLocalOf { DefaultAccent }

@Composable
fun BlankoAppTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val preferenceManager = koinInject<PreferenceManagerRepository>()
    // Theme mode, AMOLED and accent are observed, so changing them recomposes the theme live.
    val themeMode by remember(preferenceManager) {
        preferenceManager.themeModeFlow(Constants.MODE_DARK)
    }.collectAsState(
        initial = remember(preferenceManager) { preferenceManager.getThemeModeBlocking(Constants.MODE_DARK) },
    )
    val amoledDark by remember(preferenceManager) {
        preferenceManager.amoledDarkFlow(false)
    }.collectAsState(
        initial = remember(preferenceManager) { preferenceManager.getAmoledDarkBlocking(false) },
    )
    val accentId by remember(preferenceManager) {
        preferenceManager.accentColorFlow(Constants.ACCENT_DEFAULT)
    }.collectAsState(
        initial = remember(preferenceManager) { preferenceManager.getAccentColorBlocking(Constants.ACCENT_DEFAULT) },
    )
    val darkTheme =
        when (themeMode) {
            Constants.MODE_LIGHT -> false
            Constants.MODE_SYSTEM -> systemDark
            else -> true
        }

    val accent =
        AccentColors
            .firstOrNull { it.id == accentId }
            ?.takeIf { it.id != Constants.ACCENT_DEFAULT }
            ?.color(darkTheme)

    val baseScheme =
        when {
            darkTheme && amoledDark -> AmoledColorScheme
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val colorScheme =
        if (accent == null) {
            baseScheme
        } else {
            baseScheme.copy(
                primary = accent,
                onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
                primaryContainer = if (darkTheme) accent.copy(alpha = 1f).darken(0.25f) else accent.lighten(0.45f),
                onPrimaryContainer = if (darkTheme) Color.White else accent.darken(0.6f),
            )
        }

    SystemBarsEffect(darkTheme = darkTheme, surfaceColor = colorScheme.surface)

    CompositionLocalProvider(LocalAccentColor provides (accent ?: DefaultAccent)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

private fun Color.darken(fraction: Float): Color =
    Color(red * (1 - fraction), green * (1 - fraction), blue * (1 - fraction), alpha)

private fun Color.lighten(fraction: Float): Color =
    Color(red + (1 - red) * fraction, green + (1 - green) * fraction, blue + (1 - blue) * fraction, alpha)
