package io.github.septicake.db

import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID

object WhitelistTable : CompositeIdTable("whitelist") {
    val user = long("userId").index()
    val guild = long("guildId").index()
}

class WhitelistEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    var user: Long by WhitelistTable.user
    var guild: Long by WhitelistTable.guild

    companion object : CompositeEntityClass<WhitelistEntity>(WhitelistTable)
}