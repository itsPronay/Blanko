package com.pronaycoding.blanko.core.domain.usecase.preset

import com.pronaycoding.blanko.core.data.repository.PresetRepository
import com.pronaycoding.blanko.core.database.entities.PresetEntity

class UpdatePresetUseCase(
    private val presetRepository: PresetRepository,
) {
    suspend operator fun invoke(entity: PresetEntity) = presetRepository.updatePreset(entity)
}
