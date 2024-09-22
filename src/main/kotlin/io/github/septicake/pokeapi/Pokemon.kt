package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    val name: String,
    val url: String,
) {
    @Serializable
    data class Reference(
        val slot: Int,
        val pokemon: Pokemon,
    )

    @Serializable
    data class HideableReference(
        @SerialName("is_hidden")
        val hidden: Boolean,
        val slot: Int,
        val pokemon: Pokemon,
    )

    suspend fun fetchInfo(): PokemonInfo {
        return PokeApi.request(url)
    }
}
