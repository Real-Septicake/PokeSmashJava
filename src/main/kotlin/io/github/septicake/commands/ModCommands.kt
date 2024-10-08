package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.*
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.WhitelistEntity
import io.github.septicake.db.WhitelistTable
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotation.specifier.Range
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
    @CommandName("Add Whitelist")
    @CommandParams("user")
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
        event.hook.sendMessage("<@${user.id}> successfully added to whitelist.").queue()
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("whitelist remove <user>")
    @CommandName("Remove Whitelist")
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
        event.hook.sendMessage("<@${user.id}> successfully removed from whitelist.").queue()
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("set channel <channel>")
    @CommandName("Set Channel")
    fun setChannel(
        interaction: JDAInteraction,
        @Argument("channel")
        channel: Channel
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        if(channel is MessageChannel) {
            val info = transaction(bot.db) {
                GuildEntity.findById(event.guild!!.idLong)
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

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("populate <channel> [polls]")
    @CommandName("Populate")
    fun populateCommand(
        interaction: JDAInteraction,
        @Argument("channel", description = "Channel for `next` to be called in, and where announcements will be sent to")
        channel: Channel,
        @Argument("polls", description = "Number of polls per call of `next`, defaults to 5")
        @Range(min = "1", max = "10")
        polls: Int = 5
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val info = transaction(bot.db) {
            GuildEntity.findById(interaction.guild()!!.idLong)
        }

        if(info != null) {
            event.hook.sendMessage("Server has already been populated").queue()
        } else {
            if(channel !is MessageChannel) {
                event.hook.sendMessage("Channel must be a message channel").queue()
                return
            }
            transaction(bot.db) {
                GuildEntity.new {
                    this.id._value = event.guild!!.idLong
                    this.name = event.guild!!.name
                    this.channel = channel.idLong
                    this.polls = polls
                }
            }
            event.hook.sendMessage("Server populated successfully.").queue()
        }
    }

    @GuildOnly
    @UserPermissions(whitelistOnly = true)
    @Command("reply <msg>")
    @CommandName("Reply")
    fun replyCommand(
        interaction: JDAInteraction,
        @Argument("msg")
        @Greedy
        msg: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        bot.jda.getTextChannelById(bot.replyChannel!!.toLong())!!.sendMessage(
            "User `${event.user.name}` from server `${event.guild!!.name}` sent: $msg"
        ).queue()
        event.hook.sendMessage("Message sent.").queue()
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("set poll <pokemon> <smashes> <passes>")
    @CommandName("Set Poll")
    fun addPollCommand(
        interaction: JDAInteraction,
        @Argument("pokemon")
        @Pokemon
        pokemon: String,
        @Argument("smashes")
        smashes: Long,
        @Argument("passes")
        passes: Long
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val pokemonId = pokemon.toIntOrNull() ?: bot.map.inverse()[pokemon.lowercase()]!!
        try {
            bot.setPollResults(event.guild!!.idLong, pokemonId, smashes, passes)
            event.hook.sendMessage("Poll result set.").queue()
        } catch(e: PokeSmashBot.ServerNotPopulatedException) {
            event.hook.sendMessage("Server has not been populated yet.").queue()
        }
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("remove poll <pokemon>")
    @CommandName("Remove Poll")
    fun removePollCommand(
        interaction: JDAInteraction,
        @Argument("pokemon")
        @Pokemon
        pokemon: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val pokemonId = pokemon.toIntOrNull() ?: bot.map.inverse()[pokemon.lowercase()]!!
        try {
            bot.removePollResults(event.guild!!.idLong, pokemonId)
            event.hook.sendMessage("Poll result removed.").queue()
        } catch(e: PokeSmashBot.ServerNotPopulatedException) {
            event.hook.sendMessage("Server has not been populated yet.").queue()
        } catch(e: PokeSmashBot.PollDoesNotExistException) {
            event.hook.sendMessage("Poll does not exist.").queue()
        }
    }
}