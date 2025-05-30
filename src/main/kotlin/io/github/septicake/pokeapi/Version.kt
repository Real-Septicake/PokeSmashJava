package io.github.septicake.pokeapi

import io.github.reactivecircus.cache4k.Cache
import kotlinx.serialization.Serializable
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger
import kotlin.time.Duration.Companion.days

@Serializable
data class Version(
    val name: String,
    val url: String
) {
    val logger by getLogger()
    private val cache = Cache.Builder<String, VersionInfo>().expireAfterWrite(7.days).build()

    suspend fun fetchInfo(): VersionInfo {
        return cache.get(url) {
            logger.debug { "Version $name missed in cache" }
            PokeApi.request(url)
        }
    }
}
