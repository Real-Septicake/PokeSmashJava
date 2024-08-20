package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LocalizedName(
    val name: String,
    val language: JsonObject,
)
