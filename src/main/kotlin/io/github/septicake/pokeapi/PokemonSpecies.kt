package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable

@Serializable
data class PokemonSpecies(
    val name: String,
    val url: String,
) {
    suspend fun fetchInfo(): PokemonSpeciesInfo {
        return PokeApi.request(url)
    }
}
