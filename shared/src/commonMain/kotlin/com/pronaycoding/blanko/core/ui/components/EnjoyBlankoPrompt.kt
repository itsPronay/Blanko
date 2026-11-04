package com.pronaycoding.blanko.core.ui.components

/**
 * "Enjoy Blanko" prompt dialog component.
 *
 * This composable displays a series of dialogs to users after a certain number of app launches
 * (configured via [Constants.ENJOY_PROMPT_TRIGGER_LAUNCH]).
 *
 * Dialog sequence:
 * 1. Initial prompt asking "Are you enjoying Blanko?"
 * 2. Opens GitHub repository directly if user taps "Yes"
 *
 * Features:
 * - Shown only once per app installation (tracked via preferences)
 * - Automatic dismissal with state management
 * - Opens GitHub link in external browser
 *
 * @see Constants.ENJOY_PROMPT_TRIGGER_LAUNCH for trigger threshold
 * @see PreferenceManagerRepository for preference persistence
 */

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.pronaycoding.blanko.resources.*
import com.pronaycoding.blanko.core.common.Constants
import com.pronaycoding.blanko.core.common.util.openExternalUrl
import com.pronaycoding.blanko.core.datastore.PreferenceManagerRepository
import org.koin.compose.koinInject

@Composable
fun EnjoyBlankoPrompt(shouldShow: Boolean) {
    if (!shouldShow) return

    var showEnjoyDialog by rememberSaveable { mutableStateOf(false) }
    val preferenceManager = koinInject<PreferenceManagerRepository>()

    LaunchedEffect(Unit) {
        // Mark as shown as soon as we decide to show, so it won't loop on restarts.
        preferenceManager.setEnjoyPromptShown(true)
        showEnjoyDialog = true
    }

    if (showEnjoyDialog) {
        AlertDialog(
            onDismissRequest = { showEnjoyDialog = false },
            title = { Text(stringResource(Res.string.enjoy_prompt_title)) },
            text = { Text(stringResource(Res.string.enjoy_prompt_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnjoyDialog = false
                        openExternalUrl(Constants.GITHUB_REPO)
                    },
                ) {
                    Text(stringResource(Res.string.enjoy_prompt_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnjoyDialog = false }) {
                    Text(stringResource(Res.string.enjoy_prompt_no))
                }
            },
        )
    }
}
