package io.github.septicake.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object GuildTable : LongIdTable("serverinfo", columnName = "guildId") {
    val name = text("name")
    val polls = integer("pollCount").default(0)
    val offset = integer("`offset`").default(0)
    val smashes = long("smash").default(0)
    val passes = long("pass").default(0)
    val channel = long("channel").nullable().default(null)
}

class GuildEntity(id: EntityID<Long>) : LongEntity(id) {
    var name: String by GuildTable.name
    var polls: Int by GuildTable.polls
    var offset: Int by GuildTable.offset
    var smashes: Long by GuildTable.smashes
    var passes: Long by GuildTable.passes
    var channel: Long? by GuildTable.channel

    companion object : LongEntityClass<GuildEntity>(GuildTable)
}
