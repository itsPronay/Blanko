package com.pronaycoding.blanko.core.di

import com.pronaycoding.blanko.core.service.playback.GlobalPlaybackState
import com.pronaycoding.blanko.feature.home.SoundManager
import org.koin.dsl.module

/**
 * Playback services. The platform module must provide a `SoundPlayerFactory`, a `PlaybackNotifier`,
 * a `CustomSoundFileStore`, `ExternalActions` and the preferences `DataStore`.
 */
val serviceModule = module {
    single { SoundManager(get()) }
    single {
        GlobalPlaybackState(get()).apply {
            GlobalPlaybackState.setInstance(this)
        }
    }
}
