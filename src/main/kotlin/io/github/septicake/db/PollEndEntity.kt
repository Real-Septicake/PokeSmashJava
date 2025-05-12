package io.github.septicake.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object PollEndTable : LongIdTable("pollend", columnName = "message") {
    val server = long("server")
    val finish = timestamp("finish")
}

class PollEndEntity(id: EntityID<Long>) : LongEntity(id) {
    var server by PollEndTable.server
    var finish by PollEndTable.finish

    companion object : LongEntityClass<PollEndEntity>(PollEndTable)
}
