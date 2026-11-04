package com.pronaycoding.blanko.core.domain.usecase.customsound

import com.pronaycoding.blanko.core.data.repository.CustomSoundRepository

class AddCustomSoundUseCase(
    private val customSoundRepository: CustomSoundRepository,
) {
    suspend operator fun invoke(
        displayName: String,
        filePath: String,
    ) = customSoundRepository.addCustomSound(displayName, filePath)
}
