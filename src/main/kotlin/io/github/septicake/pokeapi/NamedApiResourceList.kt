package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable

@Serializable
data class NamedApiResourceList<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>,
)
