package com.pronaycoding.blanko.feature.home

import com.pronaycoding.blanko.core.common.platform.AppLog
import com.pronaycoding.blanko.core.common.platform.SoundPlayer
import com.pronaycoding.blanko.core.common.platform.SoundPlayerFactory

/**
 * Manages audio playback for both built-in and custom sounds in the Blanko application.
 *
 * The actual audio engine is supplied by the platform through [SoundPlayerFactory]
 * (the desktop engine is a Java Sound based mixer). Every sound loops
 * seamlessly and does not take audio focus, so several sounds can be mixed.
 *
 * This class handles:
 * - Built-in sounds (e.g., rain, birds, waves) loaded from the bundled sound assets
 * - Custom sounds uploaded by users from device storage
 *
 * Key responsibilities:
 * - Creating players lazily on first play (building a player is expensive)
 * - Playing, pausing, stopping, and controlling volume of sounds
 * - Tracking playing sounds for pause/resume functionality
 * - Proper resource cleanup and error handling
 *
 * Thread-safety: use this class from the main/UI thread only.
 *
 * @param playerFactory Platform factory that creates the looping players
 *
 * @see com.pronaycoding.blanko.core.service.playback.GlobalPlaybackState for overall app playback state management
 */
class SoundManager(
    private val playerFactory: SoundPlayerFactory,
) {
    private val builtInPlayers = mutableMapOf<Int, SoundPlayer>()
    private val customPlayers = mutableMapOf<Int, SoundPlayer>()

    // Players are created lazily on first play: building a player is expensive (own playback
    // thread, decoder), and creating all of them at launch blocked the main thread.
    private val customSoundSources = mutableMapOf<Int, String>()

    // Separate sets so a built-in index and a custom id with the same number never collide.
    private val pausedBuiltInSounds = mutableSetOf<Int>()
    private val pausedCustomSounds = mutableSetOf<Int>()

    /**
     * Returns the player for a built-in sound, creating it on first use.
     *
     * @see getCardList for the list of available built-in sounds
     */
    private fun builtInPlayer(index: Int): SoundPlayer? {
        builtInPlayers[index]?.let { return it }
        val item = getCardList().getOrNull(index) ?: return null
        return try {
            playerFactory
                .createBuiltIn(item.audioFile) {
                    builtInPlayers.remove(index)?.release()
                }.also { builtInPlayers[index] = it }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error loading sound: $index - ${e.message}", e)
            null
        }
    }

    private fun customPlayer(soundId: Int): SoundPlayer? {
        customPlayers[soundId]?.let { return it }
        val source = customSoundSources[soundId] ?: return null
        return try {
            playerFactory
                .createCustom(source) {
                    customPlayers.remove(soundId)?.release()
                }.also { customPlayers[soundId] = it }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error loading custom sound: $soundId - ${e.message}", e)
            null
        }
    }

    /**
     * Registers a custom sound from a file path, `file://` URI or `content://` URI.
     *
     * Returns false if the file does not exist or cannot be played. Decoding errors
     * (e.g. corrupt file) happen asynchronously; in that case the sound is logged and
     * automatically unloaded.
     */
    fun loadCustomSound(
        soundId: Int,
        filePath: String,
    ): Boolean {
        return try {
            AppLog.d(TAG, "Loading custom sound: $soundId from path: $filePath")

            if (!playerFactory.canPlayCustom(filePath)) {
                AppLog.e(TAG, "Custom sound file does not exist: $filePath")
                return false
            }

            // Already registered (e.g. the UI was recreated while it is playing): keep the live player.
            if (customSoundSources[soundId] == filePath && customPlayers.containsKey(soundId)) return true

            customPlayers.remove(soundId)?.release() // avoid leaks if reloaded
            customSoundSources[soundId] = filePath
            AppLog.d(TAG, "Registered custom sound: $soundId")
            true
        } catch (e: Exception) {
            AppLog.e(TAG, "Error loading custom sound: $soundId - ${e.message}", e)
            false
        }
    }

    /** Volumes of built-in sounds that are playing, or paused by [pauseAllSounds]. */
    fun activeBuiltInVolumes(): Map<Int, Float> =
        builtInPlayers
            .filter { (index, player) -> (player.isActive || index in pausedBuiltInSounds) && player.volume > 0f }
            .mapValues { it.value.volume }

    /** Volumes of custom sounds that are playing, or paused by [pauseAllSounds]. */
    fun activeCustomVolumes(): Map<Int, Float> =
        customPlayers
            .filter { (id, player) -> (player.isActive || id in pausedCustomSounds) && player.volume > 0f }
            .mapValues { it.value.volume }

    /**
     * Unloads a custom sound and releases resources.
     */
    fun unloadCustomSound(soundId: Int) {
        pausedCustomSounds.remove(soundId)
        customSoundSources.remove(soundId)
        customPlayers.remove(soundId)?.let {
            try {
                it.release()
                AppLog.d(TAG, "Unloaded custom sound: $soundId")
            } catch (e: Exception) {
                AppLog.e(TAG, "Error unloading custom sound: $soundId", e)
            }
        }
    }

    /**
     * Checks if any sound is playing.
     */
    fun isAnySoundPlaying(): Boolean = builtInPlayers.values.any { it.isActive } || customPlayers.values.any { it.isActive }

    /**
     * Plays a built-in sound. If it is already playing, only the volume is updated.
     */
    fun playSound(
        soundIndex: Int,
        volume: Float,
    ) {
        AppLog.d(TAG, "Play sound called for index: $soundIndex")
        builtInPlayer(soundIndex)?.let { player ->
            player.volume = volume.coerceIn(0f, 1f)
            if (!player.isActive) {
                player.play()
                AppLog.d(TAG, "Playing sound at index: $soundIndex")
            } else {
                AppLog.d(TAG, "Sound at index: $soundIndex already playing; volume updated.")
            }
        } ?: AppLog.e(TAG, "Player not found for index: $soundIndex")
    }

    fun isBuiltInPlaying(soundIndex: Int): Boolean = builtInPlayers[soundIndex]?.isActive == true

    /**
     * Plays a custom sound. If it is already playing, only the volume is updated.
     */
    fun playCustomSound(
        soundId: Int,
        volume: Float,
    ) {
        AppLog.d(TAG, "Play custom sound called for id: $soundId")
        customPlayer(soundId)?.let { player ->
            player.volume = volume.coerceIn(0f, 1f)
            if (!player.isActive) {
                player.play()
                AppLog.d(TAG, "Playing custom sound: $soundId")
            } else {
                AppLog.d(TAG, "Custom sound: $soundId already playing; volume updated.")
            }
        } ?: AppLog.e(TAG, "Custom player not found for id: $soundId")
    }

    fun isCustomSoundPlaying(soundId: Int): Boolean = customPlayers[soundId]?.isActive == true

    /**
     * Adjusts the volume of a built-in sound.
     */
    fun controlSound(
        soundIndex: Int,
        volume: Float,
    ) {
        builtInPlayers[soundIndex]?.let {
            it.volume = volume.coerceIn(0f, 1f)
        }
    }

    /**
     * Adjusts the volume of a custom sound.
     */
    fun controlCustomSound(
        soundId: Int,
        volume: Float,
    ) {
        customPlayers[soundId]?.let {
            it.volume = volume.coerceIn(0f, 1f)
        }
    }

    /**
     * Stops a built-in sound and rewinds it to the start.
     */
    fun stopSound(soundIndex: Int) {
        pausedBuiltInSounds.remove(soundIndex)
        builtInPlayers[soundIndex]?.let {
            it.pause()
            it.rewind()
            AppLog.d(TAG, "Stopped sound at index: $soundIndex")
        }
    }

    /**
     * Stops a custom sound and rewinds it to the start.
     */
    fun stopCustomSound(soundId: Int) {
        pausedCustomSounds.remove(soundId)
        customPlayers[soundId]?.let {
            it.pause()
            it.rewind()
            AppLog.d(TAG, "Stopped custom sound: $soundId")
        }
    }

    /**
     * Pauses everything that is currently playing and remembers it for [resumeAllSounds].
     */
    fun pauseAllSounds() {
        builtInPlayers.entries.forEach { (index, player) ->
            if (player.isActive) {
                player.pause()
                pausedBuiltInSounds.add(index)
                AppLog.d(TAG, "Paused sound at index: $index")
            }
        }
        customPlayers.entries.forEach { (id, player) ->
            if (player.isActive) {
                player.pause()
                pausedCustomSounds.add(id)
                AppLog.d(TAG, "Paused custom sound: $id")
            }
        }
        AppLog.d(TAG, "All sounds paused.")
    }

    /**
     * Resumes all sounds that were paused by [pauseAllSounds].
     */
    fun resumeAllSounds() {
        pausedBuiltInSounds.forEach { index -> builtInPlayers[index]?.play() }
        pausedCustomSounds.forEach { id -> customPlayers[id]?.play() }
        pausedBuiltInSounds.clear()
        pausedCustomSounds.clear()
    }

    /**
     * Stops all sounds, rewinds them, and clears the paused records.
     */
    fun stopAllSounds() {
        builtInPlayers.values.forEach {
            it.pause()
            it.rewind()
        }
        customPlayers.values.forEach {
            it.pause()
            it.rewind()
        }
        pausedBuiltInSounds.clear()
        pausedCustomSounds.clear()
        AppLog.d(TAG, "All sounds stopped.")
    }

    /**
     * Releases all players. Call when the manager is no longer needed.
     */
    fun release() {
        builtInPlayers.values.forEach { it.release() }
        customPlayers.values.forEach { it.release() }
        builtInPlayers.clear()
        customPlayers.clear()
        customSoundSources.clear()
        pausedBuiltInSounds.clear()
        pausedCustomSounds.clear()
        AppLog.d(TAG, "SoundManager resources released.")
    }

    private companion object {
        const val TAG = "SoundManager"
    }
}
