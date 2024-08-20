package io.github.septicake.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object GuildTable : LongIdTable("serverinfo", columnName = "guildId") {
    val name = text("name")
    val polls = long("pollCount")
    val offset = long("offset")
    val smashes = long("smash")
    val passes = long("pass")
    val channel = long("channel").nullable()
}

class GuildEntity(id: EntityID<Long>) : LongEntity(id) {
    var name: String by GuildTable.name
    var polls: Long by GuildTable.polls
    var offset: Long by GuildTable.offset
    var smashes: Long by GuildTable.smashes
    var passes: Long by GuildTable.passes
    var channel: Long? by GuildTable.channel

    companion object : LongEntityClass<GuildEntity>(GuildTable)
}
