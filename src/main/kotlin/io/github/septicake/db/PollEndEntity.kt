package io.github.septicake.db

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object PollEndTable : LongIdTable("pollend", columnName = "message") {
    val server = long("server")
    val finish = datetime("finish")
}

class PollEndEntity(id: EntityID<Long>) : LongEntity(id) {
    var server: Long by PollEndTable.server
    var finish: LocalDateTime by PollEndTable.finish

    companion object : LongEntityClass<PollEndEntity>(PollEndTable)
}