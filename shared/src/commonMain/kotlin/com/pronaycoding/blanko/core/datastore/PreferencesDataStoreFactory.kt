package com.pronaycoding.blanko.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** File name of the preferences store. */
const val PREFERENCES_FILE_NAME = "blanko_preferences.preferences_pb"

/**
 * Creates the app's preferences [DataStore].
 *
 * There must be exactly one instance per file, so call this once (from the platform Koin module).
 *
 * @param producePath absolute path of the preferences file
 */
fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
