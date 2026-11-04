package com.pronaycoding.blanko.core.common.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

/** File extensions the desktop audio engine can decode. */
private val SupportedExtensions = setOf("mp3", "m4a", "mp4", "wav", "ogg", "oga", "aif", "aiff", "au")

@Composable
actual fun rememberAudioPicker(onResult: (String?) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    return remember(scope) {
        {
            scope.launch {
                // The native dialog runs its own event loop on the UI thread until it is closed.
                val path = withContext(Dispatchers.Main) { pickAudioFile() }
                currentOnResult.value(path)
            }
            Unit
        }
    }
}

private fun pickAudioFile(): String? {
    val dialog = FileDialog(null as Frame?, "Select audio file", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in SupportedExtensions }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return java.io.File(dialog.directory, file).absolutePath
}
