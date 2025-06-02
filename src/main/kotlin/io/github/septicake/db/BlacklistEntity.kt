package io.github.septicake.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object BlacklistTable : LongIdTable("blacklist", columnName = "user") {
    val reason = varchar("reason", 255).default("[None Given]")
    val since = timestamp("since").index().defaultExpression(CurrentTimestamp)
}

class BlacklistEntity(id: EntityID<Long>) : LongEntity(id) {
    var reason by BlacklistTable.reason
    var since by BlacklistTable.since

    companion object : LongEntityClass<BlacklistEntity>(BlacklistTable)
}