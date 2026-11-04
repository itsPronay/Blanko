package com.pronaycoding.blanko.desktopaudio

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Records everything the mixer writes and paces it like a sound card would. */
private class CaptureSink(
    override val sampleRate: Int = 44_100,
    private val failOpen: Boolean = false,
) : AudioSink {
    private val captured = java.io.ByteArrayOutputStream()
    private val enough = CountDownLatch(1)

    @Volatile var started = false

    @Volatile var stopped = false

    override fun open() {
        if (failOpen) throw IllegalStateException("no audio device")
    }

    override fun start() {
        started = true
        stopped = false
    }

    override fun stop() {
        stopped = true
    }

    override fun write(
        data: ByteArray,
        length: Int,
    ) {
        synchronized(captured) { captured.write(data, 0, length) }
        Thread.sleep(2)
    }

    override fun close() = Unit

    fun awaitBytes(
        count: Int,
        timeoutMs: Long = 5_000,
    ): ByteArray {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            synchronized(captured) { if (captured.size() >= count) return captured.toByteArray() }
            Thread.sleep(5)
        }
        error("Timed out waiting for $count bytes")
    }
}

class AudioMixerTest {
    /** 8-frame ramp: left = 1000 * frame, right = -1000 * frame. */
    private fun ramp(frames: Int = 8) =
        DecodedAudio(
            44_100,
            ShortArray(frames * 2) { if (it % 2 == 0) (it / 2 * 1000).toShort() else (-(it / 2) * 1000).toShort() },
        )

    private fun List<Short>.toPcm(bytes: ByteArray) =
        List(bytes.size / 2) { ((bytes[it * 2].toInt() and 0xFF) or (bytes[it * 2 + 1].toInt() shl 8)).toShort() }

    private fun pcm(bytes: ByteArray): List<Short> = emptyList<Short>().toPcm(bytes)

    @Test
    fun loopsWithoutGapsAcrossTheWrapPoint() {
        val sink = CaptureSink()
        AudioMixer(sinkFactory = { sink }, chunkFrames = 64).use { mixer ->
            val track = mixer.createTrack("ramp", { ramp() })
            track.play()
            val samples = pcm(sink.awaitBytes(64 * 4 * 4))

            // Every frame must equal the ramp at (index mod 8): no silence or repeats at the loop seam.
            for (frame in 0 until 64 * 4) {
                val expectedLeft = ((frame % 8) * 1000).toShort()
                assertEquals(expectedLeft, samples[frame * 2], "left sample of frame $frame")
                assertEquals((-(frame % 8) * 1000).toShort(), samples[frame * 2 + 1], "right sample of frame $frame")
            }
        }
    }

    @Test
    fun volumeScalesTheSignal() {
        val sink = CaptureSink()
        AudioMixer(sinkFactory = { sink }, chunkFrames = 64).use { mixer ->
            val track = mixer.createTrack("ramp", { ramp() })
            track.volume = 0.5f
            track.play()
            val samples = pcm(sink.awaitBytes(64 * 4 * 2))
            for (frame in 0 until 64 * 2) {
                assertEquals((frame % 8) * 500, samples[frame * 2].toInt(), "frame $frame")
            }
        }
    }

    @Test
    fun tracksAreSummedAndClipped() {
        val sink = CaptureSink()
        val loud = DecodedAudio(44_100, ShortArray(16) { 30_000 })
        AudioMixer(sinkFactory = { sink }, chunkFrames = 32).use { mixer ->
            mixer.createTrack("a", { loud }).play()
            mixer.createTrack("b", { loud }).play()
            // Wait until both tracks are decoded and audible, then look at a later chunk.
            val samples = pcm(sink.awaitBytes(32 * 4 * 6))
            val tail = samples.takeLast(64)
            assertTrue(tail.all { it == Short.MAX_VALUE }, "two 30000 tracks must clip to 32767, got ${tail.take(4)}")
        }
    }

    @Test
    fun pauseKeepsPositionAndStopsOutput() {
        val sink = CaptureSink()
        AudioMixer(sinkFactory = { sink }, chunkFrames = 64).use { mixer ->
            val track = mixer.createTrack("ramp", { ramp() })
            track.play()
            assertTrue(track.isActive)
            sink.awaitBytes(64 * 4)
            track.pause()
            assertFalse(track.isActive)
            val deadline = System.currentTimeMillis() + 3_000
            while (!sink.stopped && System.currentTimeMillis() < deadline) Thread.sleep(5)
            assertTrue(sink.stopped, "device should be paused when nothing plays")

            track.play()
            assertTrue(track.isActive)
            sink.awaitBytes(64 * 4 * 6)
        }
    }

    @Test
    fun decodeFailureIsReportedAndTrackBecomesInactive() {
        val reported = AtomicReference<Throwable>()
        val latch = CountDownLatch(1)
        AudioMixer(sinkFactory = { CaptureSink() }).use { mixer ->
            val track =
                mixer.createTrack("bad", { throw AudioDecodeException("corrupt") }) {
                    reported.set(it)
                    latch.countDown()
                }
            track.play()
            assertTrue(latch.await(3, TimeUnit.SECONDS), "onError must be called")
            assertNotNull(reported.get())
            assertFalse(track.isActive)
        }
    }

    @Test
    fun missingAudioDeviceIsReportedToPlayingTracks() {
        val latch = CountDownLatch(1)
        AudioMixer(sinkFactory = { CaptureSink(failOpen = true) }).use { mixer ->
            val track = mixer.createTrack("ramp", { ramp() }) { latch.countDown() }
            track.play()
            assertTrue(latch.await(3, TimeUnit.SECONDS), "onError must be called when the device cannot be opened")
            assertFalse(track.isActive)
        }
    }

    @Test
    fun rewindRestartsTheLoop() {
        val sink = CaptureSink()
        AudioMixer(sinkFactory = { sink }, chunkFrames = 5).use { mixer ->
            val track = mixer.createTrack("ramp", { ramp(8) })
            track.play()
            sink.awaitBytes(5 * 4 * 3)
            track.rewind()
            // After the rewind is applied, a chunk boundary starts again at frame 0 of the ramp.
            val samples = pcm(sink.awaitBytes(5 * 4 * 20))
            var foundRestart = false
            for (chunk in 3 until 20) {
                if (samples[chunk * 10].toInt() == 0 && samples[chunk * 10 + 2].toInt() == 1000) foundRestart = true
            }
            assertTrue(foundRestart, "a chunk should start at the beginning of the ramp after rewind()")
        }
    }
}
