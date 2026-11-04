package com.pronaycoding.blanko.core.common.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class SnackbarController {
    val hostState = SnackbarHostState()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun show(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message, duration = duration)
        }
    }

    /** Shows a localized string resource (resolved with the current app language). */
    fun show(
        message: StringResource,
        vararg formatArgs: Any,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) {
        scope.launch {
            val text = getString(message, *formatArgs)
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(text, duration = duration)
        }
    }
}
