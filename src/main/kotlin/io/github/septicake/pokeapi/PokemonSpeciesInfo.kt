package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonSpeciesInfo(
    val id: Int,
    val name: String,
    val order: Int,
    val color: PokemonColorInfo,
    @SerialName("egg_groups")
    val eggGroups: List<EggGroup>,
    val names: List<LocalizedName> = listOf(),
    @SerialName("flavor_text_entries")
    val flavorTexts: List<FlavorText>,
    val genera: List<Genus>,
    val generation: Generation,
    @SerialName("gender_rate")
    val genderRate: Int,
    val varieties: List<Variety>,
) {
    @Serializable
    data class FlavorText(
        @SerialName("flavor_text")
        val flavorText: String,
        val language: Language,
        val version: Version
    ) {
        fun formatFlavorText(): String {
            return flavorText.replace("\u000c", "\n")
        }
    }

    @Serializable
    data class Variety(
        @SerialName("is_default")
        val isDefault: Boolean,
        val pokemon: Pokemon
    )

    @Serializable
    data class Genus(
        val genus: String,
        val language: Language
    )
}
