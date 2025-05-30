@file:Suppress("unused")

package io.github.septicake.commands

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.BlacklistSensitive
import io.github.septicake.cloud.annotations.CommandParams
import io.github.septicake.cloud.annotations.GuildOnly
import io.github.septicake.cloud.annotations.UserPermissions
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.WhitelistEntity
import io.github.septicake.db.WhitelistTable
import io.github.septicake.pokeapi.PokemonInfo
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
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant

class ModCommands(
    private val bot: PokeSmashBot,
) {
    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("whitelist add <user>")
    @CommandParams("user")
    suspend fun whitelistAddCommand(
        interaction: JDAInteraction,
        @Argument("user")
        user: User,
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).await()
        if (bot.userServerWhitelisted(event.guild!!.idLong, user.idLong)) {
            event.hook.sendMessage("${user.asMention} is already whitelisted.").mention().await()
            return
        }
        transaction(bot.db) {
            WhitelistEntity.new {
                guild = event.guild!!.idLong
                this.user = user.idLong
            }
        }

        event.hook.sendMessage("${user.asMention} successfully added to whitelist.").await()
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("whitelist remove <user>")
    suspend fun whitelistRemoveCommand(
        interaction: JDAInteraction,
        @Argument("user")
        user: User,
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).await()
        if (!bot.userServerWhitelisted(event.guild!!.idLong, user.idLong)) {
            event.hook.sendMessage("${user.asMention} is not whitelisted.").mention().await()
            return
        }
        transaction(bot.db) {
            WhitelistEntity.find {
                WhitelistTable.guild eq event.guild!!.idLong and (WhitelistTable.user eq user.idLong)
            }.elementAt(0).delete()
        }
        event.hook.sendMessage("${user.asMention} successfully removed from whitelist.").await()
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("set channel <channel>")
    suspend fun setChannel(
        interaction: JDAInteraction,
        @Argument("channel")
        channel: Channel,
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).await()
        if (channel is MessageChannel) {
            val info = transaction(bot.db) {
                GuildEntity.findById(event.guild!!.idLong)
            }
            if (info == null) {
                event.hook.sendMessage("Server has not been populated yet.").await()
                return
            } else {
                transaction(bot.db) {
                    info.channel = channel.idLong
                }
                event.hook.sendMessage("Channel successfully set to <#${channel.id}>").await()
                return
            }
        } else {
            event.hook.sendMessage("Channel must be a message channel").await()
        }
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("populate <channel> [polls]")
    suspend fun populateCommand(
        interaction: JDAInteraction,
        @Argument("channel", description = "Channel for `next` to be called in, and where announcements will be sent to")
        channel: Channel,
        @Argument("polls", description = "Number of polls per call of `next`, defaults to 5")
        @Range(min = "1", max = "10")
        polls: Int = 5,
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).await()
        val info = transaction(bot.db) {
            GuildEntity.findById(interaction.guild()!!.idLong)
        }

        if (info != null) {
            event.hook.sendMessage("Server has already been populated").await()
        } else {
            if (channel !is MessageChannel) {
                event.hook.sendMessage("Channel must be a message channel").await()
                return
            }
            transaction(bot.db) {
                GuildEntity.new(event.guild!!.idLong) {
                    this.name = event.guild!!.name
                    this.channel = channel.idLong
                    this.polls = polls
                }
            }
            event.hook.sendMessage("Server populated successfully.").await()
        }
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("set poll <pokemon> <smashes> <passes>")
    suspend fun addPollCommand(
        interaction: JDAInteraction,
        @Argument("pokemon")
        pokemon: PokemonInfo,
        @Argument("smashes")
        smashes: Long,
        @Argument("passes")
        passes: Long,
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).await()
        try {
            bot.setPollResults(event.guild!!.idLong, pokemon.id, smashes, passes)
            event.hook.sendMessage("Poll result set.").await()
        } catch (e: PokeSmashBot.ServerNotPopulatedException) {
            event.hook.sendMessage("Server has not been populated yet.").await()
        }
    }

    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("remove poll <pokemon>")
    suspend fun removePollCommand(
        interaction: JDAInteraction,
        @Argument("pokemon")
        pokemon: PokemonInfo,
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).await()
        try {
            bot.removePollResults(event.guild!!.idLong, pokemon.id)
            event.hook.sendMessage("Poll result removed.").await()
        } catch (e: PokeSmashBot.ServerNotPopulatedException) {
            event.hook.sendMessage("Server has not been populated yet.").await()
        } catch (e: PokeSmashBot.PollDoesNotExistException) {
            event.hook.sendMessage("Poll does not exist.").await()
        }
    }

    @GuildOnly
    @BlacklistSensitive
    @UserPermissions(whitelistOnly = true)
    @Command("message <text>")
    suspend fun messageCommand(
        interaction: JDAInteraction,
        @Argument("text")
        @Greedy
        text: String,
    ) {
        val event = interaction.interactionEvent() ?: return
        val guild = event.guild!!
        event.deferReply().setEphemeral(true).await()
        bot.jda.getTextChannelById(bot.replyChannel!!)!!.sendMessage(MessageCreate {
            embed {
                title = "Message from ${interaction.user().effectiveName}"
                description = text
                timestamp = Clock.System.now().toJavaInstant()
                field("From", interaction.user().name)
                field("Server", guild.name)
                field() // alignment
                field("Status", if (event.user.idLong == guild.ownerIdLong) "Owner" else "Whitelisted")
                field("Server ID", guild.id)
                field() // also alignment

                footer { name = "ID: ${interaction.user().id}" }
            }
        }).await()
        event.hook.sendMessage("Message sent").await()
    }
}
