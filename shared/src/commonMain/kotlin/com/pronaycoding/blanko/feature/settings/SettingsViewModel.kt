package com.pronaycoding.blanko.feature.settings

/**
 * ViewModel for the Settings screen managing user preferences.
 *
 * Responsibilities:
 * - Theme preference management (light/dark/system)
 * - Language/locale preference management
 *
 * State exposed:
 * - `selectedTheme`: Current theme mode
 * - `selectedLanguage`: Current language tag (BCP 47)
 * - `themeChoices`: Available theme options
 * - `languageChoices`: Available language options
 *
 * Theme, accent and language changes are observed by the UI and applied live.
 *
 * @see PreferenceManagerRepository for preference persistence
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronaycoding.blanko.resources.*
import com.pronaycoding.blanko.core.common.Constants
import com.pronaycoding.blanko.core.common.platform.runBlockingCompat
import com.pronaycoding.blanko.core.datastore.PreferenceManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class SettingsViewModel(
    private val prefManager: PreferenceManagerRepository,
) : ViewModel() {
    val themeChoices =
        listOf(
            ThemeChoice(Constants.MODE_LIGHT, Res.string.theme_light),
            ThemeChoice(Constants.MODE_DARK, Res.string.theme_dark),
            ThemeChoice(Constants.MODE_SYSTEM, Res.string.theme_auto),
        )

    val languageChoices =
        listOf(
            LanguageChoice(Constants.LANGUAGE_TAG_SYSTEM, Res.string.language_system),
            LanguageChoice(Constants.LANGUAGE_TAG_ENGLISH, Res.string.language_english),
            LanguageChoice(Constants.LANGUAGE_TAG_CHINESE, Res.string.language_chinese),
            LanguageChoice(Constants.LANGUAGE_TAG_HINDI, Res.string.language_hindi),
            LanguageChoice(Constants.LANGUAGE_TAG_SPANISH, Res.string.language_spanish),
            LanguageChoice(Constants.LANGUAGE_TAG_KOREAN, Res.string.language_korean),
            LanguageChoice(Constants.LANGUAGE_TAG_ARABIC, Res.string.language_arabic),
            LanguageChoice(Constants.LANGUAGE_TAG_FRENCH, Res.string.language_french),
            LanguageChoice(Constants.LANGUAGE_TAG_BENGALI, Res.string.language_bengali),
            LanguageChoice(Constants.LANGUAGE_TAG_PORTUGUESE, Res.string.language_portuguese),
            LanguageChoice(Constants.LANGUAGE_TAG_TAMIL, Res.string.language_tamil),
            LanguageChoice(Constants.LANGUAGE_TAG_URDU, Res.string.language_urdu),
            LanguageChoice(Constants.LANGUAGE_TAG_INDONESIAN, Res.string.language_indonesian),
            LanguageChoice(Constants.LANGUAGE_TAG_JAPANESE, Res.string.language_japanese),
            LanguageChoice(Constants.LANGUAGE_TAG_RUSSIAN, Res.string.language_russian),
            LanguageChoice(Constants.LANGUAGE_TAG_GERMAN, Res.string.language_german),
            LanguageChoice(Constants.LANGUAGE_TAG_TURKISH, Res.string.language_turkish),
            LanguageChoice(Constants.LANGUAGE_TAG_VIETNAMESE, Res.string.language_vietnamese),
            LanguageChoice(Constants.LANGUAGE_TAG_THAI, Res.string.language_thai),
            LanguageChoice(Constants.LANGUAGE_TAG_PERSIAN, Res.string.language_persian),
            LanguageChoice(Constants.LANGUAGE_TAG_PUNJABI, Res.string.language_punjabi),
        )

    private val _selectedTheme =
        MutableStateFlow(prefManager.getThemeModeBlocking(Constants.MODE_DARK))
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _amoledDark = MutableStateFlow(prefManager.getAmoledDarkBlocking(false))
    val amoledDark: StateFlow<Boolean> = _amoledDark.asStateFlow()

    private val _accentColor =
        MutableStateFlow(prefManager.getAccentColorBlocking(Constants.ACCENT_DEFAULT))
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    private val _selectedLanguage =
        MutableStateFlow(
            prefManager.getLanguageTagBlocking(Constants.LANGUAGE_TAG_SYSTEM),
        )
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun updateTheme(mode: String) {
        if (_selectedTheme.value == mode) return
        _selectedTheme.value = mode
        runBlockingCompat {
            prefManager.setThemeMode(mode)
        }
    }

    fun updateAmoledDark(enabled: Boolean) {
        if (_amoledDark.value == enabled) return
        _amoledDark.value = enabled
        runBlockingCompat {
            prefManager.setAmoledDark(enabled)
        }
    }

    fun updateAccentColor(id: String) {
        if (_accentColor.value == id) return
        _accentColor.value = id
        viewModelScope.launch {
            prefManager.setAccentColor(id)
        }
    }

    fun updateLanguage(tag: String) {
        if (_selectedLanguage.value == tag) return
        _selectedLanguage.value = tag
        viewModelScope.launch {
            prefManager.setLanguageTag(tag)
        }
    }
}

data class ThemeChoice(
    val mode: String,
    val labelRes: StringResource,
)

data class LanguageChoice(
    val tag: String,
    val labelRes: StringResource,
)
