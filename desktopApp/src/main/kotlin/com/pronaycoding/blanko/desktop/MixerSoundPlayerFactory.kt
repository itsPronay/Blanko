package com.pronaycoding.blanko.desktop

import com.pronaycoding.blanko.core.common.platform.SoundPlayer
import com.pronaycoding.blanko.core.common.platform.SoundPlayerFactory
import com.pronaycoding.blanko.desktopaudio.AudioDecoder
import com.pronaycoding.blanko.desktopaudio.AudioMixer
import com.pronaycoding.blanko.desktopaudio.MixerTrack
import java.io.File
import java.net.URI
import javax.swing.SwingUtilities

/**
 * Desktop audio engine: every sound is a looping track of one [AudioMixer].
 *
 * Bundled sounds ship as Vorbis files (`sounds/<name>.ogg`, trimmed to their exact loop length);
 * custom sounds are decoded from whatever file the user picked (MP3, M4A/AAC, WAV, OGG).
 */
class MixerSoundPlayerFactory(
    private val mixer: AudioMixer,
) : SoundPlayerFactory {
    override fun createBuiltIn(
        fileName: String,
        onError: () -> Unit,
    ): SoundPlayer {
        val resource = "/sounds/${fileName.substringBeforeLast('.')}.ogg"
        val track =
            mixer.createTrack(
                name = fileName,
                loader = {
                    val stream =
                        MixerSoundPlayerFactory::class.java.getResourceAsStream(resource)
                            ?: throw java.io.FileNotFoundException("Missing bundled sound $resource")
                    AudioDecoder.decode(stream)
                },
                onError = { reportOnUiThread(it, fileName, onError) },
            )
        return MixerSoundPlayer(track)
    }

    override fun canPlayCustom(path: String): Boolean = resolveFile(path).isFile

    override fun createCustom(
        path: String,
        onError: () -> Unit,
    ): SoundPlayer {
        val file = resolveFile(path)
        val track =
            mixer.createTrack(
                name = file.name,
                loader = { AudioDecoder.decode(file) },
                onError = { reportOnUiThread(it, file.name, onError) },
            )
        return MixerSoundPlayer(track)
    }

    private fun reportOnUiThread(
        error: Throwable,
        name: String,
        onError: () -> Unit,
    ) {
        System.err.println("E/SoundManager: Playback error for $name: ${error.message}")
        // Report errors on the UI thread so a player is never released from inside its own callback.
        SwingUtilities.invokeLater(onError)
    }

    private fun resolveFile(path: String): File =
        if (path.startsWith("file:")) File(URI(path)) else File(path)
}

private class MixerSoundPlayer(
    private val track: MixerTrack,
) : SoundPlayer {
    override var volume: Float
        get() = track.volume
        set(value) {
            track.volume = value
        }

    override val isActive: Boolean get() = track.isActive

    override fun play() = track.play()

    override fun pause() = track.pause()

    override fun rewind() = track.rewind()

    override fun release() = track.release()
}
