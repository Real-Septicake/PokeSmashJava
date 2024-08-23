package io.github.septicake.db

import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.slf4j.kotlin.getLogger

val logger by getLogger()

object PollTable : CompositeIdTable("votes") {
    val guild = long("guildId").index()
    val pokemon = long("pokeId").index()
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

class PollEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    val guild: Long by PollTable.guild
    val pokemon: Long by PollTable.pokemon
    var smashes: Long by PollTable.smashes
    var passes: Long by PollTable.passes
    val result: PollResult by PollTable.result

    companion object : CompositeEntityClass<PollEntity>(PollTable)
}

enum class PollResult(val value: Int) {
    PASSED(0),
    SMASHED(1),
    ERROR(-1)
}
