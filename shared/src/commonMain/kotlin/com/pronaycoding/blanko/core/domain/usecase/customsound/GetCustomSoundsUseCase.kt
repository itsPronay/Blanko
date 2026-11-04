package com.pronaycoding.blanko.core.domain.usecase.customsound

import com.pronaycoding.blanko.core.data.repository.CustomSoundRepository
import com.pronaycoding.blanko.core.database.entities.CustomSoundEntity
import kotlinx.coroutines.flow.Flow

class GetCustomSoundsUseCase(
    private val customSoundRepository: CustomSoundRepository,
) {
    operator fun invoke(): Flow<List<CustomSoundEntity>> = customSoundRepository.getAllCustomSounds()
}
