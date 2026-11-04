package com.pronaycoding.blanko.core.di

import com.pronaycoding.blanko.feature.home.HomeViewmodel
import com.pronaycoding.blanko.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::HomeViewmodel)
    viewModelOf(::SettingsViewModel)
}
