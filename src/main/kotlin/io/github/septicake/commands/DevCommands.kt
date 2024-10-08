package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.ChannelRestriction
import io.github.septicake.cloud.annotations.CommandName
import io.github.septicake.cloud.annotations.CommandParams
import io.github.septicake.cloud.annotations.UserPermissions
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.GuildTable
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.transactions.transaction

class DevCommands(
    private val bot : PokeSmashBot
) {

    @Command("allow commands <val>")
    @UserPermissions(botOwnerOnly = true)
    @CommandName("Command Toggle")
    @CommandParams("val")
    fun commandToggleCommand(
        interaction: JDAInteraction,
        @Argument("val")
        value: Boolean
    ) {
        val event = interaction.interactionEvent() ?: return
        bot.commandsEnabled = value
        event.reply("Commands ${if(value) "enabled" else "disabled"}.").queue()
    }

    @Command("message <server> <msg>")
    @CommandDescription("Send a message to a server's channel.")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandName("Message")
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
        if(info == null) {
            event.reply("Server `$server` either does not exist or has not been populated.").queue()
        } else {
            if(info.channel != null) {
                val channel = bot.jda.getTextChannelById(info.channel!!)
                if(channel == null) {
                    event.reply("Server's channel does not exist.").queue()
                    return
                } else {
                    event.deferReply().queue()
                    channel.sendMessage(str).queue()
                    event.hook.sendMessage("Message sent.").queue()
                }
            } else {
                event.reply("Server has not set a channel yet.")
            }
        }
    }

    @Command("announce <msg>")
    @CommandDescription("Announce a message to every server's channel.")
    @UserPermissions(botOwnerOnly = true)
    @CommandName("Announce")
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
        val format = msg.contains("%owner%")
        transaction(bot.db) {
            GuildTable.select(GuildTable.channel, GuildTable.id).forEach {
                if(it[GuildTable.channel] == bot.testingChannel!!.toLong())
                    return@forEach

                if(it[GuildTable.channel] == null) {
                    noChannel++
                    return@forEach
                }

                if(format){
                    val guild = bot.jda.getGuildById(it[GuildTable.id].value)!!
                    val formattedStr = msg.replace("%owner%", "<@${guild.ownerId}>")
                    bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage(formattedStr).queue()
                } else
                    bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage(msg).queue()

                sent++
            }
        }
        event.hook.sendMessage("Announcement sent to $sent server${if(sent > 0) "s" else ""}. $noChannel server${if(noChannel > 0) "s" else ""} did not have a channel.").queue()
    }

    @Command("shutdown [test]")
    @UserPermissions(botOwnerOnly = true)
    @CommandName("Shutdown")
    @CommandParams("test")
    suspend fun shutdownCommand(
        interaction: JDAInteraction,
        @Argument("test", description = "The shutdown is for a test and does not announce the shutdown to servers")
        test: Boolean = true
    ) {
        val event = interaction.interactionEvent() ?: return
        event.reply("Shutting down...").queue()
        if(!test){
            transaction(bot.db) {
                GuildTable.select(GuildTable.channel, GuildTable.id).forEach {
                    if (it[GuildTable.channel] == bot.testingChannel!!.toLong())
                        return@forEach

                    if (it[GuildTable.channel] == null) {
                        return@forEach
                    }

                    bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage("Bot shutting down...").queue()
                }
            }
        }
        bot.shutdown()
    }
}