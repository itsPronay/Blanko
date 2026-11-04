package com.pronaycoding.blanko.core.domain.usecase.preset

import com.pronaycoding.blanko.core.data.repository.PresetRepository
import com.pronaycoding.blanko.core.database.entities.PresetEntity
import kotlinx.coroutines.flow.Flow

class GetPresetsUseCase(
    private val presetRepository: PresetRepository,
) {
    operator fun invoke(): Flow<List<PresetEntity>> = presetRepository.observePresets()
}
