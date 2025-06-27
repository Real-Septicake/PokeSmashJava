package io.github.septicake

import io.github.septicake.util.getEnv

object PokeSmashConstants {
    val ownerId = getEnv("BOT_OWNER")!!.toLong()

    val whitelist = getEnv("BOT_WHITELIST")!!.split(";").map { s -> s.toLong() }

    val PollCheckIdentity = "PollCheck"
    val UsageClearIdentity = "UsageClear"

    object Errors {
        const val NOT_POPULATED = "Server has not yet been populated, get an admin to run `/setup`"
    }
}
