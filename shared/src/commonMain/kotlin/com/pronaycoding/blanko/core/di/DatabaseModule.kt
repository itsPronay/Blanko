package com.pronaycoding.blanko.core.di

import com.pronaycoding.blanko.core.database.BlankoDatabase
import com.pronaycoding.blanko.core.database.dao.CustomSoundDao
import com.pronaycoding.blanko.core.database.dao.PresetDao
import org.koin.dsl.module

/**
 * DAOs derived from [BlankoDatabase]. The database itself is provided by the platform module
 * (it needs a platform-specific builder), so every app must register a `single<BlankoDatabase>`.
 */
val databaseModule = module {
    single<CustomSoundDao> { get<BlankoDatabase>().customSoundDao() }
    single<PresetDao> { get<BlankoDatabase>().presetDao() }
}
