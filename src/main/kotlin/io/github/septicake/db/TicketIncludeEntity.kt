package io.github.septicake.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object TicketIncludeTable : IntIdTable("ticketinclude") {
    val ticket = integer("ticket").index()
    val user = long("user")
    val muted = bool("muted").default(false)
}

class TicketIncludeEntity(id: EntityID<Int>) : IntEntity(id) {
    var ticket by TicketIncludeTable.ticket
    var user by TicketIncludeTable.user
    var muted by TicketIncludeTable.muted

    companion object : IntEntityClass<TicketIncludeEntity>(TicketIncludeTable)
}