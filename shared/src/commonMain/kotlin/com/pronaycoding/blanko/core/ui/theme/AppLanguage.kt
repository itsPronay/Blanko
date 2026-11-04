package com.pronaycoding.blanko.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.pronaycoding.blanko.core.common.Constants.LANGUAGE_TAG_ARABIC
import com.pronaycoding.blanko.core.common.Constants.LANGUAGE_TAG_PERSIAN
import com.pronaycoding.blanko.core.common.Constants.LANGUAGE_TAG_SYSTEM
import com.pronaycoding.blanko.core.common.Constants.LANGUAGE_TAG_URDU
import com.pronaycoding.blanko.core.common.platform.applyAppLocale
import com.pronaycoding.blanko.core.datastore.PreferenceManagerRepository
import org.koin.compose.koinInject

/** The currently applied language tag. Changing it recomposes everything that resolves strings. */
val LocalAppLanguageTag = staticCompositionLocalOf { LANGUAGE_TAG_SYSTEM }

private val RtlLanguages = setOf(LANGUAGE_TAG_ARABIC, LANGUAGE_TAG_PERSIAN, LANGUAGE_TAG_URDU)

/**
 * Provides the user-selected language to [content] by observing the stored language tag.
 *
 * The tag is applied to the platform default locale (which Compose string resources read), and a
 * static composition local forces everything below to re-resolve its strings, so changing the
 * language takes effect in place without recreating the window or losing screen state.
 */
@Composable
fun AppLanguageProvider(content: @Composable () -> Unit) {
    val preferenceManager = koinInject<PreferenceManagerRepository>()
    val tag by remember(preferenceManager) {
        preferenceManager.languageTagFlow(LANGUAGE_TAG_SYSTEM)
    }.collectAsState(
        initial = remember(preferenceManager) { preferenceManager.getLanguageTagBlocking(LANGUAGE_TAG_SYSTEM) },
    )

    // Applied while composing (not in an effect) so the first frame already uses the right locale.
    remember(tag) { applyAppLocale(tag) }

    val systemLayoutDirection = LocalLayoutDirection.current
    val layoutDirection =
        when {
            tag in RtlLanguages -> LayoutDirection.Rtl
            tag == LANGUAGE_TAG_SYSTEM -> systemLayoutDirection
            else -> LayoutDirection.Ltr
        }

    CompositionLocalProvider(
        LocalAppLanguageTag provides tag,
        LocalLayoutDirection provides layoutDirection,
        content = content,
    )
}
