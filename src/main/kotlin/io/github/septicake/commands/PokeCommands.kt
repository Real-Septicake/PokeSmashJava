@file:Suppress("unused")

package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.*
import io.github.septicake.db.GuildEntity
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
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
    @CommandDescription("Reset poll count, starting from bulbasaur again, or whatever national dex number 1 is")
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

    @Command("smash global totals <info> <format>")
    @CommandDescription("Get global total pass data.")
    fun smashGlobalTotalCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.result).where {
                    PollTable.result eq PollResult.SMASHED
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage("Smash has won `$smashes` times").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().count()
                }
                event.hook.sendMessage("Smash has won `${"%.2f".format((smashes / total) * 100)}`% of the time").queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.smashes).sumOf { it[PollTable.smashes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for smash").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes have been for smash")
                    .queue()
            }
        }
    }

    @Command("smash global pokemon <info> <format> <pokemon>")
    @CommandDescription("Get the global smash data on the specified pokemon. Due to api shenanigans, using the national dex number is suggested")
    fun smashGlobalPokemonCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String,
        @Argument("pokemon")
        @Pokemon
        pokemon: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val pokemonId = pokemon.toIntOrNull() ?: bot.pokemonMap.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.SMASHED and (PollTable.pokemon eq pokemonId)
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage(
                    "`$smashes` server(s) have voted to smash `${
                        bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                            Char::titlecase
                        )
                    }`"
                ).queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId
                    }.count()
                }
                if (total == 0L)
                    event.hook.sendMessage(
                        "No server has completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage("Smash has won `${"%.2f".format((smashes / total) * 100)}`% of the time")
                        .queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.smashes).sumOf { it[PollTable.smashes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for smash").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }

                if (total == 0L)
                    event.hook.sendMessage(
                        "No server has completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage(
                        "`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                            bot.pokemonMap[pokemonId]
                        } have been for smash"
                    )
                        .queue()
            }
        }
    }

    @GuildOnly
    @Command("smash server totals <info> <format>")
    @CommandDescription("Get this server's total smash data.")
    fun smashServerTotalCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if (info == "polls") {
            val guildInfo = transaction(bot.db) {
                GuildEntity.findById(event.guild!!.idLong)
            }
            if (guildInfo == null) {
                event.hook.sendMessage("Server has not been populated yet.")
                return
            }
            if (guildInfo.smashes + guildInfo.passes == 0L) {
                event.hook.sendMessage("Server has not completed any polls")
                return
            }
            if (format == "count") {
                event.hook.sendMessage("Smash has won `${guildInfo.smashes}` times").queue()
            } else {
                event.hook.sendMessage("Smash has won `${"%.2f".format((guildInfo.smashes / (guildInfo.smashes + guildInfo.passes)) * 100)}`% of the time")
                    .queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.guild eq event.guild!!.idLong
                }.sumOf { it[PollTable.smashes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for smash").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes have been for smash")
                    .queue()
            }
        }
    }

    @GuildOnly
    @Command("smash server pokemon <info> <format> <pokemon>")
    @CommandDescription("Get this server's smash data on the specified pokemon. Due to api shenanigans, using the national dex number is suggested")
    fun smashServerPokemonCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String,
        @Argument("pokemon")
        @Pokemon
        pokemon: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val pokemonId = pokemon.toIntOrNull() ?: bot.pokemonMap.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.SMASHED and (PollTable.pokemon eq pokemonId
                            and (PollTable.guild eq event.guild!!.idLong))
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage(
                    "`$smashes` server(s) have voted to smash `${
                        bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                            Char::titlecase
                        )
                    }`"
                ).queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId and (PollTable.guild eq event.guild!!.idLong)
                    }.count()
                }
                if (total == 0L)
                    event.hook.sendMessage(
                        "No server has completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage("Smash has won `${"%.2f".format((smashes / total) * 100)}`% of the time")
                        .queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.smashes).where {
                    PollTable.guild eq event.guild!!.idLong
                }.sumOf { it[PollTable.smashes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for smash").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }

                if (total == 0L)
                    event.hook.sendMessage(
                        "No server has completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage(
                        "`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(Char::titlecase)
                        } have been for smash"
                    )
                        .queue()
            }
        }
    }

    @Command("pass global totals <info> <format>")
    @CommandDescription("Get global total pass data")
    fun passGlobalTotalCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if (info == "polls") {
            val passes = transaction(bot.db) {
                PollTable.select(PollTable.result).where {
                    PollTable.result eq PollResult.PASSED
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage("Pass has won `$passes` times").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().count()
                }
                event.hook.sendMessage("Pass has won `${"%.2f".format((passes / total) * 100)}`% of the time").queue()
            }
        } else {
            val passes = transaction(bot.db) {
                PollTable.select(PollTable.passes).sumOf { it[PollTable.passes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$passes` votes for pass").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((passes / total) * 100)}`% of the total votes have been for pass")
                    .queue()
            }
        }
    }

    @GuildOnly
    @Command("pass server totals <info> <format>")
    @CommandDescription("Get this server's total pass data.")
    fun passServerTotalCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if (info == "polls") {
            val guildInfo = transaction(bot.db) {
                GuildEntity.findById(event.guild!!.idLong)
            }
            if (guildInfo == null) {
                event.hook.sendMessage("Server has not been populated yet.")
                return
            }
            if (guildInfo.smashes + guildInfo.passes == 0L) {
                event.hook.sendMessage("Server has not completed any polls")
                return
            }
            if (format == "count") {
                event.hook.sendMessage("Pass has won `${guildInfo.passes}` times").queue()
            } else {
                event.hook.sendMessage("Pass has won `${"%.2f".format((guildInfo.passes / (guildInfo.smashes + guildInfo.passes)) * 100)}`% of the time")
                    .queue()
            }
        } else {
            val passes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.guild eq event.guild!!.idLong
                }.sumOf { it[PollTable.passes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$passes` votes for pass").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((passes / total) * 100)}`% of the total votes have been for pass")
                    .queue()
            }
        }
    }

    @Command("pass global pokemon <info> <format> <pokemon>")
    @CommandDescription("Get global pass data on the specified pokemon. Due to api shenanigans, using the national dex number is suggested")
    fun passGlobalPokemonCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String,
        @Argument("pokemon")
        @Pokemon
        pokemon: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val pokemonId = pokemon.toIntOrNull() ?: bot.pokemonMap.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.PASSED and (PollTable.pokemon eq pokemonId)
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage(
                    "`$smashes` server(s) have voted to pass `${
                        bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                            Char::titlecase
                        )
                    }`"
                ).queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId
                    }.count()
                }
                if (total == 0L)
                    event.hook.sendMessage(
                        "No server has completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage("Pass has won `${"%.2f".format((smashes / total) * 100)}`% of the time")
                        .queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.passes).where {
                    PollTable.pokemon eq pokemonId
                }.sumOf { it[PollTable.passes] }
            }
            if (format == "count") {
                event.hook.sendMessage(
                    "There have been `$smashes` votes to pass ${
                        bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                            Char::titlecase
                        )
                    }"
                ).queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }

                if (total == 0L)
                    event.hook.sendMessage(
                        "No server has completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage(
                        "`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                            bot.pokemonMap[pokemonId]
                        } have been for pass"
                    )
                        .queue()
            }
        }
    }

    @GuildOnly
    @Command("pass server pokemon <info> <format> <pokemon>")
    @CommandDescription("Get this server's pass data on the specified pokemon. Due to api shenanigans, using the national dex number is suggested")
    fun passServerPokemonCommand(
        interaction: JDAInteraction,
        @Argument(
            "info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")"
        )
        @RequireOptions("polls", "votes")
        info: String,
        @Argument(
            "format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")"
        )
        @RequireOptions("count", "percent")
        format: String,
        @Argument("pokemon")
        @Pokemon
        pokemon: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val pokemonId = pokemon.toIntOrNull() ?: bot.pokemonMap.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.PASSED and (PollTable.pokemon eq pokemonId
                            and (PollTable.guild eq event.guild!!.idLong))
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage(
                    "`$smashes` server(s) have voted to pass `${
                        bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                            Char::titlecase
                        )
                    }`"
                ).queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId and (PollTable.guild eq event.guild!!.idLong)
                    }.count()
                }
                if (total == 0L)
                    event.hook.sendMessage(
                        "This server has not completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage(
                        "Pass has won `${"%.2f".format((smashes / total) * 100)}`% of the time for ${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }"
                    ).queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.passes).where {
                    PollTable.guild eq event.guild!!.idLong
                }.sumOf { it[PollTable.passes] }
            }
            if (format == "count") {
                event.hook.sendMessage(
                    "There have been `$smashes` votes to pass ${
                        bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                            Char::titlecase
                        )
                    }"
                ).queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }

                if (total == 0L)
                    event.hook.sendMessage(
                        "This server has not completed a poll for `${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(
                                Char::titlecase
                            )
                        }`"
                    )
                else
                    event.hook.sendMessage(
                        "`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                            bot.pokemonMap[pokemonId]!!.replaceFirstChar(Char::titlecase)
                        } have been for pass"
                    )
                        .queue()
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
            return@foldIndexed acc + (if (i != 0) ", " else "") + eggGroup.fetchInfo().names.find { it.language.name == "en" }!!.name
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

