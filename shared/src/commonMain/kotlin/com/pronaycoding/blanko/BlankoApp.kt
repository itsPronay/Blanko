package com.pronaycoding.blanko

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pronaycoding.blanko.core.common.Constants
import com.pronaycoding.blanko.core.datastore.PreferenceManagerRepository
import com.pronaycoding.blanko.core.ui.components.EnjoyBlankoPrompt
import com.pronaycoding.blanko.core.ui.theme.AppLanguageProvider
import com.pronaycoding.blanko.core.ui.theme.BlankoAppTheme
import org.koin.compose.koinInject

/**
 * Root of the shared UI, hosted by the desktop window.
 *
 * Applies the stored language and theme, hosts the navigation graph and the "enjoying Blanko?" prompt.
 *
 * @param overlay extra platform-specific content drawn on top of the app
 */
@Composable
fun BlankoApp(overlay: @Composable () -> Unit = {}) {
    val preferenceManager = koinInject<PreferenceManagerRepository>()
    var shouldShowEnjoyPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(preferenceManager) {
        val launchCount = preferenceManager.incrementLaunchCount()
        shouldShowEnjoyPrompt =
            launchCount == Constants.ENJOY_PROMPT_TRIGGER_LAUNCH &&
            !preferenceManager.isEnjoyPromptShown()
    }

    AppLanguageProvider {
        BlankoAppTheme {
            Surface {
                Navigation()
                EnjoyBlankoPrompt(shouldShow = shouldShowEnjoyPrompt)
                overlay()
            }
        }
    }
}
