package io.github.septicake

import io.github.septicake.util.getEnv

object PokeSmashConstants {
    val ownerId = getEnv("BOT_OWNER")!!.toLong()

    val whitelist = getEnv("BOT_WHITELIST")!!.split(";").map { s -> s.toLong() }
}
