package com.pronaycoding.blanko.core.common.util

import androidx.compose.material3.SnackbarDuration
import com.pronaycoding.blanko.core.common.Constants
import com.pronaycoding.blanko.core.common.platform.ExternalActions
import com.pronaycoding.blanko.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.context.GlobalContext

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

private fun snackbar(): SnackbarController = GlobalContext.get().get()

private fun externalActions(): ExternalActions = GlobalContext.get().get()

/**
 * Opens an external URL in the default browser.
 *
 * Shows a snackbar when no browser is available or the URL cannot be opened.
 *
 * @param url The full URL to open (should start with http:// or https://)
 */
fun openExternalUrl(url: String) {
    val opened =
        try {
            externalActions().openUrl(url)
        } catch (e: Exception) {
            snackbar().show(Res.string.error_unexpected, duration = SnackbarDuration.Long)
            return
        }
    if (!opened) {
        snackbar().show(Res.string.no_browser_installed, duration = SnackbarDuration.Long)
    }
}

/**
 * Opens the platform share mechanism with a prefilled app recommendation message.
 */
fun shareApp() {
    scope.launch {
        try {
            val shared =
                externalActions().shareText(
                    subject = getString(Res.string.app_name),
                    text = getString(Res.string.share_app_message, "${Constants.GITHUB_REPO}/releases"),
                    chooserTitle = getString(Res.string.share_app_chooser_title),
                )
            if (!shared) {
                snackbar().show(Res.string.no_share_app_installed, duration = SnackbarDuration.Long)
            } else if (externalActions().copiesShareTextToClipboard) {
                snackbar().show(Res.string.share_link_copied)
            }
        } catch (e: Exception) {
            snackbar().show(Res.string.error_unexpected, duration = SnackbarDuration.Long)
        }
    }
}
