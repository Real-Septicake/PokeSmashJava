package io.github.septicake.pokeapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonColor(
    val id: Int,
    val name: String,
    val names: List<LocalizedName>,
    @SerialName("pokemon_species")
    val species: List<PokemonSpecies>,
) {
    fun colorFromName(): Int? = colorsByName[name]

    companion object {
        // colors from https://bulbapedia.bulbagarden.net/wiki/List_of_Pok%C3%A9mon_by_color#Changes
        val colorsByName = mapOf(
            "red" to 0xE60033,
            "blue" to 0x0095D9,
            "yellow" to 0xFFD900,
            "green" to 0x3EB370,
            "black" to 0x2B2B2B,
            "brown" to 0x965042,
            "purple" to 0x884898,
            "gray" to 0x7D7D7D,
            "white" to 0xFFFFFF,
            "pink" to 0xE38698,
        )
    }
}
