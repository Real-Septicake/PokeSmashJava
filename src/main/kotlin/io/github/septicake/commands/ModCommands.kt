package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.GuildOnly
import io.github.septicake.cloud.annotations.UserPermissions
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.WhitelistEntity
import io.github.septicake.db.WhitelistTable
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class ModCommands(
    private val bot : PokeSmashBot
) {

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("whitelist add <user>")
    fun whitelistAddCommand(
        interaction: JDAInteraction,
        @Argument("user")
        user: User
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(bot.userServerWhitelisted(event.guild!!.idLong, user.idLong)) {
            event.hook.sendMessage("<@${user.id}> is already whitelisted.").mention().queue()
            return
        }
        transaction(bot.db) {
            WhitelistEntity.new {
                guild = event.guild!!.idLong
                this.user = user.idLong
            }
        }
        event.hook.sendMessage("<@${user.id}> successfully added to whitelist.").mention().queue()
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("whitelist remove <user>")
    fun whitelistRemoveCommand(
        interaction: JDAInteraction,
        @Argument("user")
        user: User
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(!bot.userServerWhitelisted(event.guild!!.idLong, user.idLong)) {
            event.hook.sendMessage("<@${user.id}> is not whitelisted.").mention().queue()
            return
        }
        transaction(bot.db) {
            WhitelistEntity.find {
                WhitelistTable.guild eq event.guild!!.idLong and (WhitelistTable.user eq user.idLong)
            }.elementAt(0).delete()
        }
        event.hook.sendMessage("<@${user.id}> successfully removed from whitelist.").mention().queue()
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("set channel <channel>")
    fun setChannel(
        interaction: JDAInteraction,
        @Argument("channel")
        channel: Channel
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(channel is MessageChannel) {
            val info = transaction(bot.db) {
                GuildEntity.findById(interaction.guild()!!.idLong)
            }
            if(info == null) {
                event.hook.sendMessage("Server has not been populated yet.").queue()
                return
            } else {
                transaction(bot.db) {
                    info.channel = channel.idLong
                }
                event.hook.sendMessage("Channel successfully set to <#${channel.id}>").queue()
                return
            }
        } else {
            event.hook.sendMessage("Channel must be a message channel").queue()
        }
    }
}