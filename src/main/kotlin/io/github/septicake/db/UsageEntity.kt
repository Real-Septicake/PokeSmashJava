package io.github.septicake.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsageTable : IntIdTable("usages") {
    val commandName = varchar("name", 30).index()
    val category = varchar("category", 20).index()
    val used = timestamp("used").defaultExpression(CurrentTimestamp)
    val clear = timestamp("clear").index()
}

class UsageEntity(id: EntityID<Int>) : IntEntity(id) {
    var commandName by UsageTable.commandName
    var category by UsageTable.category
    val used by UsageTable.used
    var clear by UsageTable.clear

    companion object : IntEntityClass<UsageEntity>(UsageTable)
}