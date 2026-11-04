package com.pronaycoding.blanko.desktopaudio

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Software mixer that plays any number of looping tracks through **one** output line.
 *
 * Mixing everything in software means:
 * - loops are gapless (the track position simply wraps while the line keeps being fed),
 * - all tracks stay in sync, and
 * - no extra audio lines are needed (some Linux setups cannot open several lines at once).
 *
 * Decoding happens on background threads the first time a track is played; until it is ready the
 * track already reports [MixerTrack.isActive], like a buffering player.
 *
 * @param outputRate sample rate every track is converted to and the output is opened with
 * @param sinkFactory creates the output for [outputRate]; the default uses Java Sound
 * @param chunkFrames frames mixed per write (1024 frames is about 23 ms at 44.1 kHz)
 */
class AudioMixer(
    val outputRate: Int = 44_100,
    private val sinkFactory: (sampleRate: Int) -> AudioSink = { JavaSoundSink(it) },
    private val chunkFrames: Int = 1_024,
) : AutoCloseable {
    private val tracks = CopyOnWriteArrayList<TrackImpl>()
    private val lock = Object()

    @Volatile
    private var closed = false
    private var thread: Thread? = null

    private val loaders =
        Executors.newCachedThreadPool(
            ThreadFactory { runnable ->
                Thread(runnable, "blanko-audio-decoder").apply { isDaemon = true }
            },
        )

    /**
     * Creates a looping track.
     *
     * @param name used for thread names and messages
     * @param loader decodes the audio; called on a background thread the first time the track plays
     * @param onError called (on a background thread) if decoding or the output device fails
     */
    fun createTrack(
        name: String,
        loader: () -> DecodedAudio,
        onError: (Throwable) -> Unit = {},
    ): MixerTrack {
        check(!closed) { "Mixer is closed" }
        val track = TrackImpl(name, loader, onError)
        tracks.add(track)
        return track
    }

    private fun ensureThread() {
        synchronized(lock) {
            if (thread == null && !closed) {
                thread =
                    Thread(::mixLoop, "blanko-audio-mixer").apply {
                        isDaemon = true
                        start()
                    }
            }
        }
    }

    private fun wake() {
        synchronized(lock) { lock.notifyAll() }
    }

    private fun mixLoop() {
        val sink = sinkFactory(outputRate)
        val accumulator = IntArray(chunkFrames * 2)
        val bytes = ByteArray(chunkFrames * 4)
        var running = false
        var opened = false
        try {
            while (!closed) {
                val audible = tracks.filter { it.isAudible }
                if (audible.isEmpty()) {
                    if (running) {
                        runCatching { sink.stop() }
                        running = false
                    }
                    synchronized(lock) {
                        if (!closed && tracks.none { it.isAudible }) lock.wait(250)
                    }
                    continue
                }
                if (!opened) {
                    try {
                        sink.open()
                        opened = true
                    } catch (e: Throwable) {
                        audible.forEach { it.fail(e) }
                        continue
                    }
                }
                if (!running) {
                    sink.start()
                    running = true
                }

                accumulator.fill(0)
                audible.forEach { it.mixInto(accumulator, chunkFrames, sink.sampleRate) }
                for (i in accumulator.indices) {
                    val sample = accumulator[i].coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    bytes[i * 2] = sample.toByte()
                    bytes[i * 2 + 1] = (sample shr 8).toByte()
                }
                sink.write(bytes, bytes.size)
            }
        } catch (e: Throwable) {
            tracks.forEach { it.fail(e) }
        } finally {
            runCatching { sink.close() }
        }
    }

    override fun close() {
        closed = true
        wake()
        tracks.forEach { it.release() }
        loaders.shutdownNow()
        thread?.join(2_000)
    }

    private inner class TrackImpl(
        private val name: String,
        private val loader: () -> DecodedAudio,
        private val onError: (Throwable) -> Unit,
    ) : MixerTrack {
        @Volatile private var wantsToPlay = false

        @Volatile private var failed = false

        @Volatile private var released = false

        @Volatile private var loading = false

        @Volatile private var audio: DecodedAudio? = null

        @Volatile private var rewindRequested = false

        @Volatile private var targetVolume = 1f

        // Only touched by the mixer thread.
        private var position = 0
        private var appliedVolume = -1f

        override var volume: Float
            get() = targetVolume
            set(value) {
                targetVolume = value.coerceIn(0f, 1f)
            }

        override val isActive: Boolean get() = wantsToPlay && !failed && !released

        /** True when the mixer should currently pull samples from this track. */
        val isAudible: Boolean get() = isActive && audio != null

        override fun play() {
            if (released || failed) return
            wantsToPlay = true
            if (audio == null) startLoading()
            ensureThread()
            wake()
        }

        override fun pause() {
            wantsToPlay = false
        }

        override fun rewind() {
            rewindRequested = true
        }

        override fun release() {
            released = true
            wantsToPlay = false
            audio = null
            tracks.remove(this)
        }

        fun fail(error: Throwable) {
            if (failed || released) return
            failed = true
            wantsToPlay = false
            onError(error)
        }

        private fun startLoading() {
            synchronized(this) {
                if (loading || audio != null) return
                loading = true
            }
            try {
                loaders.execute {
                    try {
                        val decoded = loader().resampled(outputRate)
                        if (!released) audio = decoded
                    } catch (e: Throwable) {
                        fail(e)
                    } finally {
                        loading = false
                        wake()
                    }
                }
            } catch (e: Throwable) {
                loading = false
                fail(e)
            }
        }

        /** Adds [frames] frames of this track (looping) into [accumulator]. Mixer thread only. */
        fun mixInto(
            accumulator: IntArray,
            frames: Int,
            @Suppress("UNUSED_PARAMETER") rate: Int,
        ) {
            val current = audio ?: return
            val samples = current.samples
            if (samples.isEmpty()) return
            if (rewindRequested) {
                position = 0
                rewindRequested = false
            }
            val target = targetVolume
            if (appliedVolume < 0f) appliedVolume = target
            val gainStart = appliedVolume
            val gainStep = (target - gainStart) / frames
            var pos = position
            var gain = gainStart
            for (frame in 0 until frames) {
                if (pos >= samples.size) pos = 0
                val fixedGain = (gain * 256f).toInt()
                accumulator[frame * 2] += (samples[pos].toInt() * fixedGain) shr 8
                accumulator[frame * 2 + 1] += (samples[pos + 1].toInt() * fixedGain) shr 8
                pos += 2
                gain += gainStep
            }
            position = if (pos >= samples.size) 0 else pos
            appliedVolume = target
        }

        override fun toString() = "MixerTrack($name)"
    }
}

/** A looping track created by [AudioMixer.createTrack]. All methods are thread-safe. */
interface MixerTrack {
    /** Linear volume in `0f..1f`. */
    var volume: Float

    /** True when the track is set to play and has not failed (a still-decoding track counts). */
    val isActive: Boolean

    fun play()

    fun pause()

    /** Restarts the loop from the beginning (takes effect on the next mixed chunk). */
    fun rewind()

    fun release()
}
