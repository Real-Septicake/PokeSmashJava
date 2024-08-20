package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
data class PokemonAbility(
    val name: String,
    val url: String,
) {
    suspend fun fetchInfo(): PokemonAbilityInfo {
        return PokeApi.request(url)
    }

    @Serializable
    data class PokemonAbilityInfo(
        val id: Int,
        val name: String,
        @SerialName("is_main_series")
        val mainSeries: Boolean,
        val generation: JsonObject,
        val names: List<LocalizedName>,
        @SerialName("effect_entries")
        val effectEntries: JsonArray,
        @SerialName("effect_changes")
        val effectChanges: JsonArray,
        @SerialName("flavor_text_entries")
        val flavorTextEntries: JsonArray,
        val pokemon: List<Pokemon.HideableReference>,
    )

    @Serializable
    data class Reference(
        @SerialName("is_hidden")
        val hidden: Boolean,
        val slot: Int,
        val ability: PokemonAbility,
    )
}
