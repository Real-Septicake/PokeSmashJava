@file:Suppress("unused")

package io.github.septicake.commands

import dev.minn.jda.ktx.coroutines.await
import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.*
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.PokemonTable
import io.github.septicake.db.PollResult
import io.github.septicake.db.PollTable
import io.github.septicake.pokeapi.PokeApi
import io.github.septicake.pokeapi.PokemonInfo
import io.github.septicake.pokeapi.PokemonSpeciesInfo
import io.github.septicake.util.sendMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.messages.MessagePollData
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info
import kotlin.math.min

class PokeCommands(
    private val bot: PokeSmashBot
) {
    val logger by getLogger()

    @Command("reset")
    @GuildOnly
    @UserPermissions(whitelistOnly = true)
    @CommandDescription("Reset poll count, starting from bulbasaur, or whatever national dex number 1 is")
    fun resetCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val info = transaction(bot.db) {
            GuildEntity.findById(event.guild!!.idLong)
        }
        if (info == null) {
            event.hook.sendMessage("Server has not been populated yet.").queue()
        } else {
            transaction(bot.db) { info.offset = 0 }
            event.hook.sendMessage("Reset successful").queue()
        }
    }

    @Command("next")
    @GuildOnly
    @UserPermissions(whitelistOnly = true)
    @ChannelRestriction(serverChannel = true)
    @CommandsEnabled
    @CommandDescription("Send the next polls")
    suspend fun nextCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val info = transaction(bot.db) {
            GuildEntity.findById(event.guild!!.idLong)
        }
        if (info == null) {
            event.hook.sendMessage("Server has not been populated yet.").queue()
        } else {
            if (!event.guild!!.selfMember.hasPermission(
                    event.guildChannel,
                    Permission.CREATE_PUBLIC_THREADS,
                    Permission.MESSAGE_SEND_POLLS,
                    Permission.MESSAGE_SEND_IN_THREADS
                )
            ) {
                event.hook.sendMessage("Insufficient permissions. Requires `Create Public Threads`, `Send Messages in Threads`, and, `Create Polls`").queue()
                return
            }
            val count = min(bot.pokemonMap.size - info.offset, info.polls)
            if (count != 0) {
                PokeApi.listPokemonPaged(info.offset, count).results.forEach { pokemon ->
                    (event.channel as MessageChannel).sendMessage("").setPoll(
                        MessagePollData.builder(pokemon.name.replaceFirstChar { it.titlecase() })
                            .addAnswer("Smash")
                            .addAnswer("Pass")
                            .build()
                    ).complete().createThreadChannel(pokemon.name.replaceFirstChar { it.titlecase() }).complete()
                        .sendMessage(pokemon.fetchInfo().sprites["front_default"]!!.jsonPrimitive.content)
                        .queue()
                }
                event.hook.sendMessage("Next `$count` pokemon sent").queue()
                transaction(bot.db) {
                    info.offset += count
                }
            } else {
                event.hook.sendMessage("Final pokemon reached, run `/reset` to start again").queue()
            }
        }
    }

    @Command("global totals")
    suspend fun globalTotalCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().await()
        var smashes = 0L
        var passes = 0L
        var smashWins = 0L
        var passWins = 0L
        transaction(bot.db) {
            smashWins =
                PokemonTable.select(PokemonTable.smashWins).sumOf {
                    it[PokemonTable.smashWins]
                }
            passWins =
                PokemonTable.select(PokemonTable.passWins).sumOf {
                    it[PokemonTable.passWins]
                }

            smashes =
                PokemonTable.select(PokemonTable.smashes).sumOf {
                    it[PokemonTable.smashes]
                }
            passes =
                PokemonTable.select(PokemonTable.passes).sumOf {
                    it[PokemonTable.passes]
                }
        }

        event.hook.sendMessage {
            embed {
                title = "Global Totals"
                timestamp = Clock.System.now().toJavaInstant()

                field("Smash Votes", smashes.toString())
                field("Pass Votes", passes.toString())
                field("Smash Vote Percent", "%.2f%%".format((smashes.toDouble() / (smashes + passes)) * 100))
                field("Smash Wins", smashWins.toString())
                field("Pass Wins", passWins.toString())
                field("Smash Win Percent", "%.2f%%".format((smashWins.toDouble() / (smashWins + passWins)) * 100))
            }
        }
    }

    @Command("server totals")
    @GuildOnly
    suspend fun serverTotalCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        val guild = event.guild!!
        val guildEntity = bot.guildEntity(guild)

        event.deferReply().await()
        var smashes = 0L
        var passes = 0L
        var smashWins = 0L
        var passWins = 0L

        transaction(bot.db) {
            smashWins = guildEntity.smashes
            passWins = guildEntity.passes

            smashes =
                PollTable.select(PollTable.smashes, PollTable.guild).where {
                    PollTable.guild eq guild.idLong
                }.sumOf {
                    it[PollTable.smashes]
                }
            passes =
                PollTable.select(PollTable.passes, PollTable.guild).where {
                    PollTable.guild eq guild.idLong
                }.sumOf {
                    it[PollTable.passes]
                }
        }

        event.hook.sendMessage {
            embed {
                title = "${guild.name} Totals"
                timestamp = Clock.System.now().toJavaInstant()

                field("Smash Votes", smashes.toString())
                field("Pass Votes", passes.toString())
                field("Smash Vote Percent", "%.2f%%".format((smashes.toDouble() / (smashes + passes)) * 100))
                field("Smash Wins", smashWins.toString())
                field("Pass Wins", passWins.toString())
                field("Smash Win Percent", "%.2f%%".format((smashWins.toDouble() / (smashWins + passWins)) * 100))
            }
        }
    }

    @Command("global pokemon <pokemon>")
    suspend fun globalPokemonCommand(
        interaction: JDAInteraction,
        @Argument("pokemon")
        pokemon: PokemonInfo
    ) {
        val event = interaction.interactionEvent() ?: return
        val pokemonEntity = bot.pokemonEntity(pokemon.id)

        event.deferReply().await()
        if(pokemonEntity == null) {
            event.hook.sendMessage("${pokemon.name.replaceFirstChar { it.titlecase() }} has no completed polls").queue()
            return
        }

        event.hook.sendMessage {
            embed {
                title = "Global ${pokemon.name.replaceFirstChar { it.titlecase() }} Info"
                timestamp = Clock.System.now().toJavaInstant()
                thumbnail = pokemon.sprites["front_default"]?.jsonPrimitive?.contentOrNull
                color = pokemon.species.fetchInfo().color.colorFromName()
                url = "https://pokemondb.net/pokedex/%04d".format(pokemon.id)

                field("Smash Votes", pokemonEntity.smashes.toString())
                field("Pass Votes", pokemonEntity.passes.toString())
                field("Smash Vote Percent", "%.2f%%".format((pokemonEntity.smashes.toDouble() / (pokemonEntity.smashes + pokemonEntity.passes)) * 100))
                field("Smash Wins", pokemonEntity.smashWins.toString())
                field("Pass Wins", pokemonEntity.passWins.toString())
                field("Smash Win Percent", "%.2f%%".format((pokemonEntity.smashWins.toDouble() / (pokemonEntity.smashWins + pokemonEntity.passWins)) * 100))
            }
        }
    }

    @Command("server pokemon <pokemon>")
    @GuildOnly
    suspend fun serverPokemonCommand(
        interaction: JDAInteraction,
        @Argument("pokemon")
        pokemon: PokemonInfo
    ) {
        val event = interaction.interactionEvent() ?: return
        val guild = event.guild!!
        event.deferReply().await()

        val pollEntity = bot.pollEntity(guild.idLong, pokemon.id)
        if(pollEntity == null) {
            event.hook.sendMessage("This server has not completed a poll for ${pokemon.name.replaceFirstChar { it.titlecase() }}").queue()
            return
        }

        event.hook.sendMessage {
            embed {
                title = "${guild.name} ${pokemon.name.replaceFirstChar { it.titlecase() }} Info"
                timestamp = Clock.System.now().toJavaInstant()
                thumbnail = pokemon.sprites["front_default"]?.jsonPrimitive?.contentOrNull
                color = pokemon.species.fetchInfo().color.colorFromName()
                url = "https://pokemondb.net/pokedex/%04d".format(pokemon.id)

                field("Result", if(pollEntity.result == PollResult.SMASHED) "**Smashed!**" else "**Passed!**")
                field("Votes", "${pollEntity.smashes} smashes | ${pollEntity.passes} passes")
                field("Smash Percentage", "%.2f%%".format((pollEntity.smashes.toDouble() / (pollEntity.smashes + pollEntity.passes)) * 100))
            }
        }
    }

    @GuildOnly
    @Command("pokemon info <pokemon>")
    suspend fun pokemonInfoCommand(
        interaction: JDAInteraction,
        @Argument(
            value = "pokemon",
            description = "The pokemon to query. Using the national dex number is suggested"
        )
        pokemon: PokemonInfo,
    ) {
        val event = interaction.interactionEvent() ?: error("The interaction event should never be null")

        event.deferReply().queue()

        val jdaGuild = interaction.guild() ?: error("The guild should never be null")
        val pokemonEntity = bot.pokemonEntity(pokemon.id)
        val pollEntity = bot.pollEntity(jdaGuild.idLong, pokemon.id)

        val species = pokemon.species.fetchInfo()
        val flavor = species.flavorTexts.findLast { it.language.name == "en" }!!

        logger.info { "Pokemon ${pokemon.name} (${pokemon.id}) info checked in server ${jdaGuild.name}" }

        event.hook.sendMessage {
            embed {
                title = pokemon.name.replaceFirstChar { it.titlecase() }
                color = species.color.colorFromName()
                url = "https://pokemondb.net/pokedex/%04d".format(pokemon.id)
                description = flavor.formatFlavorText()
                // description = // TODO: Find some reasonable way to get a description
                // could we use "https://img.pokemondb.net/artwork/large/${pokemon.name}.jpg" instead?
                thumbnail = pokemon.sprites["front_default"]?.jsonPrimitive?.contentOrNull

                timestamp = Clock.System.now().toJavaInstant()

                field(name = "Name", value = pokemon.name.replaceFirstChar { it.titlecase() })
                field(name = "Height", value = "${pokemon.height * 10}cm") // height is in decimeters (why)
                field(name = "Weight", value = "%.1fkg".format(pokemon.weight / 10.0)) // weight is in hectograms (why)
                field(name = "Species", value = species.name.replaceFirstChar { it.titlecase() })
                field(
                    name = "Types",
                    value = pokemon.types.joinToString(separator = " & ") { type -> type.type.name.replaceFirstChar { it.titlecase() } })
                field() // empty field to keep alignment

                if (pokemonEntity != null)
                    field("Global Votes", value = "${pokemonEntity.smashes} Smashes • ${pokemonEntity.passes} Passes")
                else
                    field("Global Votes", value = "No polls have been completed for this pokemon")

                if (pollEntity != null)
                    field(name = "Server Votes", value = "${pollEntity.smashes} Smashes • ${pollEntity.passes} Passes")
                else
                    field() // empty field to keep alignment

                field(name = "National Dex Number", value = "${pokemon.id}")

                footer {
                    name = "Info for ${pokemon.name.replaceFirstChar { it.titlecase() }} • Pokedex Entry: ${flavor.version.fetchInfo().names.find { it.language.name == "en" }!!.name}"
                }
            }
        }
    }

    @Command("species info <species>")
    suspend fun speciesInfoCommand(
        interaction: JDAInteraction,
        @Argument("species")
        species: PokemonSpeciesInfo
    ) {
        val event = interaction.interactionEvent() ?: return

        event.deferReply().queue()

        val flavor = species.flavorTexts.find { it.language.name == "en" }!!
        val generation = species.generation.fetchInfo()
        val default = species.varieties.find { it.isDefault }!!
        val groups = species.eggGroups.foldIndexed("") { i, acc, eggGroup ->
            acc + (if (i != 0) ", " else "") + eggGroup.fetchInfo().names.find { it.language.name == "en" }!!.name
        }

        logger.info { "Species ${species.name} (${species.id}) info checked in server ${interaction.guild()?.name ?: "DMs"}" }

        event.hook.sendMessage {
            embed {
                title = species.name.replaceFirstChar { it.titlecase() }
                color = species.color.colorFromName()
                url = "https://pokemondb.net/pokedex/%04d".format(species.id)
                description = flavor.formatFlavorText()
                thumbnail = default.pokemon.fetchInfo().sprites["front_default"]?.jsonPrimitive?.contentOrNull

                timestamp = Clock.System.now().toJavaInstant()

//                field(name = "Name", value = species.name.replaceFirstChar { it.titlecase() })
                field(name = "Default Form", value = default.pokemon.name.replaceFirstChar { it.titlecase() })
                field(name = "Genus", value = species.genera.find { it.language.name == "en" }!!.genus)
                field(name = "Egg Groups", value = groups)

                footer { name = "First from " + generation.names.find { it.language.name == "en" }!!.name }
            }
        }
    }
}

