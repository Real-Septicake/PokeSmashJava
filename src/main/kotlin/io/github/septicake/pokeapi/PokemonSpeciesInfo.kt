package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
data class PokemonSpeciesInfo(
    val id: Int,
    val name: String,
    val order: Int,
    @SerialName("form_order")
    val formOrder: Int,
    @SerialName("is_default")
    val default: Boolean,
    @SerialName("is_battle_only")
    val battleOnly: Boolean,
    @SerialName("is_mega")
    val mega: Boolean,
    @SerialName("form_name")
    val formName: String,
    val pokemon: Pokemon,
    val types: JsonArray,
    val sprites: JsonObject,
    val versionGroup: JsonObject,
    val names: List<LocalizedName> = listOf(),
    val formNames: List<LocalizedName> = listOf(),
)
