package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable

@Serializable
data class LocalizedName(
    val name: String,
    val language: Language,
)
