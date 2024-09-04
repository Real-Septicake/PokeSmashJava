package io.github.septicake.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.slf4j.kotlin.getLogger

val logger by getLogger()

object PollTable : IntIdTable("votes") {
    val guild = long("guildId").index()
    val pokemon = integer("pokeId").index()
    val smashes = long("smashes")
    val passes = long("passes")
    val result = customEnumeration("result", fromDb = {
        res -> when(res) {
            0 -> PollResult.PASSED
            1 -> PollResult.SMASHED
            else -> {
                logger.error("Unexpected result: $res")
                PollResult.ERROR
            }
        }
    }, toDb = {
        res: PollResult -> res.value
    }).index()
}

class PollEntity(id: EntityID<Int>) : IntEntity(id) {
    var guild: Long by PollTable.guild
    var pokemon: Int by PollTable.pokemon
    var smashes: Long by PollTable.smashes
    var passes: Long by PollTable.passes
    var result: PollResult by PollTable.result

    companion object : IntEntityClass<PollEntity>(PollTable)
}

enum class PollResult(val value: Int) {
    PASSED(0),
    SMASHED(1),
    ERROR(-1)
}
