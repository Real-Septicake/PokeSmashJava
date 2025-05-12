package io.github.septicake.pokeapi

import io.github.reactivecircus.cache4k.Cache
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

@Serializable
data class PokemonSpecies(
    val name: String,
    val url: String,
) {
    private val speciesCache = Cache.Builder<String, PokemonSpeciesInfo>().expireAfterWrite(7.days).build()

    suspend fun fetchInfo(): PokemonSpeciesInfo {
        return speciesCache.get(url) { PokeApi.request(url) }
    }
}
