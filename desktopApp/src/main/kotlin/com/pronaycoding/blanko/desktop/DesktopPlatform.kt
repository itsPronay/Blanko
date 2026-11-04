package com.pronaycoding.blanko.desktop

import com.pronaycoding.blanko.core.common.platform.CustomSoundFileStore
import com.pronaycoding.blanko.core.common.platform.ExternalActions
import com.pronaycoding.blanko.core.common.platform.PlaybackNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

/** Desktop has no media notification; the tray icon (see `Main.kt`) is the playback surface. */
object NoOpPlaybackNotifier : PlaybackNotifier {
    override fun requestSync(
        canPlay: Boolean,
        hasAudibleMix: Boolean,
    ) = Unit
}

/** Copies picked audio into the app's data directory so it keeps working if the original moves. */
class DesktopCustomSoundFileStore(
    private val directory: File,
) : CustomSoundFileStore {
    override suspend fun persist(
        soundId: Int,
        originalPath: String,
    ): String =
        withContext(Dispatchers.IO) {
            try {
                directory.mkdirs()
                val source = if (originalPath.startsWith("file:")) File(URI(originalPath)) else File(originalPath)
                val extension = source.extension.ifEmpty { "audio" }
                val target = File(directory, "sound_$soundId.$extension")
                if (!target.exists()) source.copyTo(target)
                target.absolutePath
            } catch (e: Exception) {
                System.err.println("E/CustomSoundFileStore: Error copying sound file: ${e.message}")
                originalPath
            }
        }
}

class DesktopExternalActions : ExternalActions {
    override val copiesShareTextToClipboard: Boolean = true

    override fun openUrl(url: String): Boolean {
        val uri = URI(url)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri)
            return true
        }
        // Some Linux setups lack the GNOME libraries java.awt.Desktop needs; xdg-open works there.
        return try {
            ProcessBuilder("xdg-open", url).start()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun shareText(
        subject: String,
        text: String,
        chooserTitle: String,
    ): Boolean =
        try {
            // There is no share sheet on desktop: put the message on the clipboard instead.
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            true
        } catch (e: Exception) {
            false
        }
}
