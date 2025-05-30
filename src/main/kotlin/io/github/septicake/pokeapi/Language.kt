package io.github.septicake.pokeapi

import io.github.reactivecircus.cache4k.Cache
import kotlinx.serialization.Serializable
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger
import kotlin.time.Duration.Companion.days

@Serializable
data class Language(
    val name: String,
    val url: String
) {
    val logger by getLogger()
    private val cache = Cache.Builder<String, LanguageInfo>().expireAfterWrite(7.days).build()

    suspend fun fetchInfo(): LanguageInfo {
        return cache.get(url) {
            logger.debug { "Language $name missed in cache" }
            PokeApi.request(url)
        }
    }
}
