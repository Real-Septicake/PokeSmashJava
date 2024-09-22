package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
data class PokemonInfo(
    val id: Int,
    val name: String,
    @SerialName("base_experience")
    val baseExperience: Int?,
    val height: Int,
    @SerialName("is_default")
    val default: Boolean,
    val order: Int,
    val weight: Int,
    val abilities: List<PokemonAbility.Reference>,
    @SerialName("past_abilities")
    val pastAbilities: JsonArray,
    val forms: List<PokemonForm>,
    @SerialName("game_indices")
    val gameIndices: JsonArray,
    @SerialName("held_items")
    val heldItems: JsonArray,
    @SerialName("location_area_encounters")
    val locationAreaEncounters: String,
    val moves: JsonArray,
    @SerialName("past_types")
    val pastTypes: List<PokemonTypePast>,
    val sprites: JsonObject,
    val cries: JsonObject,
    val species: JsonObject,
    val stats: JsonArray,
    val types: List<PokemonType.Reference>,
) {
    @Serializable
    data class PokemonTypePast(
        val generation: JsonObject,
        val types: List<PokemonType.Reference>,
    )
}
