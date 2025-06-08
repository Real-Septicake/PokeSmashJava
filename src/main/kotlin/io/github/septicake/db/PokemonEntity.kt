package io.github.septicake.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.DenseRank
import org.jetbrains.exposed.sql.SortOrder

object PokemonTable : IntIdTable("pokemoninfo", columnName = "pokeId") {
    val smashWins = long("smashCount").default(0)
    val passWins = long("passCount").default(0)
    val smashes = long("smashVotes").default(0)
    val passes = long("passVotes").default(0)

    val rank = DenseRank().over().orderBy(
        smashWins to SortOrder.DESC,
        smashes to SortOrder.DESC,
        passes to SortOrder.ASC
    )
}

class PokemonEntity(id: EntityID<Int>) : IntEntity(id) {
    var smashWins: Long by PokemonTable.smashWins
    var passWins: Long by PokemonTable.passWins
    var smashes: Long by PokemonTable.smashes
    var passes: Long by PokemonTable.passes

    val rank: Long
        get() = readValues[PokemonTable.rank]

    companion object : IntEntityClass<PokemonEntity>(PokemonTable) {
        // Pokemon ID included to make things consistent
        // It is excluded from the actual ranking
        val rankOrdering = arrayOf(
            PokemonTable.smashWins to SortOrder.DESC,
            PokemonTable.smashes to SortOrder.DESC,
            PokemonTable.passes to SortOrder.ASC,
            PokemonTable.id to SortOrder.ASC
        )

        val reverseRankOrdering = arrayOf(
            PokemonTable.smashWins to SortOrder.ASC,
            PokemonTable.smashes to SortOrder.ASC,
            PokemonTable.passes to SortOrder.DESC,
            PokemonTable.id to SortOrder.DESC
        )
    }
}
