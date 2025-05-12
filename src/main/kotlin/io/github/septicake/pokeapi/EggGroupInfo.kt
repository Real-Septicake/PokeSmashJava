package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable

@Serializable
data class EggGroupInfo(
    val id: Int,
    val name: String,
    val names: List<LocalizedName>
)
