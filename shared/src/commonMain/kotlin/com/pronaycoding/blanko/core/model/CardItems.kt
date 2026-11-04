package com.pronaycoding.blanko.core.model

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.pronaycoding.blanko.resources.*

/**
 * Sealed class representing available sounds in the Blanko app.
 *
 * This sealed class defines all built-in ambient sounds and provides a framework for custom user-uploaded sounds.
 * Sounds are organized into categories (Nature, Travel, Interiors, Noise, Music, Custom) and each includes:
 * - A localized title (from string resources)
 * - An icon for UI display
 * - An audio resource file (for built-in sounds)
 * - Category information for UI grouping
 *
 * Built-in sounds are defined as data objects (singletons), while custom user sounds are represented
 * as data class instances of [CustomCardItem].
 *
 * @property titleRes String resource for the sound's localized name
 * @property icon Drawable resource for the sound's icon
 * @property audioFile Bundled audio file name under the `sounds/` asset folder (only for built-in sounds)
 * @property type Category name (e.g., "Nature", "Travel", "Custom") for grouping
 * @property firstInType Boolean indicating if this is the first sound in its category (used for UI section headers)
 * @property customSoundId Unique ID for custom sounds (null for built-in sounds)
 * @property filePath File path or URI to audio file (only for custom sounds)
 *
 * @see CustomCardItem for custom user-uploaded sound representation
 * @see SoundManager for audio playback
 */
sealed class CardItems(
    val titleRes: StringResource,
    var icon: DrawableResource,
    val audioFile: String,
    val type: String = "",
    val firstInType: Boolean = false,
    val customSoundId: Int? = null,
    val filePath: String? = null,
) {
    /**
     * Returns the localized title string for this sound.
     *
     * For custom sounds, returns the user-provided display name.
     * For built-in sounds, loads the localized string from resources.
     *
     * @return The localized title string
     */
    @Composable
    fun localizedTitle(): String =
        when (this) {
            is CustomCardItem -> displayName
            else -> stringResource(titleRes)
        }

    /**
     * Rain sound - Nature category.
     * Gentle rainfall sounds for a calm, natural atmosphere.
     */
    data object Rain : CardItems(
        titleRes = Res.string.sound_rain,
        icon = Res.drawable.rain,
        audioFile = "rain",
        type = "Nature",
        firstInType = true,
    )

    /**
     * Summer Night sound - Nature category.
     * Evening outdoor ambience with summer insects and breeze.
     */
    data object SummerNight : CardItems(
        titleRes = Res.string.sound_summer_night,
        icon = Res.drawable.moon,
        audioFile = "summer_night",
        type = "Nature",
    )

    /**
     * Wind sound - Nature category.
     * Gentle wind and rustling leaves for outdoor ambience.
     */
    data object Wind : CardItems(
        titleRes = Res.string.sound_wind,
        icon = Res.drawable.wind,
        audioFile = "wind",
        type = "Nature",
    )

    /**
     * Ocean Wave sound - Nature category.
     * Soothing waves crashing on shore.
     */
    data object Wave : CardItems(
        titleRes = Res.string.sound_wave,
        icon = Res.drawable.wave,
        audioFile = "waves",
        type = "Nature",
    )

    /**
     * Stream sound - Nature category.
     * Gentle flowing water and babbling brook.
     */
    data object Stream : CardItems(
        titleRes = Res.string.sound_stream,
        icon = Res.drawable.stream,
        audioFile = "stream",
        type = "Nature",
    )

    /**
     * Thunder Storm sound - Nature category.
     * Thunderstorm with rain and distant lightning.
     */
    data object Storm : CardItems(
        titleRes = Res.string.sound_storm,
        icon = Res.drawable.storm,
        audioFile = "storm",
        type = "Nature",
    )

    /**
     * Bird Chirping sound - Nature category.
     * Morning birds and forest ambience.
     */
    data object Birds : CardItems(
        titleRes = Res.string.sound_birds,
        icon = Res.drawable.birds,
        audioFile = "birds",
        type = "Nature",
    )

    /**
     * Train sound - Travel category.
     * Moving train with rhythmic wheel sounds.
     */
    data object Train : CardItems(
        titleRes = Res.string.sound_train,
        icon = Res.drawable.train,
        audioFile = "train",
        type = "Travel",
        firstInType = true,
    )

    /**
     * Boat/Sailing sound - Travel category.
     * Boat engine and water ambience.
     */
    data object Boat : CardItems(
        titleRes = Res.string.sound_boat,
        icon = Res.drawable.sailboat,
        audioFile = "boat",
        type = "Travel",
    )

    /**
     * City Ambience sound - Travel category.
     * Urban background with traffic and city sounds.
     */
    data object City : CardItems(
        titleRes = Res.string.sound_city,
        icon = Res.drawable.city,
        audioFile = "city",
        type = "Travel",
    )

    /**
     * Coffee Shop sound - Interiors category.
     * Cozy coffee shop ambience with background chatter.
     */
    data object CoffeeShop : CardItems(
        titleRes = Res.string.sound_coffee_shop,
        icon = Res.drawable.coffee,
        audioFile = "coffee_shop",
        type = "Interiors",
        firstInType = true,
    )

    /**
     * Fireplace sound - Interiors category.
     * Crackling fireplace for warmth and comfort.
     */
    data object FirePlace : CardItems(
        titleRes = Res.string.sound_fireplace,
        icon = Res.drawable.fireplace,
        audioFile = "fireplace",
        type = "Interiors",
    )

    /**
     * Busy Restaurant sound - Interiors category.
     * Lively restaurant ambience with background noise.
     */
    data object BusyRestaurant : CardItems(
        titleRes = Res.string.sound_busy_restaurant,
        icon = Res.drawable.food_delivery,
        audioFile = "indoor_busy_restaurant",
        type = "Interiors",
    )

    /**
     * Pink Noise sound - Noise category.
     * Pink noise (deeper than white noise) for masking and focus.
     */
    data object PinkNoise : CardItems(
        titleRes = Res.string.sound_pink_noise,
        icon = Res.drawable.pink_noise,
        audioFile = "pink_noise",
        type = "Noise",
        firstInType = true,
    )

    /**
     * White Noise sound - Noise category.
     * Classic white noise for sleep and masking ambient sounds.
     */
    data object WhiteNoise : CardItems(
        titleRes = Res.string.sound_white_noise,
        icon = Res.drawable.white_noise,
        audioFile = "white_noise",
        type = "Noise",
    )

    /**
     * Custom sounds category marker.
     * Not a real sound, used as a placeholder for the Custom category.
     */
    data object Custom : CardItems(
        titleRes = Res.string.empty_string,
        icon = Res.drawable.city,
        audioFile = "city",
        type = "Custom",
    )

    /**
     * Represents a custom user-uploaded sound file.
     *
     * Custom sounds are audio files imported by users, extending the built-in sound library.
     * Each custom sound is tracked by a unique ID and its file path for persistent playback.
     *
     * @property id Unique database ID for this custom sound
     * @property displayName User-provided name for the custom sound
     * @property soundFilePath File path or content URI to the audio file
     *
     * @see CustomSoundRepository for custom sound management
     */
    data class CustomCardItem(
        val id: Int,
        val displayName: String,
        val soundFilePath: String,
    ) : CardItems(
            titleRes = Res.string.empty_string,
            icon = Res.drawable.white_noise,
            audioFile = "white_noise",
            type = "Custom",
            customSoundId = id,
            filePath = soundFilePath,
        )
}
