package com.pronaycoding.blanko.core.domain.usecase.customsound

import com.pronaycoding.blanko.core.data.repository.CustomSoundRepository

class RemoveCustomSoundUseCase(
    private val customSoundRepository: CustomSoundRepository,
) {
    suspend operator fun invoke(id: Int) = customSoundRepository.removeCustomSound(id)
}
