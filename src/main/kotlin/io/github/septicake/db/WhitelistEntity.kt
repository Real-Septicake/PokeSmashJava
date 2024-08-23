package io.github.septicake.db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object WhitelistTable : IntIdTable("whitelist") {
    val user = long("userId").index()
    val guild = long("guildId").index()
}

class WhitelistEntity(id: EntityID<Int>) : IntEntity(id) {
    var user: Long by WhitelistTable.user
    var guild: Long by WhitelistTable.guild

    companion object : IntEntityClass<WhitelistEntity>(WhitelistTable)
}