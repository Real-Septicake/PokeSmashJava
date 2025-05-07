package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable

@Serializable
data class Language(
    val name: String,
    val url: String
) {
    suspend fun fetchInfo(): LanguageInfo {
        return PokeApi.request(url)
    }
}
