package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.GuildOnly
import io.github.septicake.cloud.annotations.Pokemon
import io.github.septicake.cloud.annotations.RequireOptions
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.PollResult
import io.github.septicake.db.PollTable
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class PokeCommands(
    private val bot: PokeSmashBot
) {

    @Command("smash global totals <info> <format>")
    fun smashGlobalTotalCommand(
        interaction: JDAInteraction,
        @Argument("info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")")
        @RequireOptions("polls", "votes")
        info: String,
        @Argument("format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")")
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.result).where {
                    PollTable.result eq PollResult.SMASHED
                }.count()
            }
            if(format == "count") {
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
            if(format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for smash").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes have been for smash").queue()
            }
        }
    }

    @Command("smash global pokemon <info> <format> <pokemon>")
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
        val pokemonId = pokemon.toIntOrNull() ?: bot.map.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.SMASHED and (PollTable.pokemon eq pokemonId.toLong())
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage("`$smashes` server(s) have voted to smash `${bot.map[pokemonId]}`").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId.toLong()
                    }.count()
                }
                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
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

                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
                    event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                        bot.map[pokemonId]
                    } have been for smash")
                        .queue()
            }
        }
    }

    @Command("smash server totals <info> <format>")
    @GuildOnly
    fun smashServerTotalCommand(
        interaction: JDAInteraction,
        @Argument("info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")")
        @RequireOptions("polls", "votes")
        info: String,
        @Argument("format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")")
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(info == "polls") {
            val guildInfo = transaction(bot.db) {
                GuildEntity.findById(event.guild!!.idLong)
            }
            if(guildInfo == null) {
                event.hook.sendMessage("Server has not been populated yet.")
                return
            }
            if(guildInfo.smashes + guildInfo.passes == 0L) {
                event.hook.sendMessage("Server has not completed any polls")
                return
            }
            if(format == "count") {
                event.hook.sendMessage("Smash has won `${guildInfo.smashes}` times").queue()
            } else {
                event.hook.sendMessage("Smash has won `${"%.2f".format((guildInfo.smashes / (guildInfo.smashes + guildInfo.passes)) * 100)}`% of the time").queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.guild eq event.guild!!.idLong
                }.sumOf { it[PollTable.smashes] }
            }
            if(format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for smash").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes have been for smash").queue()
            }
        }
    }

    @Command("smash server pokemon <info> <format> <pokemon>")
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
        val pokemonId = pokemon.toIntOrNull() ?: bot.map.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.SMASHED and (PollTable.pokemon eq pokemonId.toLong()
                            and (PollTable.guild eq event.guild!!.idLong))
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage("`$smashes` server(s) have voted to smash `${bot.map[pokemonId]}`").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId.toLong() and (PollTable.guild eq event.guild!!.idLong)
                    }.count()
                }
                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
                    event.hook.sendMessage("Smash has won `${"%.2f".format((smashes / total) * 100)}`% of the time").queue()
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

                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
                    event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                        bot.map[pokemonId]
                    } have been for smash")
                        .queue()
            }
        }
    }

    @Command("pass global totals <info> <format>")
    fun passGlobalTotalCommand(
        interaction: JDAInteraction,
        @Argument("info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")")
        @RequireOptions("polls", "votes")
        info: String,
        @Argument("format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")")
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(info == "polls") {
            val passes = transaction(bot.db) {
                PollTable.select(PollTable.result).where {
                    PollTable.result eq PollResult.PASSED
                }.count()
            }
            if(format == "count") {
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
            if(format == "count") {
                event.hook.sendMessage("There have been `$passes` votes for pass").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((passes / total) * 100)}`% of the total votes have been for pass").queue()
            }
        }
    }

    @Command("pass server totals <info> <format>")
    @GuildOnly
    fun passServerTotalCommand(
        interaction: JDAInteraction,
        @Argument("info",
            description = "What info should be should be displayed (either \"polls\" or \"votes\")")
        @RequireOptions("polls", "votes")
        info: String,
        @Argument("format",
            description = "What format the info should be displayed in (either \"count\" or \"percent\")")
        @RequireOptions("count", "percent")
        format: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(info == "polls") {
            val guildInfo = transaction(bot.db) {
                GuildEntity.findById(event.guild!!.idLong)
            }
            if(guildInfo == null) {
                event.hook.sendMessage("Server has not been populated yet.")
                return
            }
            if(guildInfo.smashes + guildInfo.passes == 0L) {
                event.hook.sendMessage("Server has not completed any polls")
                return
            }
            if(format == "count") {
                event.hook.sendMessage("Pass has won `${guildInfo.passes}` times").queue()
            } else {
                event.hook.sendMessage("Pass has won `${"%.2f".format((guildInfo.passes / (guildInfo.smashes + guildInfo.passes)) * 100)}`% of the time").queue()
            }
        } else {
            val passes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.guild eq event.guild!!.idLong
                }.sumOf { it[PollTable.passes] }
            }
            if(format == "count") {
                event.hook.sendMessage("There have been `$passes` votes for pass").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }
                event.hook.sendMessage("`${"%.2f".format((passes / total) * 100)}`% of the total votes have been for pass").queue()
            }
        }
    }

    @Command("pass global pokemon <info> <format> <pokemon>")
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
        val pokemonId = pokemon.toIntOrNull() ?: bot.map.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.PASSED and (PollTable.pokemon eq pokemonId.toLong())
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage("`$smashes` server(s) have voted to pass `${bot.map[pokemonId]}`").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId.toLong()
                    }.count()
                }
                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
                    event.hook.sendMessage("Pass has won `${"%.2f".format((smashes / total) * 100)}`% of the time").queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.passes).sumOf { it[PollTable.passes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for pass").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }

                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
                    event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                        bot.map[pokemonId]
                    } have been for pass")
                        .queue()
            }
        }
    }

    @Command("pass server pokemon <info> <format> <pokemon>")
    @GuildOnly
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
        val pokemonId = pokemon.toIntOrNull() ?: bot.map.inverse()[pokemon.lowercase()]!!
        if (info == "polls") {
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.result eq PollResult.PASSED and (PollTable.pokemon eq pokemonId.toLong()
                            and (PollTable.guild eq event.guild!!.idLong))
                }.count()
            }
            if (format == "count") {
                event.hook.sendMessage("`$smashes` server(s) have voted to pass `${bot.map[pokemonId]}`").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.pokemon eq pokemonId.toLong() and (PollTable.guild eq event.guild!!.idLong)
                    }.count()
                }
                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
                    event.hook.sendMessage("Pass has won `${"%.2f".format((smashes / total) * 100)}`% of the time").queue()
            }
        } else {
            val smashes = transaction(bot.db) {
                PollTable.select(PollTable.passes).where {
                    PollTable.guild eq event.guild!!.idLong
                }.sumOf { it[PollTable.passes] }
            }
            if (format == "count") {
                event.hook.sendMessage("There have been `$smashes` votes for pass").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.sumOf {
                        it[PollTable.smashes] + it[PollTable.passes]
                    }
                }

                if(total == 0L)
                    event.hook.sendMessage("No server has completed a poll for `${bot.map[pokemonId]}`")
                else
                    event.hook.sendMessage("`${"%.2f".format((smashes / total) * 100)}`% of the total votes for ${
                        bot.map[pokemonId]
                    } have been for pass")
                        .queue()
            }
        }
    }
}