package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GenerationInfo(
    val abilities: List<JsonObject>,
    val id: Int,
    @SerialName("main_region")
    val mainRegion: JsonObject,
    val moves: List<JsonObject>,
    val name: String,
    val names: List<LocalizedName>,
    @SerialName("pokemon_species")
    val pokemonSpecies: List<PokemonSpecies>,
    val types: List<PokemonType>,
    @SerialName("version_groups")
    val versionGroups: List<JsonObject>
)
