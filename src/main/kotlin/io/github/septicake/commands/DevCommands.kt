@file:Suppress("unused")

package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.*
import io.github.septicake.db.BlacklistEntity
import io.github.septicake.db.GuildEntity
import io.github.septicake.db.GuildTable
import io.github.septicake.db.WhitelistTable
import io.github.septicake.util.sendMessage
import io.github.septicake.util.toDiscordTimestamp
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.error
import org.slf4j.kotlin.getLogger

@Category(CategoryEnum.DEV)
class DevCommands(
    private val bot: PokeSmashBot
) {
    val logger by getLogger()

    @Command("allow commands <val>")
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("val")
    @CommandDescription("Allow or disallow certain commands")
    @ProperName("Allow commands")
    @LongDescription("Honest to god this doesn't do sh\\*t")
    fun commandToggleCommand(
        interaction: JDAInteraction,
        @Argument("val", description = "Whether polls should be enabled")
        value: Boolean
    ) {
        val event = interaction.interactionEvent() ?: return
        bot.commandsEnabled = value
        event.reply("Commands ${if (value) "enabled" else "disabled"}.").queue()
    }

    @Command("whitelist strip <user>")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandDescription("Strips user of whitelist permissions")
    @ProperName("Strip whitelist")
    @LongDescription(
        "Strips a user of their whitelist privilege in all servers they are whitelisted in.\n" +
                "This does not override the whitelist privileges given by guild owners."
    )
    fun whitelistStripCommand(
        interaction: JDAInteraction,
        @Argument("user", description = "User to strip from whitelist")
        user: Long
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        transaction(bot.db) {
            WhitelistTable.deleteWhere {
                this.user eq user
            }
        }

        event.hook.sendMessage("User $user has been purged from whitelist").queue()
    }

    @Command("msg <server> <msg>")
    @CommandDescription("Messages a server's channel")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("server", "msg")
    @ProperName("Message Server")
    @LongDescription("Sends a message to the server's channel")
    fun messageCommand(
        interaction: JDAInteraction,
        @Argument("server", description = "Server to message")
        server: Long,
        @Argument("msg", description = "Message to send")
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
                event.reply("Server has not set a channel yet.").queue()
            }
        }
    }

    @Command("announce <msg>")
    @CommandDescription("Announces the message to all servers")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("msg")
    @ProperName("Announce")
    @LongDescription(
        "Sends an announcement to all servers, the sequence `%owner` can be used to mention" +
                " the server owner. Some further information is returned as a response to the original command invocation."
    )
    fun announceCommand(
        interaction: JDAInteraction,
        @Argument("msg", description = "Message to announce")
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
                        val guild = bot.jda.getGuildById(it[GuildTable.id].value) ?: return@forEach
                        val formattedStr = msg.replace("%owner%", "<@${guild.ownerId}>")
                        bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage(formattedStr).queue()
                    } else
                        bot.jda.getTextChannelById(it[GuildTable.channel]!!)!!.sendMessage(msg).queue()
                } catch (e: InsufficientPermissionException) {
                    logger.error { "Could not send message to ${it[GuildTable.name]}, lacking permissions" }
                    lackPerms++
                } catch (e: NullPointerException) {
                    logger.error { "Could not send message to ${it[GuildTable.name]}, channel was deleted" }
                    noChannel++
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

    @Command("warn <user> <reason>")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("user", "reason")
    @ProperName("Warn")
    @CommandDescription("Warns a user for the specified reason")
    @LongDescription(
        "Sends a DM to the specified user warning them for the specified reason, no other " +
                "actions are taken by this command"
    )
    fun warnCommand(
        interaction: JDAInteraction,
        @Argument("user", description = "User to warn")
        user: Long,
        @Argument("reason", description = "Reason for warning")
        @Greedy
        reason: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        bot.openDM(user, {
            it.sendMessage("# Warning Issued\nReason: $reason").queue()
            event.hook.sendMessage("Warning issued to user \"$user\"").queue()
        }, { event.hook.sendMessage("User does not exist").queue() })
    }

    @Command("blacklist add <user> <reason>")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("user", "reason")
    @CommandDescription("Adds a user to the blacklist")
    @ProperName("Blacklist add")
    @LongDescription(
        "Adds a user to the blacklist, preventing them from using certain commands. " +
                "The user is alerted of the fact that they have been blacklisted"
    )
    fun blacklistAddCommand(
        interaction: JDAInteraction,
        @Argument("user", description = "User to blacklist")
        user: Long,
        @Argument("reason", description = "Reason for blacklist")
        @Greedy
        @LengthMax(255)
        reason: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        bot.openDM(user, { channel ->
            val userEntity = bot.userBlacklisted(user)
            if (userEntity != null) {
                event.hook.sendMessage("User \"$user\" already blacklisted for `${userEntity.reason}`").queue()
                return@openDM
            }

            transaction(bot.db) {
                BlacklistEntity.new(user) {
                    this.reason = reason
                    this.since = Clock.System.now()
                }
            }
            event.hook.sendMessage("User \"$user\" has been blacklisted for `$reason`").queue()

            channel.sendMessage("You have been blacklisted for `$reason`").queue()
        }, { event.hook.sendMessage("User does not exist").queue() })
    }

    @Command("blacklist remove <user>")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("user")
    @CommandDescription("Removes a user from the blacklist")
    @ProperName("Blacklist remove")
    @LongDescription(
        "Removes a user from the blacklist, allowing them to use \"blacklist sensitive\" " +
                "commands again. The user is alerted that they were removed from the blacklist"
    )
    fun blacklistRemoveCommand(
        interaction: JDAInteraction,
        @Argument("user", description = "User to remove from blacklist")
        user: Long
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        bot.openDM(user, { channel ->
            val userEntity = bot.userBlacklisted(user)
            if (userEntity == null) {
                event.hook.sendMessage("User \"$user\" is not blacklisted").queue()
                return@openDM
            }

            transaction(bot.db) { userEntity.delete() }
            event.hook.sendMessage("User \"$user\" has been removed from blacklist").queue()

            channel.sendMessage("You have been removed from the blacklist").queue()
        }, { event.hook.sendMessage("User does not exist").queue() })
    }

    @Command("blacklist info <user>")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("user")
    @CommandDescription("Retrieves the info on a user's blacklisting")
    @ProperName("Blacklist info")
    @LongDescription("Retrieves info on a user's blacklisting, being why they were blacklisted and when")
    suspend fun blacklistInfoCommand(
        interaction: JDAInteraction,
        @Argument("user", description = "User to query blacklist info of")
        user: Long
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        val entity = bot.userBlacklisted(user)
        if (entity == null) {
            event.hook.sendMessage("User \"$user\" is not blacklisted").queue()
            return
        }

        event.hook.sendMessage {
            embed {
                title = "Blacklisted User: $user"
                description = entity.reason
                timestamp = Clock.System.now().toJavaInstant()

                field("since", entity.since.toDiscordTimestamp())
            }
        }
    }

    @Command("servers")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandDescription("Gets the number of servers the bot is in")
    @ProperName("Servers")
    @LongDescription("Shows the number of servers the bot is in, no further information is given")
    fun serverCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.reply("The bot is in ${bot.jda.selfUser.mutualGuilds.size} servers").queue()
    }

    @Command("shutdown [test]")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("test")
    @CommandDescription("Shuts down the bot")
    @ProperName("Shutdown")
    @LongDescription("Shuts down the bot, it is announced if `test` is not set to true")
    suspend fun shutdownCommand(
        interaction: JDAInteraction,
        @Argument("test", description = "The shutdown is for a test and does not announce the shutdown to servers")
        @Default("true")
        test: Boolean = true
    ) {
        val event = interaction.interactionEvent() ?: return
        event.reply("Shutting down...").queue()
        if (!test) {
            transaction(bot.db) {
                GuildTable.select(GuildTable.channel, GuildTable.id, GuildTable.name).forEach {
                    if (it[GuildTable.channel] == bot.testingChannel)
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

    @Command("alert <msg>")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @CommandParams("msg")
    @CommandDescription("Alert guild owners")
    @ProperName("Alert")
    @LongDescription("Send a message to the owners of all guilds the bot is in")
    fun alertCommand(
        interaction: JDAInteraction,
        @Argument("msg", description = "The message to send to guild owners")
        @Greedy
        msg: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        var total = 0
        bot.jda.selfUser.mutualGuilds.forEach { guild ->
            bot.openDM(guild.ownerIdLong, {
                it.sendMessage(msg).queue()
                total++
            }, {})
        }
        event.hook.sendMessage("Alert sent to $total guild owners").queue()
    }
}