package io.github.septicake.pokeapi

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object PokeApi {
    val fuel = FuelManager().apply {
        basePath = "https://pokeapi.co/api/v2/"
    }

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend inline fun <reified T : Any> request(path: String, parameters: Parameters? = null): T {
        return fuel.get(path.replace(fuel.basePath!!, ""), parameters)
            .awaitObject(kotlinxDeserializerOf<T>(serializer<T>(), json))
    }

    suspend fun listPokemon(bulkQuery: Int = 20): Flow<Pokemon> = flow {
        var results: NamedApiResourceList<Pokemon>
        var offset = 0
        do {
            results = listPokemonPaged(offset = offset, limit = bulkQuery)
            offset += bulkQuery
            for (result in results.results)
                emit(result)

        } while (results.next != null)
    }

    suspend fun listPokemonPaged(offset: Int, limit: Int): NamedApiResourceList<Pokemon> {
        return request<NamedApiResourceList<Pokemon>>("/pokemon", listOf("offset" to offset, "limit" to limit))
    }

    suspend fun pokemon(name: String): PokemonInfo = request<PokemonInfo>("/pokemon/$name")

    suspend fun pokemon(id: Int): PokemonInfo = request<PokemonInfo>("/pokemon/$id")

    suspend fun pokemonColor(pokemonId: Int): PokemonColor = request("/pokemon-color/$pokemonId")

    suspend fun pokemonColor(pokemonName: String): PokemonColor = request("/pokemon-color/$pokemonName")
}
