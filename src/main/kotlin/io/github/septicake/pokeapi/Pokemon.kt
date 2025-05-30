package io.github.septicake.pokeapi

import io.github.reactivecircus.cache4k.Cache
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger
import kotlin.time.Duration.Companion.days

@Serializable
data class Pokemon(
    val name: String,
    val url: String,
) {
    val logger by getLogger()
    private val cache = Cache.Builder<String, PokemonInfo>().expireAfterWrite(7.days).build()

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
        return cache.get(url) {
            logger.debug { "Pokemon $name missed in cache" }
            PokeApi.request(url)
        }
    }
}
