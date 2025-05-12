@file:Suppress("unused")

package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.ChannelRestriction
import io.github.septicake.cloud.annotations.CommandParams
import io.github.septicake.cloud.annotations.UserPermissions
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.GuildTable
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.error
import org.slf4j.kotlin.getLogger

class DevCommands(
    private val bot: PokeSmashBot
) {
    val logger by getLogger()

    @Command("allow commands <val>")
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("val")
    @CommandDescription("Only usable by bot developer")
    fun commandToggleCommand(
        interaction: JDAInteraction,
        @Argument("val")
        value: Boolean
    ) {
        val event = interaction.interactionEvent() ?: return
        bot.commandsEnabled = value
        event.reply("Commands ${if (value) "enabled" else "disabled"}.").queue()
    }

    @Command("msg <server> <msg>")
    @CommandDescription("Only usable by bot developer")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("server", "msg")
    fun messageCommand(
        interaction: JDAInteraction,
        @Argument("server")
        server: Long,
        @Argument("msg")
        @Greedy
        str: String
    ) {
        val event = interaction.interactionEvent() ?: return
        val info = transaction(bot.db) {
            GuildEntity.findById(server)
        }
        if (info == null) {
            event.reply("Server `$server` either does not exist or has not been populated.").queue()
        } else {
            if (info.channel != null) {
                val channel = bot.jda.getTextChannelById(info.channel!!)
                if (channel == null) {
                    event.reply("Server's channel does not exist.").queue()
                    return
                } else {
                    event.deferReply().queue()
                    try {
                        channel.sendMessage(str).queue()
                        event.hook.sendMessage("Message sent to ${info.name}.").queue()
                        return
                    } catch (e: InsufficientPermissionException) {
                        logger.error { "Could not send message to ${info.name}, lacking permissions" }
                        event.hook.sendMessage("Lacking permissions").queue()
                        return
                    }
                }
            } else {
                event.reply("Server has not set a channel yet.")
            }
        }
    }

    @Command("announce <msg>")
    @CommandDescription("Only usable by bot developer")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("msg")
    fun announceCommand(
        interaction: JDAInteraction,
        @Argument("msg")
        @Greedy
        msg: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        var sent = 0
        var noChannel = 0
        var lackPerms = 0
        val format = msg.contains("%owner%")
        transaction(bot.db) {
            GuildTable.select(GuildTable.channel, GuildTable.id, GuildTable.name).forEach {
                if (it[GuildTable.channel] == null) {
                    noChannel++
                    return@forEach
                }

                try {
                    if (format) {
                        val guild = bot.jda.getGuildById(it[GuildTable.id].value)!!
                        val formattedStr = msg.replace("%owner%", "<@${guild.ownerId}>")
                        bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage(formattedStr).queue()
                    } else
                        bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage(msg).queue()
                } catch (e: InsufficientPermissionException) {
                    logger.error { "Could not send message to ${it[GuildTable.name]}, lacking permissions" }
                    lackPerms++
                }

                sent++
            }
        }
        event.hook.sendMessage(
            "Announcement sent to $sent server${if (sent != 1) "s" else ""}.\n" +
                    "$noChannel server${if (noChannel != 1) "s" else ""} did not have a channel.\n" +
                    "$lackPerms server${if (noChannel != 1) "s" else ""} did not give bot necessary permissions"
        ).queue()
    }

    @Command("shutdown [test]")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("test")
    @CommandDescription("Only usable by bot developer")
    suspend fun shutdownCommand(
        interaction: JDAInteraction,
        @Argument("test", description = "The shutdown is for a test and does not announce the shutdown to servers")
        test: Boolean = true
    ) {
        val event = interaction.interactionEvent() ?: return
        event.reply("Shutting down...").queue()
        if (!test) {
            transaction(bot.db) {
                GuildTable.select(GuildTable.channel, GuildTable.id, GuildTable.name).forEach {
                    if (it[GuildTable.channel] == bot.testingChannel!!.toLong())
                        return@forEach

                    if (it[GuildTable.channel] == null) {
                        return@forEach
                    }

                    try {
                        bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage("Bot shutting down...")
                            .queue()
                    } catch (e: InsufficientPermissionException) {
                        logger.error { "Could not send message to ${it[GuildTable.name]}, lacking permissions" }
                    }
                }
            }
        }
        bot.shutdown()
    }

    @Command("resolve <guild>")
    @UserPermissions(botOwnerOnly = true)
    @Command("guild")
    @CommandDescription("Only usable by bot developer")
    fun resolveCommand(
        interaction: JDAInteraction,
        @Argument("guild", description = "The guild id to resolve")
        guildId: Long
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        val guild = transaction(bot.db) {
            GuildEntity.findById(guildId)
        }

        if (guild == null) {
            event.hook.sendMessage("Guild \"$guildId\" does not exist in database").queue()
        } else {
            event.hook.sendMessage("Guild \"$guildId\" resolves to ${guild.name}")
        }
    }
}