package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.GuildOnly
import io.github.septicake.cloud.annotations.Pokemon
import io.github.septicake.cloud.annotations.RequireOptions
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
            val smashes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.guild eq event.guild!!.idLong and (PollTable.result eq PollResult.SMASHED)
                }.count()
            }
            if(format == "count") {
                event.hook.sendMessage("Smash has won `$smashes` times").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.count()
                }
                event.hook.sendMessage("Smash has won `${"%.2f".format((smashes / total) * 100)}`% of the time").queue()
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
            val passes = transaction(bot.db) {
                PollTable.selectAll().where {
                    PollTable.guild eq event.guild!!.idLong and (PollTable.result eq PollResult.PASSED)
                }.count()
            }
            if(format == "count") {
                event.hook.sendMessage("Pass has won `$passes` times").queue()
            } else {
                val total = transaction(bot.db) {
                    PollTable.selectAll().where {
                        PollTable.guild eq event.guild!!.idLong
                    }.count()
                }
                event.hook.sendMessage("Pass has won `${"%.2f".format((passes / total) * 100)}`% of the time").queue()
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
}