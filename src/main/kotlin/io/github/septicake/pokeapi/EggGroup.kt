package io.github.septicake.pokeapi

import io.github.reactivecircus.cache4k.Cache
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

@Serializable
data class EggGroup(
    val name: String,
    val url: String
) {
    private val cache = Cache.Builder<String, EggGroupInfo>().expireAfterWrite(7.days).build()

    suspend fun fetchInfo(): EggGroupInfo {
        return cache.get(url) {
            PokeApi.request(url)
        }
    }
}
