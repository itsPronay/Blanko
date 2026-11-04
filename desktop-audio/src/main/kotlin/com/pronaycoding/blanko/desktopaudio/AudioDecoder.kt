package com.pronaycoding.blanko.desktopaudio

import org.jcodec.codecs.aac.AACDecoder
import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Pure-JVM audio decoder: no native libraries and no system codecs are needed, so it works the same
 * in a packaged Windows/Linux app.
 *
 * Supported: AAC in MP4/M4A (JCodec), MP3 (JLayer) and whatever the JDK / installed Java Sound
 * service providers read (WAV, AIFF, AU, OGG Vorbis).
 */
object AudioDecoder {
    fun decode(file: File): DecodedAudio = decode(file.readBytes())

    fun decode(stream: InputStream): DecodedAudio = decode(stream.use { it.readBytes() })

    fun decode(bytes: ByteArray): DecodedAudio =
        try {
            when {
                isMp4(bytes) -> decodeAac(bytes)
                isMp3(bytes) -> decodeMp3(bytes)
                else -> decodeJavaSound(bytes)
            }
        } catch (e: AudioDecodeException) {
            throw e
        } catch (e: Exception) {
            throw AudioDecodeException("Could not decode audio: ${e.message}", e)
        }

    // ---------------------------------------------------------------- format sniffing

    private fun isMp4(b: ByteArray) =
        b.size > 12 && b[4] == 'f'.code.toByte() && b[5] == 't'.code.toByte() && b[6] == 'y'.code.toByte() && b[7] == 'p'.code.toByte()

    private fun isMp3(b: ByteArray) =
        b.size > 3 &&
            (
                (b[0] == 'I'.code.toByte() && b[1] == 'D'.code.toByte() && b[2] == '3'.code.toByte()) ||
                    ((b[0].toInt() and 0xFF) == 0xFF && (b[1].toInt() and 0xE0) == 0xE0)
            )

    // ---------------------------------------------------------------- AAC (MP4 / M4A)

    private fun decodeAac(bytes: ByteArray): DecodedAudio {
        val demuxer = MP4Demuxer.createMP4Demuxer(ByteBufferSeekableByteChannel.readFromByteBuffer(ByteBuffer.wrap(bytes)))
        val track = demuxer.audioTracks.firstOrNull() ?: throw AudioDecodeException("No audio track found")
        val specificConfig = track.meta.codecPrivate ?: throw AudioDecodeException("Missing AAC decoder configuration")
        val decoder = AACDecoder(specificConfig)

        var sampleRate = 0
        var channels = 0
        val chunks = ArrayList<ShortArray>()
        var totalFrames = 0L
        val frameBuffer = ByteBuffer.allocate(1 shl 17)
        while (true) {
            val packet = track.nextFrame() ?: break
            frameBuffer.clear()
            val decoded = decoder.decodeFrame(packet.data, frameBuffer)
            if (sampleRate == 0) {
                sampleRate = decoded.format.sampleRate
                channels = decoded.format.channels
            }
            val data = decoded.data.duplicate()
            data.order(if (decoded.format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
            val shorts = ShortArray(data.remaining() / 2)
            data.asShortBuffer().get(shorts)
            val stereo = toStereo(shorts, channels)
            chunks.add(stereo)
            totalFrames += stereo.size / 2
        }
        if (sampleRate == 0) throw AudioDecodeException("No audio frames decoded")

        val all = ShortArray((totalFrames * 2).toInt())
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(all, offset)
            offset += chunk.size
        }

        // AAC has encoder priming samples at the start and padding at the end. The files carry the
        // real length either in an iTunes `iTunSMPB` tag or in the MP4 edit list; trimming to it is
        // what makes a loop seamless.
        val (skip, length) = gaplessRange(bytes, track, demuxer.movie.timescale, sampleRate, totalFrames)
        val trimmed = all.copyOfRange(skip * 2, (skip + length) * 2)
        return DecodedAudio(sampleRate, trimmed)
    }

    /** Returns (framesToSkip, framesToKeep): the real audio inside the decoded stream. */
    private fun gaplessRange(
        bytes: ByteArray,
        track: org.jcodec.common.DemuxerTrack,
        movieTimescale: Int,
        sampleRate: Int,
        decodedFrames: Long,
    ): Pair<Int, Int> {
        var skip = 0L
        var keep = decodedFrames

        val smpb = parseItunesGaplessInfo(bytes)
        val edit = (track as? AbstractMP4DemuxerTrack)?.box?.edits?.firstOrNull { it.mediaTime >= 0 }
        if (smpb != null) {
            skip = smpb.delay
            keep = smpb.length
        } else if (edit != null) {
            skip = edit.mediaTime
            keep = decodedFrames - skip
            if (edit.duration > 0 && movieTimescale > 0) {
                keep = minOf(keep, edit.duration * sampleRate / movieTimescale)
            }
        }

        // Never trust metadata blindly: fall back to the whole stream if it does not fit.
        skip = skip.coerceIn(0L, decodedFrames)
        keep = keep.coerceIn(0L, decodedFrames - skip)
        if (keep <= 0L) {
            skip = 0L
            keep = decodedFrames
        }
        return skip.toInt() to keep.toInt()
    }

    private class GaplessInfo(
        val delay: Long,
        val length: Long,
    )

    private val itunesGapless = Regex("data.{8}((?: [0-9A-Fa-f]{8,16}){4,})", RegexOption.DOT_MATCHES_ALL)

    /**
     * Reads the `iTunSMPB` tag (` 00000000 <delay> <padding> <length> ...`, all hex), written by
     * iTunes, Apple's encoders and ffmpeg, if present.
     */
    private fun parseItunesGaplessInfo(bytes: ByteArray): GaplessInfo? {
        val marker = "iTunSMPB".toByteArray(Charsets.ISO_8859_1)
        val start = indexOf(bytes, marker)
        if (start < 0) return null
        val window = String(bytes, start, minOf(512, bytes.size - start), Charsets.ISO_8859_1)
        val tokens =
            itunesGapless
                .find(window)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.split(' ') ?: return null
        return try {
            GaplessInfo(delay = tokens[1].toLong(16), length = tokens[3].toLong(16))
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun indexOf(
        haystack: ByteArray,
        needle: ByteArray,
    ): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }

    // ---------------------------------------------------------------- MP3

    private fun decodeMp3(bytes: ByteArray): DecodedAudio {
        val bitstream = javazoom.jl.decoder.Bitstream(ByteArrayInputStream(bytes))
        val decoder = javazoom.jl.decoder.Decoder()
        var sampleRate = 0
        val chunks = ArrayList<ShortArray>()
        var totalFrames = 0L
        try {
            while (true) {
                val header = bitstream.readFrame() ?: break
                val output = decoder.decodeFrame(header, bitstream) as javazoom.jl.decoder.SampleBuffer
                if (sampleRate == 0) sampleRate = output.sampleFrequency
                val shorts = output.buffer.copyOf(output.bufferLength)
                val stereo = toStereo(shorts, output.channelCount)
                chunks.add(stereo)
                totalFrames += stereo.size / 2
                bitstream.closeFrame()
            }
        } finally {
            bitstream.close()
        }
        if (sampleRate == 0) throw AudioDecodeException("No MP3 frames found")
        val all = ShortArray((totalFrames * 2).toInt())
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(all, offset)
            offset += chunk.size
        }
        return DecodedAudio(sampleRate, all)
    }

    // ---------------------------------------------------------------- Java Sound (WAV, AIFF, AU, OGG ...)

    private fun decodeJavaSound(bytes: ByteArray): DecodedAudio {
        val source =
            try {
                AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
            } catch (e: Exception) {
                throw AudioDecodeException("Unsupported audio format", e)
            }
        source.use { input ->
            val base = input.format
            val pcmFormat =
                AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.sampleRate,
                    16,
                    base.channels,
                    base.channels * 2,
                    base.sampleRate,
                    false,
                )
            val pcm: AudioInputStream = AudioSystem.getAudioInputStream(pcmFormat, input)
            val raw = pcm.readAllBytes()
            val shorts = ShortArray(raw.size / 2)
            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            return DecodedAudio(base.sampleRate.toInt(), toStereo(shorts, base.channels))
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Converts interleaved [channels]-channel samples to interleaved stereo. */
    internal fun toStereo(
        samples: ShortArray,
        channels: Int,
    ): ShortArray =
        when {
            channels == 2 -> samples
            channels == 1 -> {
                val out = ShortArray(samples.size * 2)
                for (i in samples.indices) {
                    out[i * 2] = samples[i]
                    out[i * 2 + 1] = samples[i]
                }
                out
            }
            channels > 2 -> {
                val frames = samples.size / channels
                val out = ShortArray(frames * 2)
                for (i in 0 until frames) {
                    out[i * 2] = samples[i * channels]
                    out[i * 2 + 1] = samples[i * channels + 1]
                }
                out
            }
            else -> throw AudioDecodeException("Unsupported channel count: $channels")
        }
}
