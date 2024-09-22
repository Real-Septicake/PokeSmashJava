package io.github.septicake.commands

import dev.minn.jda.ktx.messages.Embed
import io.github.septicake.PokeSmashBot
import io.github.septicake.pokeapi.PokeApi
import io.github.septicake.pokeapi.PokemonInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
        val event = interaction.interactionEvent() ?: error("The interaction event should never be null")

        val jdaGuild = interaction.guild() ?: error("The guild should never be null")
        val pokemonEntity = bot.pokemonEntity(pokemon.id)
        val pollEntity = bot.pollEntity(jdaGuild.idLong, pokemon.id)

        Embed {
            title = pokemon.name
            color = PokeApi.pokemonColor(pokemon.id).colorFromName()
            url = "https://pokemondb.net/pokedex/%04d".format(pokemon.id)
            // description = // TODO: Find some reasonable way to get a description
            // could we use "https://img.pokemondb.net/artwork/large/${pokemon.name}.jpg" instead?
            thumbnail = pokemon.sprites["front_default"]?.jsonPrimitive?.contentOrNull

            timestamp = Clock.System.now().toJavaInstant()

            field(name = "Name", value = pokemon.name)
            field(name = "Height", value = "${pokemon.height * 10}cm") // height is in decimeters (why)
            field(name = "Weight", value = "%.1fkg".format(pokemon.weight / 10)) // weight is in hectograms (why)
            field(name = "Species", value = pokemon.species.name)
            field(name = "Types", value = pokemon.types.joinToString { it.type.name })
            field() // empty field to keep alignment

            field("Global Votes", value = "${pokemonEntity.smashes} Smashes • ${pokemonEntity.passes} Passes")

            if (pollEntity != null)
                field(name = "Server Votes", value = "${pollEntity.smashes} Smashes • ${pollEntity.passes} Passes")
            else
                field() // empty field to keep alignment

            footer {
                name = "Info for ${pokemon.name}"
            }
        }
    }
}
