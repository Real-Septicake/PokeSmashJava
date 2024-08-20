package io.github.septicake.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object PokemonTable : LongIdTable("MEMBER_DATA") {
    val smashWins = long("smash_wins")
    val passWins = long("pass_wins")
    val smashes = long("smashes")
    val passes = long("passes")
}

class PokemonEntity(id: EntityID<Long>) : LongEntity(id) {
    var smashWins: Long by PokemonTable.smashWins
    var passWins: Long by PokemonTable.passWins
    var smashes: Long by PokemonTable.smashes
    var passes: Long by PokemonTable.passes

    companion object : LongEntityClass<PokemonEntity>(PokemonTable)
}
