package com.pronaycoding.blanko.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronaycoding.blanko.resources.*
import com.pronaycoding.blanko.core.common.PresetJson
import com.pronaycoding.blanko.core.common.platform.AppLog
import com.pronaycoding.blanko.core.common.platform.CustomSoundFileStore
import com.pronaycoding.blanko.core.common.platform.PlaybackNotifier
import com.pronaycoding.blanko.core.common.util.SnackbarController
import com.pronaycoding.blanko.core.database.entities.CustomSoundEntity
import com.pronaycoding.blanko.core.database.entities.PresetEntity
import com.pronaycoding.blanko.core.domain.usecase.customsound.AddCustomSoundUseCase
import com.pronaycoding.blanko.core.domain.usecase.customsound.GetCustomSoundsUseCase
import com.pronaycoding.blanko.core.domain.usecase.customsound.RemoveCustomSoundUseCase
import com.pronaycoding.blanko.core.domain.usecase.customsound.UpdateCustomSoundDisplayNameUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.DeletePresetUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.GetPresetsUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.SavePresetUseCase
import com.pronaycoding.blanko.core.domain.usecase.preset.UpdatePresetUseCase
import com.pronaycoding.blanko.core.service.playback.GlobalPlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.time.TimeSource

class HomeViewmodel(
    private val soundManager: SoundManager,
    private val getCustomSoundsUseCase: GetCustomSoundsUseCase,
    private val addCustomSoundUseCase: AddCustomSoundUseCase,
    private val removeCustomSoundUseCase: RemoveCustomSoundUseCase,
    private val updateCustomSoundDisplayNameUseCase: UpdateCustomSoundDisplayNameUseCase,
    private val getPresetsUseCase: GetPresetsUseCase,
    private val savePresetUseCase: SavePresetUseCase,
    private val deletePresetUseCase: DeletePresetUseCase,
    private val updatePresetUseCase: UpdatePresetUseCase,
    private val globalPlaybackState: GlobalPlaybackState,
    private val mediaPlaybackNotifications: PlaybackNotifier,
    private val customSoundFileStore: CustomSoundFileStore,
    private val snackbarController: SnackbarController,
) : ViewModel() {
    val canPlay: StateFlow<Boolean> = globalPlaybackState.canPlay

    private val _customSounds: MutableStateFlow<List<CustomSoundEntity>> = MutableStateFlow(emptyList())
    val customSounds: StateFlow<List<CustomSoundEntity>> = _customSounds.asStateFlow()

    private val _builtinVolumes: MutableStateFlow<Map<Int, Float>> = MutableStateFlow(emptyMap())
    val builtinVolumes: StateFlow<Map<Int, Float>> = _builtinVolumes.asStateFlow()

    private val _customVolumes: MutableStateFlow<Map<Int, Float>> = MutableStateFlow(emptyMap())
    val customVolumes: StateFlow<Map<Int, Float>> = _customVolumes.asStateFlow()

    val presets: StateFlow<List<PresetEntity>> =
        getPresetsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val loadedCustomSoundIds: MutableSet<Int> = mutableSetOf()
    private var customSoundsCollectionJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _sleepTimerRemainingMillis: MutableStateFlow<Long?> = MutableStateFlow(null)
    val sleepTimerRemainingMillis: StateFlow<Long?> = _sleepTimerRemainingMillis.asStateFlow()

    init {
        startCustomSoundsCollection()
        restorePlaybackState()
    }

    /**
     * SoundManager and GlobalPlaybackState outlive this ViewModel (and the window content), so sounds can
     * still be playing when it is recreated. Rebuild the UI state from them instead of stopping everything.
     */
    private fun restorePlaybackState() {
        _builtinVolumes.value = soundManager.activeBuiltInVolumes()
        _customVolumes.value = soundManager.activeCustomVolumes()
        syncPlaybackNotification()
    }

    private fun builtInPresetIndexEndExclusive(): Int = getCardList().size

    private fun startCustomSoundsCollection() {
        if (customSoundsCollectionJob?.isActive == true) return
        customSoundsCollectionJob =
            viewModelScope.launch {
                getCustomSoundsUseCase().collect { sounds ->
                    _customSounds.value = sounds

                    sounds.forEach { sound ->
                        if (!loadedCustomSoundIds.contains(sound.id)) {
                            // Copying/checking the file is disk I/O; the store moves it off the main thread.
                            val persistentPath = customSoundFileStore.persist(sound.id, sound.filePath)
                            AppLog.d("HomeViewmodel", "Loading custom sound from: $persistentPath")
                            soundManager.loadCustomSound(sound.id, persistentPath)
                            loadedCustomSoundIds.add(sound.id)
                            AppLog.d("HomeViewmodel", "Loaded new custom sound: ${sound.displayName}")
                        }
                    }

                    val soundIds = sounds.map { it.id }.toSet()
                    loadedCustomSoundIds.forEach { loadedId ->
                        if (!soundIds.contains(loadedId)) {
                            soundManager.unloadCustomSound(loadedId)
                            AppLog.d("HomeViewmodel", "Unloaded deleted custom sound: $loadedId")
                        }
                    }
                    loadedCustomSoundIds.retainAll(soundIds)
                    _customVolumes.update { current ->
                        current.filterKeys { it in soundIds }
                    }
                    syncPlaybackNotification()
                }
            }
    }

    private fun stopCustomSoundsCollection() {
        customSoundsCollectionJob?.cancel()
        customSoundsCollectionJob = null
        loadedCustomSoundIds.toList().forEach { id ->
            soundManager.unloadCustomSound(id)
        }
        loadedCustomSoundIds.clear()
        _customSounds.value = emptyList()
        _customVolumes.value = emptyMap()
        syncPlaybackNotification()
    }

    private fun hasAudibleMix(): Boolean {
        val end = builtInPresetIndexEndExclusive()
        val builtIn = _builtinVolumes.value.any { (i, v) -> i in 0 until end && v > 0f }
        val custom = _customVolumes.value.any { (_, v) -> v > 0f }
        return builtIn || custom
    }

    private fun syncPlaybackNotification() {
        globalPlaybackState.lastHasAudibleMix = hasAudibleMix()
        mediaPlaybackNotifications.requestSync(
            globalPlaybackState.canPlay.value,
            globalPlaybackState.lastHasAudibleMix,
        )
    }

    fun handlePlayPause(canPlay: Boolean) {
        globalPlaybackState.setCanPlay(canPlay)
        syncPlaybackNotification()
    }

    fun resetAllSounds() {
        soundManager.stopAllSounds()
        _builtinVolumes.value = emptyMap()
        _customVolumes.value = emptyMap()
        globalPlaybackState.setCanPlay(true)
        syncPlaybackNotification()
    }

    fun setBuiltinVolume(
        index: Int,
        volume: Float,
    ) {
        _builtinVolumes.update { current ->
            current.toMutableMap().apply { put(index, volume) }
        }
        if (!globalPlaybackState.canPlay.value) return
        if (volume <= 0f) {
            soundManager.stopSound(index)
        } else {
            if (soundManager.isBuiltInPlaying(index)) {
                soundManager.controlSound(index, volume)
            } else {
                soundManager.playSound(index, volume)
            }
        }
        syncPlaybackNotification()
    }

    fun previewBuiltinVolume(
        index: Int,
        volume: Float,
    ) {
        if (!globalPlaybackState.canPlay.value) return
        if (volume <= 0f) {
            soundManager.stopSound(index)
        } else {
            if (soundManager.isBuiltInPlaying(index)) {
                soundManager.controlSound(index, volume)
            } else {
                soundManager.playSound(index, volume)
            }
        }
    }

    fun onBuiltInCardClick(index: Int) {
        val isCurrentlyActive = (_builtinVolumes.value[index] ?: 0f) > 0f
        if (!globalPlaybackState.canPlay.value) {
            globalPlaybackState.setCanPlay(true)
            if (!isCurrentlyActive) {
                setBuiltinVolume(index, 1f)
            }
            return
        }
        val targetVolume = if (isCurrentlyActive) 0f else 1f
        setBuiltinVolume(index, targetVolume)
    }

    fun setCustomSoundVolume(
        soundId: Int,
        volume: Float,
    ) {
        _customVolumes.update { current ->
            current.toMutableMap().apply { put(soundId, volume) }
        }
        if (!globalPlaybackState.canPlay.value) return
        if (volume <= 0f) {
            soundManager.stopCustomSound(soundId)
        } else {
            if (soundManager.isCustomSoundPlaying(soundId)) {
                soundManager.controlCustomSound(soundId, volume)
            } else {
                soundManager.playCustomSound(soundId, volume)
            }
        }
        syncPlaybackNotification()
    }

    fun previewCustomSoundVolume(
        soundId: Int,
        volume: Float,
    ) {
        if (!globalPlaybackState.canPlay.value) return
        if (volume <= 0f) {
            soundManager.stopCustomSound(soundId)
        } else {
            if (soundManager.isCustomSoundPlaying(soundId)) {
                soundManager.controlCustomSound(soundId, volume)
            } else {
                soundManager.playCustomSound(soundId, volume)
            }
        }
    }

    fun onCustomCardClick(soundId: Int) {
        val isCurrentlyActive = (_customVolumes.value[soundId] ?: 0f) > 0f
        if (!globalPlaybackState.canPlay.value) {
            globalPlaybackState.setCanPlay(true)
            if (!isCurrentlyActive) {
                setCustomSoundVolume(soundId, 1f)
            }
            return
        }
        val targetVolume = if (isCurrentlyActive) 0f else 1f
        setCustomSoundVolume(soundId, targetVolume)
    }

    fun stopSound(int: Int) {
        soundManager.stopSound(int)
    }

    fun stopCustomSound(soundId: Int) {
        soundManager.stopCustomSound(soundId)
    }

    fun playSound(
        int: Int,
        volume: Float,
    ) {
        if (globalPlaybackState.canPlay.value) {
            soundManager.playSound(int, volume)
        }
    }

    fun playCustomSound(
        soundId: Int,
        volume: Float,
    ) {
        if (globalPlaybackState.canPlay.value) {
            soundManager.playCustomSound(soundId, volume)
        }
    }

    fun addCustomSound(
        displayName: String,
        filePath: String,
    ) {
        viewModelScope.launch {
            addCustomSoundUseCase(displayName, filePath)
        }
    }

    fun removeCustomSound(soundId: Int) {
        viewModelScope.launch {
            soundManager.unloadCustomSound(soundId)
            removeCustomSoundUseCase(soundId)
            _customVolumes.update { it - soundId }
            syncPlaybackNotification()
        }
    }

    fun renameCustomSound(
        soundId: Int,
        newDisplayName: String,
    ) {
        val sanitizedName = newDisplayName.trim()
        if (sanitizedName.isEmpty()) return
        viewModelScope.launch {
            updateCustomSoundDisplayNameUseCase(soundId, sanitizedName)
        }
    }

    fun snapshotForNewPreset(): Pair<Map<Int, Float>, Map<Int, Float>> {
        val end = builtInPresetIndexEndExclusive()
        val builtIn =
            _builtinVolumes.value.filter { (idx, vol) ->
                idx in 0 until end && vol > 0f
            }
        val custom = _customVolumes.value.filter { (_, vol) -> vol > 0f }
        return builtIn to custom
    }

    fun savePreset(name: String) {
        val (builtIn, custom) = snapshotForNewPreset()
        if (builtIn.isEmpty() && custom.isEmpty()) return
        viewModelScope.launch {
            savePresetUseCase(
                PresetEntity(
                    name = name.trim().ifEmpty { getString(Res.string.preset_untitled) },
                    builtInVolumesJson = PresetJson.mapToJson(builtIn),
                    customVolumesJson = PresetJson.mapToJson(custom),
                ),
            )
            snackbarController.show(Res.string.preset_saved)
        }
    }

    fun applyPreset(preset: PresetEntity) {
        val endExclusive = builtInPresetIndexEndExclusive()
        val builtIn =
            PresetJson
                .jsonToMap(preset.builtInVolumesJson)
                .filterKeys { it in 0 until endExclusive }
        val custom = PresetJson.jsonToMap(preset.customVolumesJson)

        soundManager.stopAllSounds()

        _builtinVolumes.value = builtIn
        _customVolumes.value = custom
        globalPlaybackState.setCanPlay(true)

        builtIn.forEach { (index, volume) ->
            if (volume > 0f) {
                soundManager.playSound(index, volume)
            }
        }
        custom.forEach { (soundId, volume) ->
            if (volume > 0f && loadedCustomSoundIds.contains(soundId)) {
                soundManager.playCustomSound(soundId, volume)
            }
        }
        syncPlaybackNotification()
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            deletePresetUseCase(id)
        }
    }

    fun renamePreset(
        preset: PresetEntity,
        newName: String,
    ) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            updatePresetUseCase(preset.copy(name = trimmed))
        }
    }

    fun startSleepTimer(durationMillis: Long) {
        if (durationMillis <= 0L) return
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMillis.value = durationMillis
        sleepTimerJob =
            viewModelScope.launch {
                val startMark = TimeSource.Monotonic.markNow()
                while (true) {
                    val remaining = (durationMillis - startMark.elapsedNow().inWholeMilliseconds).coerceAtLeast(0L)
                    _sleepTimerRemainingMillis.value = remaining
                    if (remaining <= 0L) break
                    delay(1000L)
                }
                _sleepTimerRemainingMillis.value = null
                resetAllSounds()
                snackbarController.show(Res.string.sleep_timer_finished)
            }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMillis.value = null
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        customSoundsCollectionJob?.cancel()
        super.onCleared()
    }
}
