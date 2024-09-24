package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.slf4j.kotlin.getLogger

class TestCommand(
    private val bot: PokeSmashBot,
) {
    private val logger by getLogger()

    // @CommandScope(guilds = [-1])
    @Command("test <pokemon>")
    suspend fun testCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: error("The interaction event should never be null")

        event.reply("why're you using this?").queue()
    }
}
