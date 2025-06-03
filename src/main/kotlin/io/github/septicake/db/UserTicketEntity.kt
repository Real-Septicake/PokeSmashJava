package io.github.septicake.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object UserTicketTable : LongIdTable("userticket", "user") {
    val openTickets = integer("openTickets").default(0)
    val maxOpenTickets = integer("maxOpen").default(5)
    val totalTickets = integer("totalTickets").default(0)
}

class UserTicketEntity(id: EntityID<Long>) : LongEntity(id) {
    var openTickets by UserTicketTable.openTickets
    var maxOpenTickets by UserTicketTable.maxOpenTickets
    var totalTickets by UserTicketTable.totalTickets

    companion object : LongEntityClass<UserTicketEntity>(UserTicketTable)
}