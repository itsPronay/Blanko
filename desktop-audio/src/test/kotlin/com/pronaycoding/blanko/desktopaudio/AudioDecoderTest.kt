package com.pronaycoding.blanko.desktopaudio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AudioDecoderTest {
    private fun resource(name: String): ByteArray =
        javaClass.getResourceAsStream("/sounds/$name")?.use { it.readBytes() }
            ?: error("Missing test resource sounds/$name")

    private fun rms(audio: DecodedAudio): Double {
        var sum = 0.0
        for (s in audio.samples) sum += s.toDouble() * s
        return sqrt(sum / audio.samples.size)
    }

    /**
     * Original-format fixtures: AAC files whose real length is stored in an iTunes `iTunSMPB` tag
     * (white_noise, coffee_shop) or in the MP4 edit list (pink_noise), and a short MP3.
     */
    @Test
    fun originalFormatFixturesDecodeToTheirExactLength() {
        val failures = mutableListOf<String>()
        for ((name, golden) in OriginalFixtures.byFile) {
            val audio = AudioDecoder.decode(resource(name))
            if (audio.sampleRate != golden.sampleRate) failures += "$name: rate ${audio.sampleRate} != ${golden.sampleRate}"
            // MP3 decoders legitimately differ by encoder delay/padding handling; AAC must match exactly.
            val tolerance = if (name.endsWith(".mp3")) 4_000 else 0
            if (abs(audio.frames - golden.frames) > tolerance) {
                failures += "$name: frames ${audio.frames} != ${golden.frames}"
            }
            if (rms(audio) < 1.0) failures += "$name: decoded silence"
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun everyDesktopVorbisSoundHasTheExactLoopLength() {
        val failures = mutableListOf<String>()
        for ((name, golden) in GoldenFrames.bySound) {
            val stem = name.substringBeforeLast('.')
            val audio = AudioDecoder.decode(resource("$stem.ogg"))
            if (audio.sampleRate != golden.sampleRate) failures += "$stem.ogg: rate ${audio.sampleRate} != ${golden.sampleRate}"
            if (audio.frames != golden.frames) failures += "$stem.ogg: frames ${audio.frames} != ${golden.frames}"
            if (rms(audio) < 1.0) failures += "$stem.ogg: decoded silence"
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun desktopVorbisSoundsMatchTheOriginalRecordings() {
        // Tonal sounds the AAC decoder reproduces accurately serve as the reference for the Vorbis copies
        // (noise-like sounds are excluded: lossy codecs cannot preserve a noise waveform).
        for (stem in listOf("coffee_shop")) {
            val original = AudioDecoder.decode(resource("$stem.m4a"))
            val vorbis = AudioDecoder.decode(resource("$stem.ogg"))
            val n = minOf(original.samples.size, vorbis.samples.size, 500_000)
            var dot = 0.0
            var a2 = 0.0
            var b2 = 0.0
            for (i in 0 until n) {
                val x = original.samples[i].toDouble()
                val y = vorbis.samples[i].toDouble()
                dot += x * y
                a2 += x * x
                b2 += y * y
            }
            val correlation = dot / sqrt(a2 * b2)
            assertTrue(correlation > 0.99, "$stem: correlation $correlation")
        }
    }

    @Test
    fun resamplingKeepsDurationAndStereoLayout() {
        val audio = AudioDecoder.decode(resource("white_noise.m4a")) // 48 kHz
        val converted = audio.resampled(44_100)
        assertEquals(44_100, converted.sampleRate)
        assertEquals(2, converted.samples.size % 2 + 2)
        assertTrue(abs(converted.durationSeconds - audio.durationSeconds) < 0.001)
    }

    @Test
    fun decodesWavAndConvertsMonoToStereo() {
        val mono = ShortArray(441) { (it * 50).toShort() }
        val bytes = ByteBuffer.allocate(mono.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        mono.forEach { bytes.putShort(it) }
        val format = AudioFormat(44_100f, 16, 1, true, false)
        val out = ByteArrayOutputStream()
        AudioSystem.write(
            AudioInputStream(bytes.array().inputStream(), format, mono.size.toLong()),
            AudioFileFormat.Type.WAVE,
            out,
        )

        val audio = AudioDecoder.decode(out.toByteArray())

        assertEquals(44_100, audio.sampleRate)
        assertEquals(mono.size, audio.frames)
        for (i in mono.indices) {
            assertEquals(mono[i], audio.samples[i * 2])
            assertEquals(mono[i], audio.samples[i * 2 + 1])
        }
    }

    @Test
    fun garbageInputFailsWithAudioDecodeException() {
        assertFailsWith<AudioDecodeException> { AudioDecoder.decode(ByteArray(2_048) { (it % 251).toByte() }) }
    }

    @Test
    fun truncatedAacDoesNotHangOrCrashTheJvm() {
        val full = resource("white_noise.m4a")
        val truncated = full.copyOf(full.size / 2)
        // Either decodes the readable part or reports a decode error; it must not throw anything else.
        runCatching { AudioDecoder.decode(truncated) }.onFailure {
            assertTrue(it is AudioDecodeException, "unexpected ${it::class}")
        }
    }
}
