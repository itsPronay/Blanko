package com.pronaycoding.blanko.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pronaycoding.blanko.core.common.platform.runBlockingCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of [PreferenceManagerRepository] using (multiplatform) Jetpack DataStore.
 *
 * DataStore is a modern replacement for SharedPreferences, providing:
 * - Type-safe access to preferences
 * - Encryption of sensitive data
 * - Coroutine-based async API
 * - Reactive Flows for preference changes
 *
 * This implementation stores preferences in a datastore file named "blanko_preferences".
 * All preference keys and their types are defined in the companion object's Keys class.
 *
 * Thread-safety: All methods are thread-safe and can be called from any thread.
 *
 * @param dataStore The single app-wide preferences DataStore (see [createPreferencesDataStore])
 *
 * @see PreferenceManagerRepository for the interface contract
 */
class PreferenceManagerRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : PreferenceManagerRepository {

    /**
     * Retrieves the stored theme mode using synchronous blocking.
     *
     * Uses runBlocking internally - should only be called from contexts where blocking is acceptable.
     *
     * @param defaultValue Default value if not set
     * @return The stored theme mode or defaultValue
     */
    override fun getThemeModeBlocking(defaultValue: String): String =
        runBlockingCompat {
            dataStore.data.first()[Keys.themeMode] ?: defaultValue
        }

    /**
     * Stores the theme mode preference.
     *
     * @param mode Theme mode to store
     */
    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.themeMode] = mode
        }
    }

    override fun themeModeFlow(defaultValue: String): Flow<String> =
        dataStore.data.map { it[Keys.themeMode] ?: defaultValue }

    override fun amoledDarkFlow(defaultValue: Boolean): Flow<Boolean> =
        dataStore.data.map { it[Keys.amoledDark] ?: defaultValue }

    override fun getAmoledDarkBlocking(defaultValue: Boolean): Boolean =
    runBlockingCompat {
        dataStore.data.first()[Keys.amoledDark] ?: defaultValue
    }

    override suspend fun setAmoledDark(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.amoledDark] = enabled
        }
    }

    override fun getAccentColorBlocking(defaultValue: String): String =
        runBlockingCompat {
            dataStore.data.first()[Keys.accentColor] ?: defaultValue
        }

    override fun accentColorFlow(defaultValue: String): Flow<String> =
        dataStore.data.map { it[Keys.accentColor] ?: defaultValue }

    override suspend fun setAccentColor(id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.accentColor] = id
        }
    }

    /**
     * Retrieves the stored language tag using synchronous blocking.
     *
     * The language tag is a BCP 47 locale identifier (e.g., "en", "hi", "es-MX").
     * Uses runBlocking internally - should only be called from contexts where blocking is acceptable.
     *
     * @param defaultValue Default value if not set
     * @return The stored language tag or defaultValue
     */
    override fun getLanguageTagBlocking(defaultValue: String): String =
        runBlockingCompat {
            dataStore.data.first()[Keys.languageTag] ?: defaultValue
        }

    override fun languageTagFlow(defaultValue: String): Flow<String> =
        dataStore.data.map { it[Keys.languageTag] ?: defaultValue }

    /**
     * Stores the language tag preference.
     *
     * @param tag BCP 47 language tag to store
     */
    override suspend fun setLanguageTag(tag: String) {
        dataStore.edit { prefs ->
            prefs[Keys.languageTag] = tag
        }
    }

    /**
     * Atomically increments the launch count by 1 and returns the new value.
     *
     * @return The new launch count after incrementing
     */
    override suspend fun incrementLaunchCount(): Int {
        var next = 1
        dataStore.edit { prefs ->
            next = (prefs[Keys.launchCount] ?: 0) + 1
            prefs[Keys.launchCount] = next
        }
        return next
    }

    /**
     * Retrieves the current launch count.
     *
     * @return The launch count, or 0 if not yet set
     */
    override suspend fun getLaunchCount(): Int = dataStore.data.first()[Keys.launchCount] ?: 0

    /**
     * Checks if the "Enjoy Blanko" prompt has been displayed.
     *
     * @return true if shown, false otherwise
     */
    override suspend fun isEnjoyPromptShown(): Boolean = dataStore.data.first()[Keys.enjoyPromptShown] ?: false

    /**
     * Marks the "Enjoy Blanko" prompt as shown/hidden.
     *
     * @param shown true to mark as shown
     */
    override suspend fun setEnjoyPromptShown(shown: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.enjoyPromptShown] = shown
        }
    }

    /**
     * Checks if the notification permission primer has been displayed.
     *
     * @return true if shown, false otherwise
     */
    override suspend fun isNotificationPrimerShown(): Boolean = dataStore.data.first()[Keys.notificationPrimerShown] ?: false

    /**
     * Marks the notification permission primer as shown/hidden.
     *
     * @param shown true to mark as shown
     */
    override suspend fun setNotificationPrimerShown(shown: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.notificationPrimerShown] = shown
        }
    }

    /**
     * Private object containing all DataStore preference keys.
     *
     * Each key is typed and corresponds to a specific preference value.
     */
    private object Keys {
        val themeMode: Preferences.Key<String> = stringPreferencesKey("app_theme_mode")
        val amoledDark: Preferences.Key<Boolean> = booleanPreferencesKey("amoled_dark")
        val accentColor: Preferences.Key<String> = stringPreferencesKey("accent_color")
        val languageTag: Preferences.Key<String> = stringPreferencesKey("app_language_tag")
        val launchCount: Preferences.Key<Int> = intPreferencesKey("launch_count")
        val enjoyPromptShown: Preferences.Key<Boolean> = booleanPreferencesKey("enjoy_prompt_shown")
        val notificationPrimerShown: Preferences.Key<Boolean> =
            booleanPreferencesKey("notification_primer_shown")
    }
}

