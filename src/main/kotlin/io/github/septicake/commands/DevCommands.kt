package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.db.GuildEntity
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.transactions.transaction

class DevCommands(
    private val bot : PokeSmashBot
) {

    @Command("message <server> <msg>")
    @CommandDescription("Send a message to a server's channel. DEV COMMAND, CANNOT BE USED BY NORMAL USERS")
    fun messageCommand(
        interaction: JDAInteraction,
        @Argument("server")
        server: Long,
        @Argument("msg")
        @Greedy
        str: String
    ) {
        val event = interaction.interactionEvent() ?: return
        if(event.channel!!.id != bot.testingChannel) {
            event.reply("Command cannot be used in this channel.").queue()
        } else {
            val info = transaction(bot.db) {
                GuildEntity.findById(server)
            }
            if(info == null) {
                event.reply("Server `$server` either does not exist or has not been populated.").queue()
            } else {
                if(info.channel != null){
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
    }
}