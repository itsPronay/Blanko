package com.pronaycoding.blanko.core.common.platform

/**
 * A single looping audio track.
 *
 * Implementations loop their source forever and never take audio focus, so several tracks can
 * play at the same time (a Java Sound mixer on desktop).
 */
interface SoundPlayer {
    /** Linear volume in `0f..1f`. */
    var volume: Float

    /** True when the track is set to play and has not failed (a buffering track still counts). */
    val isActive: Boolean

    fun play()

    fun pause()

    /** Moves the playback position back to the start. */
    fun rewind()

    fun release()
}

/** Creates [SoundPlayer]s for bundled and user-provided audio. */
interface SoundPlayerFactory {
    /**
     * Creates a player for a bundled sound.
     *
     * @param fileName file name inside the `sounds/` asset folder, e.g. `rain.m4a`
     * @param onError called (on the main thread) when playback fails asynchronously
     */
    fun createBuiltIn(
        fileName: String,
        onError: () -> Unit,
    ): SoundPlayer

    /** Returns true if [path] (file path, `file://` or `content://` URI) can be played. */
    fun canPlayCustom(path: String): Boolean

    /** Creates a player for a user-provided file. See [createBuiltIn] for [onError]. */
    fun createCustom(
        path: String,
        onError: () -> Unit,
    ): SoundPlayer
}

/** Keeps the platform's "now playing" surface (e.g. a media notification) in sync. */
interface PlaybackNotifier {
    fun requestSync(
        canPlay: Boolean,
        hasAudibleMix: Boolean,
    )
}

/** Copies user-picked audio into app-private storage so it survives restarts. */
interface CustomSoundFileStore {
    /**
     * Makes sure [originalPath] is stored in the app's private directory under [soundId] and
     * returns the path to play from (the original path if copying failed).
     */
    suspend fun persist(
        soundId: Int,
        originalPath: String,
    ): String
}

/** Platform actions that leave the app. */
interface ExternalActions {
    /** Opens [url] in the default browser. Returns false if nothing could handle it. */
    fun openUrl(url: String): Boolean

    /**
     * True when [shareText] only copies the text to the clipboard (desktop has no share sheet),
     * so the UI can tell the user that it happened.
     */
    val copiesShareTextToClipboard: Boolean get() = false

    /** Shares [text] via the platform's share mechanism. Returns false if that is not possible. */
    fun shareText(
        subject: String,
        text: String,
        chooserTitle: String,
    ): Boolean
}
