@file:Suppress("unused")

package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.Category
import io.github.septicake.cloud.annotations.CategoryEnum
import io.github.septicake.cloud.annotations.LongDescription
import io.github.septicake.cloud.annotations.ProperName
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.slf4j.kotlin.getLogger

@Category(CategoryEnum.TEST)
class TestCommand(
    private val bot: PokeSmashBot
) {
    private val logger by getLogger()

    @Command("test")
    @ProperName("Test")
    @CommandDescription("it;s a test command")
    @LongDescription("it's really just a test command")
    fun testCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return

        event.reply("why're you using this?").queue()
    }
}
