package com.pronaycoding.blanko.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Creates the Room database inside [directory] (created if missing).
 *
 * Uses the bundled SQLite driver, so the app does not depend on a system SQLite library.
 */
fun createBlankoDatabase(directory: File): BlankoDatabase {
    directory.mkdirs()
    return Room
        .databaseBuilder<BlankoDatabase>(name = File(directory, "${BlankoDatabase.DATABASE_NAME}.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
