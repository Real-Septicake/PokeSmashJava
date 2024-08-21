package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.slf4j.kotlin.getLogger

class TestCommand(
    private val bot: PokeSmashBot,
) {
    private val logger by getLogger()

    fun testCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return

    }
}
