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
    val result = enumeration<FilterReason>("result")
}

class UsageEntity(id: EntityID<Int>) : IntEntity(id) {
    var commandName by UsageTable.commandName
    var category by UsageTable.category
    val used by UsageTable.used
    var clear by UsageTable.clear
    var result by UsageTable.result

    companion object : IntEntityClass<UsageEntity>(UsageTable)
}

enum class FilterReason(val reason: String) {
    PASSED("Passed"),
    COMMANDS_DISABLED("Commands were disabled"),
    NOT_PRIVATE("Private Only command used outside DMs"),
    NOT_GUILD("Guild Only commands used outside guild"),
    NOT_BOT_OWNER("Not bot owner"),
    NOT_ADMIN("Not admin"),
    NOT_WHITELISTED("Not whitelisted in server"),
    NOT_POPULATED("Server was not populated"),
    NO_CHANNEL("Server does not have a channel set"),
    NOT_DEV_CHANNEL("Command used outside dev channel"),
    NOT_SERVER_CHANNEL("Command used outside server channel"),
    BLACKLISTED("Blacklisted")
}