package com.pronaycoding.blanko.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

/** Creates the preferences DataStore inside [directory] (created if missing). */
fun createPreferencesDataStore(directory: File): DataStore<Preferences> {
    directory.mkdirs()
    return createPreferencesDataStore { File(directory, PREFERENCES_FILE_NAME).absolutePath }
}
