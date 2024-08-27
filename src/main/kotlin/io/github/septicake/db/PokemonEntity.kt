package io.github.septicake.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object PokemonTable : IntIdTable("pokemoninfo", columnName = "pokeId") {
    val smashWins = long("smashCount")
    val passWins = long("passCount")
    val smashes = long("smashVotes")
    val passes = long("passVotes")
}

class PokemonEntity(id: EntityID<Int>) : IntEntity(id) {
    var smashWins: Long by PokemonTable.smashWins
    var passWins: Long by PokemonTable.passWins
    var smashes: Long by PokemonTable.smashes
    var passes: Long by PokemonTable.passes

    companion object : IntEntityClass<PokemonEntity>(PokemonTable)
}
