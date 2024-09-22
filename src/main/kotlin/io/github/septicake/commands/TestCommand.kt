package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.pokeapi.PokemonInfo
import org.incendo.cloud.annotations.Argument
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
        interaction: JDAInteraction,
        @Argument(
            value = "pokemon",
            description = "The pokemon to query."
        )
        pokemon: PokemonInfo,
    ) {
        val event = interaction.interactionEvent() ?: return

        event.reply("why are you running this").queue()
    }
}
