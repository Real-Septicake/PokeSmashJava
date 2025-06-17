@file:Suppress("unused")

package io.github.septicake.commands

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.*
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.WhitelistEntity
import io.github.septicake.db.WhitelistTable
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotation.specifier.Range
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default

class ModCommands(
    private val bot: PokeSmashBot,
) {
    @GuildOnly
    @UserPermissions(guildOwnerOnly = true)
    @Command("whitelist add <user>")
    @CommandParams("user")
    @ProperName("Whitelist add")
    @CommandDescription("Adds a user to the server's whitelist")
    @LongDescription("Adds a user to this server's whitelist, allowing them to use several commands " +
            "not available to most users")
    @Category(CategoryEnum.MANAGEMENT)
    suspend fun whitelistAddCommand(
        interaction: JDAInteraction,
        @Argument("user", description = "User to whitelist")
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
    @CommandParams("user")
    @ProperName("Whitelist remove")
    @CommandDescription("Removes a user from the server's whitelist")
    @LongDescription("Removes a user from this server's whitelist, preventing them from using some commands")
    @Category(CategoryEnum.MANAGEMENT)
    suspend fun whitelistRemoveCommand(
        interaction: JDAInteraction,
        @Argument("user", description = "User to remove from whitelist")
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
    @CommandParams("channel")
    @ProperName("Set channel")
    @CommandDescription("Sets the channel for this server")
    @LongDescription("Sets the channel that announcements are sent to, and where channel-locked commands " +
            "can be used")
    @Category(CategoryEnum.SETUP)
    suspend fun setChannel(
        interaction: JDAInteraction,
        @Argument("channel", description = "Channel for channel-locked commands, and where announcements will be sent to")
        channel: Channel
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).await()
        if (channel is StandardGuildMessageChannel) {
            if(!channel.isNSFW) {
                event.hook.sendMessage("Channel must be marked as nsfw").queue()
                return
            }
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
    @Command("setup <channel> [polls]")
    @CommandParams("channel", "polls")
    @ProperName("Setup")
    @CommandDescription("Sets up the server info")
    @LongDescription("Sets up the server info, setting the channel as well as the number of polls " +
            "sent by calls to `/next`")
    @Category(CategoryEnum.SETUP)
    suspend fun populateCommand(
        interaction: JDAInteraction,
        @Argument("channel", description = "Channel for `next` to be called in, and where announcements will be sent to")
        channel: Channel,
        @Argument("polls", description = "Number of polls per call of `next`, defaults to 5")
        @Range(min = "1", max = "10")
        @Default("5")
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
            if (channel !is StandardGuildMessageChannel) {
                event.hook.sendMessage("Channel must be a message channel").await()
                return
            }
            if(!channel.isNSFW) {
                event.hook.sendMessage("Channel must be marked as nsfw").queue()
                return
            }
            transaction(bot.db) {
                GuildEntity.new(event.guild!!.idLong) {
                    this.name = event.guild!!.name
                    this.channel = channel.idLong
                    this.polls = polls
                }
            }
            event.hook.sendMessage("Server populated successfully. you can now call `/next` in the specified channel").await()
        }
    }

    @GuildOnly
    @BlacklistSensitive
    @UserPermissions(whitelistOnly = true)
    @Command("message <text>")
    @CommandParams("text")
    @ProperName("Message")
    @CommandDescription("Send a message to the bot")
    @LongDescription("Send a message to the bot owner, abuse of this will result in a blacklist")
    @Category(CategoryEnum.MANAGEMENT)
    suspend fun messageCommand(
        interaction: JDAInteraction,
        @Argument("text", description = "Message to send")
        @Greedy
        text: String,
    ) {
        val event = interaction.interactionEvent() ?: return
        val guild = event.guild!!
        event.deferReply().setEphemeral(true).await()
        bot.jda.getTextChannelById(bot.replyChannel)!!.sendMessage(MessageCreate {
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

                thumbnail = event.user.avatarUrl

                footer { name = "ID: ${event.user.id}" }
            }
        }).await()
        event.hook.sendMessage("Message sent").await()
    }
}
