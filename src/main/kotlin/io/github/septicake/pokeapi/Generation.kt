package io.github.septicake.pokeapi

import io.github.reactivecircus.cache4k.Cache
import kotlinx.serialization.Serializable
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger
import kotlin.time.Duration.Companion.days

@Serializable
data class Generation(
    val name: String,
    val url: String
) {
    val logger by getLogger()
    private val cache = Cache.Builder<String, GenerationInfo>().expireAfterWrite(7.days).build()

    suspend fun fetchInfo(): GenerationInfo {
        return cache.get(url) {
            logger.debug { "Generation $name missed in cache" }
            PokeApi.request(url)
        }
    }
}
