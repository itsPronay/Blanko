package com.pronaycoding.blanko.core.di

import com.pronaycoding.blanko.core.domain.usecase.customsound.AddCustomSoundUseCase
import com.pronaycoding.blanko.core.domain.usecase.customsound.GetCustomSoundsUseCase
import com.pronaycoding.blanko.core.domain.usecase.customsound.RemoveCustomSoundUseCase
import com.pronaycoding.blanko.core.domain.usecase.customsound.UpdateCustomSoundDisplayNameUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.DeletePresetUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.GetPresetsUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.SavePresetUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.UpdatePresetUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::GetPresetsUseCase)
    factoryOf(::SavePresetUseCase)
    factoryOf(::DeletePresetUseCase)
    factoryOf(::UpdatePresetUseCase)
    factoryOf(::GetCustomSoundsUseCase)
    factoryOf(::AddCustomSoundUseCase)
    factoryOf(::RemoveCustomSoundUseCase)
    factoryOf(::UpdateCustomSoundDisplayNameUseCase)
}
