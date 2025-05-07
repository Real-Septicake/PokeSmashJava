package io.github.septicake.pokeapi

import kotlinx.serialization.Serializable

@Serializable
data class Version(
    val name: String,
    val url: String
) {
    suspend fun fetchInfo(): VersionInfo {
        return PokeApi.request(url)
    }
}
