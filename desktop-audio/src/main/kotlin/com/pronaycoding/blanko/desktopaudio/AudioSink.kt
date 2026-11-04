package com.pronaycoding.blanko.desktopaudio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Destination for the mixed audio: 16-bit signed little-endian **stereo** at [sampleRate].
 *
 * Abstracted so the mixer can be tested without a sound card.
 */
interface AudioSink : AutoCloseable {
    val sampleRate: Int

    /** Acquires the output device. May throw if there is no usable audio device. */
    fun open()

    fun start()

    /** Lets already-written audio finish, then pauses the device. */
    fun stop()

    /** Writes [length] bytes, blocking until the device accepted them (this paces the mixer). */
    fun write(
        data: ByteArray,
        length: Int,
    )
}

/** [AudioSink] backed by a single Java Sound [SourceDataLine] (WASAPI/DirectSound on Windows, ALSA/Pulse on Linux). */
class JavaSoundSink(
    override val sampleRate: Int = 44_100,
    private val bufferFrames: Int = 8_192,
) : AudioSink {
    private var line: SourceDataLine? = null

    override fun open() {
        if (line != null) return
        val format = AudioFormat(sampleRate.toFloat(), 16, 2, true, false)
        val opened = AudioSystem.getSourceDataLine(format)
        opened.open(format, bufferFrames * 4)
        line = opened
    }

    override fun start() {
        line?.start()
    }

    override fun stop() {
        line?.let {
            it.drain()
            it.stop()
        }
    }

    override fun write(
        data: ByteArray,
        length: Int,
    ) {
        line?.write(data, 0, length)
    }

    override fun close() {
        line?.let {
            it.stop()
            it.flush()
            it.close()
        }
        line = null
    }
}
