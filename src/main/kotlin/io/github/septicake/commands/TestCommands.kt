package io.github.septicake.commands

import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info

class TestCommands {
    private val logger by getLogger()

    @Command("test thing <number> [string]")
    fun testCommand(
        interaction: JDAInteraction,
        number: Int,
        @Argument("string")
        @Default("string!")
        str: String,
    ) {
        logger.info { "Invoked test command with arguments '$number' and '$str'" }
    }
}
