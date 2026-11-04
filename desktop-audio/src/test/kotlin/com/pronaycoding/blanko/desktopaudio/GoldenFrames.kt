package com.pronaycoding.blanko.desktopaudio

/**
 * Expected native sample rate and exact audio length (in frames) of every bundled sound.
 *
 * The lengths come from the container metadata (iTunes `iTunSMPB` tag or MP4 edit list), not from
 * the decoder, so the test checks that priming/padding samples are trimmed correctly. The MP3 value
 * is ffmpeg's decoded length. Decoded audio content was additionally compared with ffmpeg's output
 * (mean absolute sample difference below 0.05).
 */
data class Golden(
    val sampleRate: Int,
    val frames: Int,
)

/** Expected values for the original-format fixtures in src/test/resources/sounds. */
object OriginalFixtures {
    val byFile: Map<String, Golden> =
        mapOf(
            "white_noise.m4a" to Golden(48000, 948000), // iTunSMPB
            "pink_noise.m4a" to Golden(48000, 960000), // elst
            "coffee_shop.m4a" to Golden(44100, 565396), // iTunSMPB
            "short_restaurant.mp3" to Golden(44100, 176400), // ffmpeg decoded length
        )
}

object GoldenFrames {
    val bySound: Map<String, Golden> =
        mapOf(
        "birds.m4a" to Golden(44100, 5649258), // iTunSMPB
        "boat.m4a" to Golden(48000, 1948933), // iTunSMPB
        "city.m4a" to Golden(44100, 944488), // iTunSMPB
        "coffee_shop.m4a" to Golden(44100, 565396), // iTunSMPB
        "fireplace.m4a" to Golden(44100, 1168513), // iTunSMPB
        "indoor_busy_restaurant.mp3" to Golden(44100, 7689600), // ffmpeg
        "pink_noise.m4a" to Golden(48000, 960000), // elst
        "rain.m4a" to Golden(44100, 4068696), // iTunSMPB
        "storm.m4a" to Golden(44100, 1131738), // elst
        "stream.m4a" to Golden(44100, 6539432), // iTunSMPB
        "summer_night.m4a" to Golden(44100, 2066332), // iTunSMPB
        "train.m4a" to Golden(44100, 2716560), // elst
        "waves.m4a" to Golden(44100, 5090425), // iTunSMPB
        "white_noise.m4a" to Golden(48000, 948000), // iTunSMPB
        "wind.m4a" to Golden(44100, 9311164), // iTunSMPB
        )
}
