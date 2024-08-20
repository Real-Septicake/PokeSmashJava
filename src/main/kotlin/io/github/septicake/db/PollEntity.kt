package io.github.septicake.db

import kotlinx.uuid.UUID
import kotlinx.uuid.exposed.KotlinxUUIDEntity
import kotlinx.uuid.exposed.KotlinxUUIDEntityClass
import kotlinx.uuid.exposed.KotlinxUUIDTable
import org.jetbrains.exposed.dao.id.EntityID

object PollTable : KotlinxUUIDTable("votes") {
    val guild = reference("guildId", GuildTable).index()
    val pokemon = reference("pokeId", PokemonTable).index()
    val smashes = long("smashes")
    val passes = long("passes")
    val result = enumeration<PollResult>("result").index()
}

class PollEntity(id: EntityID<UUID>) : KotlinxUUIDEntity(id) {
    val guild: GuildEntity by GuildEntity referencedOn PollTable.guild
    val pokemon: PokemonEntity by PokemonEntity referencedOn PollTable.pokemon
    var smashes: Long by PollTable.smashes
    var passes: Long by PollTable.passes
    val result: PollResult by PollTable.result

    companion object : KotlinxUUIDEntityClass<PollEntity>(PollTable)
}

enum class PollResult(val value: Int) {
    PASSED(0),
    SMASHED(1)
}
