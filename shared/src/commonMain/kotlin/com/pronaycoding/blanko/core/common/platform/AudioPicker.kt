package com.pronaycoding.blanko.core.common.platform

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens the platform's audio file picker.
 *
 * [onResult] receives the picked file as a path or URI string, or `null` when the user cancelled.
 */
@Composable
expect fun rememberAudioPicker(onResult: (String?) -> Unit): () -> Unit
