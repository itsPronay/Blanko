package com.pronaycoding.blanko.core.common.platform

import com.pronaycoding.blanko.core.common.Constants.LANGUAGE_TAG_SYSTEM
import kotlinx.coroutines.runBlocking
import java.util.Locale

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun <T> runBlockingCompat(block: suspend () -> T): T = runBlocking { block() }

actual object AppLog {
    actual fun d(
        tag: String,
        message: String,
    ) {
        if (DEBUG) println("D/$tag: $message")
    }

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        System.err.println("E/$tag: $message")
        throwable?.printStackTrace()
    }

    /** Debug logging is opt-in on desktop: run with `-Dblanko.debug=true`. */
    private val DEBUG = System.getProperty("blanko.debug") == "true"
}

private val systemLocale: Locale = Locale.getDefault()

actual fun applyAppLocale(tag: String) {
    Locale.setDefault(if (tag == LANGUAGE_TAG_SYSTEM) systemLocale else Locale.forLanguageTag(tag))
}
