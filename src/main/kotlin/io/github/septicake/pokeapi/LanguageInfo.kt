package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable

@Serializable
data class LanguageInfo(
    val id: Int,
    val iso3166: String,
    val iso639: String,
    val name: String,
    val official: Boolean,
    val names: List<LocalizedName>
)