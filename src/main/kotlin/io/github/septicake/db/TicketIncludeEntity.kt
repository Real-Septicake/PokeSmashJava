package io.github.septicake.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object TicketIncludeTable : IntIdTable("ticketinclude") {
    val ticket = integer("ticket").index()
    val user = long("user")
}

class TicketIncludeEntity(id: EntityID<Int>) : IntEntity(id) {
    var user by TicketIncludeTable.user

    companion object : IntEntityClass<TicketIncludeEntity>(TicketIncludeTable)
}