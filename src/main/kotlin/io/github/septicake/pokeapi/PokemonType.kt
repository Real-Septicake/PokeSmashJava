package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
data class PokemonType(
    val name: String,
    val url: String,
) {
    suspend fun fetchInfo(): PokemonTypeInfo {
        return PokeApi.request(url)
    }

    @Serializable
    data class PokemonTypeInfo(
        val id: Int,
        val name: String,
        @SerialName("damage_relations")
        val damageRelations: JsonObject,
        @SerialName("past_damage_relations")
        val pastDamageRelations: JsonArray,
        @SerialName("game_indices")
        val gameIndices: JsonArray,
        val generation: JsonObject,
        @SerialName("move_damage_class")
        val moveDamageClass: JsonObject,
        val names: List<LocalizedName>,
        val pokemon: Pokemon.Reference,
        val moves: JsonArray,
    )

    @Serializable
    data class Reference(
        val slot: Int,
        val type: PokemonType,
    )
}
