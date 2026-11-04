package com.pronaycoding.blanko.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Applies the theme to the platform's system bars (a no-op on desktop, which has none).
 */
@Composable
expect fun SystemBarsEffect(
    darkTheme: Boolean,
    surfaceColor: Color,
)
