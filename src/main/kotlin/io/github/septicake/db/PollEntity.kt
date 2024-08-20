package io.github.septicake.db

import kotlinx.uuid.UUID
import kotlinx.uuid.exposed.KotlinxUUIDEntity
import kotlinx.uuid.exposed.KotlinxUUIDEntityClass
import kotlinx.uuid.exposed.KotlinxUUIDTable
import org.jetbrains.exposed.dao.id.EntityID

object PollTable : KotlinxUUIDTable("POLL_DATA") {
    val guild = reference("guild", GuildTable).index()
    val pokemon = reference("pokemon", PokemonTable).index()
    val smashes = long("smashes")
    val passes = long("passes")
    val result = enumeration<PollResult>("result").index()
}

class PollEntity(id: EntityID<UUID>) : KotlinxUUIDEntity(id) {
    val guild: GuildEntity by GuildEntity referencedOn PollTable.guild
    val pokemon: PokemonEntity by PokemonEntity referencedOn PollTable.pokemon
    var smashes: Long by GuildTable.smashes
    var passes: Long by GuildTable.passes
    val result: PollResult by PollTable.result

    companion object : KotlinxUUIDEntityClass<PollEntity>(PollTable)
}

enum class PollResult {
    PASSED,
    SMASHED
}
