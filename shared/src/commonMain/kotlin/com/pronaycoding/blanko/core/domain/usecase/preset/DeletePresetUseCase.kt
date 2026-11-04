package com.pronaycoding.blanko.core.domain.usecase.preset

import com.pronaycoding.blanko.core.data.repository.PresetRepository

class DeletePresetUseCase(
    private val presetRepository: PresetRepository,
) {
    suspend operator fun invoke(id: Long) = presetRepository.deletePreset(id)
}
