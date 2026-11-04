package com.pronaycoding.blanko.desktop

import com.pronaycoding.blanko.core.common.platform.CustomSoundFileStore
import com.pronaycoding.blanko.core.common.platform.ExternalActions
import com.pronaycoding.blanko.core.common.platform.PlaybackNotifier
import com.pronaycoding.blanko.core.common.platform.SoundPlayerFactory
import com.pronaycoding.blanko.core.database.BlankoDatabase
import com.pronaycoding.blanko.core.database.createBlankoDatabase
import com.pronaycoding.blanko.core.datastore.createPreferencesDataStore
import com.pronaycoding.blanko.desktopaudio.AudioMixer
import org.koin.dsl.module
import java.io.File

/** Desktop implementations of everything the shared module needs from the platform. */
fun desktopPlatformModule(dataDirectory: File) =
    module {
        single<BlankoDatabase> { createBlankoDatabase(File(dataDirectory, "database")) }
        single { createPreferencesDataStore(File(dataDirectory, "preferences")) }
        single { AudioMixer() }
        single<SoundPlayerFactory> { MixerSoundPlayerFactory(get()) }
        single<PlaybackNotifier> { NoOpPlaybackNotifier }
        single<CustomSoundFileStore> { DesktopCustomSoundFileStore(File(dataDirectory, "custom_sounds")) }
        single<ExternalActions> { DesktopExternalActions() }
    }
