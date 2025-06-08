package io.github.septicake.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object TicketTable : IntIdTable("tickets") {
    val author = long("author").index()
    val thread = long("threadId").uniqueIndex()
    val topic = varchar("topic", 75)
    val lastActive = timestamp("lastActive").index()
    val opened = timestamp("opened").defaultExpression(CurrentTimestamp)
}

class TicketEntity(id: EntityID<Int>) : IntEntity(id) {
    var author by TicketTable.author
    var thread by TicketTable.thread
    var topic by TicketTable.topic
    var lastActive by TicketTable.lastActive
    val opened by TicketTable.opened

    companion object : IntEntityClass<TicketEntity>(TicketTable)
}