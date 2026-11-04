package com.pronaycoding.blanko.core.common.platform

/** Wall-clock time in milliseconds since the Unix epoch. */
expect fun currentTimeMillis(): Long

/**
 * Runs [block] and blocks the calling thread until it finishes.
 *
 * Only for the few places that need a synchronous read (theme/language at startup).
 */
expect fun <T> runBlockingCompat(block: suspend () -> T): T

/** Minimal logging shared by all platforms (stdout/stderr on desktop). */
expect object AppLog {
    fun d(
        tag: String,
        message: String,
    )

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

/**
 * Applies the app language to the platform's default locale.
 *
 * @param tag BCP-47 language tag, or `Constants.LANGUAGE_TAG_SYSTEM` to restore the system locale.
 */
expect fun applyAppLocale(tag: String)
