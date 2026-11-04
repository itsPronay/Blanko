package com.pronaycoding.blanko.core.domain.usecase.customsound

import com.pronaycoding.blanko.core.data.repository.CustomSoundRepository

class UpdateCustomSoundDisplayNameUseCase(
    private val customSoundRepository: CustomSoundRepository,
) {
    suspend operator fun invoke(
        id: Int,
        displayName: String,
    ) = customSoundRepository.updateCustomSoundDisplayName(id, displayName)
}
