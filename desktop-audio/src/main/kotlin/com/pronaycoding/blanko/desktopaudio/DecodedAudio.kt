package com.pronaycoding.blanko.desktopaudio

/**
 * Fully decoded audio: 16-bit signed, interleaved **stereo**.
 *
 * @property sampleRate frames per second
 * @property samples interleaved L/R samples, `frames * 2` entries
 */
class DecodedAudio(
    val sampleRate: Int,
    val samples: ShortArray,
) {
    val frames: Int get() = samples.size / 2

    val durationSeconds: Double get() = frames.toDouble() / sampleRate

    /** Returns this audio resampled to [targetRate] (linear interpolation), or `this` if equal. */
    fun resampled(targetRate: Int): DecodedAudio {
        if (targetRate == sampleRate || frames == 0) return this
        val outFrames = (frames.toLong() * targetRate / sampleRate).toInt().coerceAtLeast(1)
        val out = ShortArray(outFrames * 2)
        val step = sampleRate.toDouble() / targetRate
        for (i in 0 until outFrames) {
            val position = i * step
            val index = position.toInt().coerceAtMost(frames - 1)
            val next = (index + 1).coerceAtMost(frames - 1)
            val fraction = (position - index).toFloat()
            for (channel in 0..1) {
                val a = samples[index * 2 + channel].toInt()
                val b = samples[next * 2 + channel].toInt()
                out[i * 2 + channel] = (a + (b - a) * fraction).toInt().toShort()
            }
        }
        return DecodedAudio(targetRate, out)
    }
}

/** Thrown when audio data cannot be decoded (unknown format or corrupt data). */
class AudioDecodeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
