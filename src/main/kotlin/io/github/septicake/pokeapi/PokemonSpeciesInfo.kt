package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonSpeciesInfo(
    val id: Int,
    val name: String,
    val order: Int,
    val color: PokemonColorInfo,
    val names: List<LocalizedName> = listOf(),
    @SerialName("flavor_text_entries")
    val flavorTexts: List<FlavorText>,
) {
    @Serializable
    data class FlavorText(
        @SerialName("flavor_text")
        val flavorText: String,
        val language: Language,
        val version: Version
    ) {
        fun formatFlavorText(): String {
            return flavorText.replace('\u000c', '\n')
        }
    }
}
