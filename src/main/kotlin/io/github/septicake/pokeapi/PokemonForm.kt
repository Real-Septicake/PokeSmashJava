package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PokemonForm(
    val name: String,
    val url: String,
) {
    suspend fun fetchInfo(): PokemonFormInfo {
        return PokeApi.request(url)
    }

    @Serializable
    data class PokemonFormInfo(
        val id: Int,
        val name: String,
        val order: Int,
        @SerialName("form_order")
        val formOrder: Int,
        @SerialName("is_default")
        val default: Boolean,
        @SerialName("is_battle_only")
        val battleOnly: Boolean,
        @SerialName("is_mega")
        val mega: Boolean,
        @SerialName("form_name")
        val formName: String,
        val pokemon: NamedApiResourceList<Pokemon>,
        val types: List<PokemonType.Reference>,
        val sprites: JsonObject,
        @SerialName("version_group")
        val versionGroup: JsonObject,
        val names: List<LocalizedName>,
        @SerialName("form_names")
        val formNames: List<LocalizedName>,
    )
}
