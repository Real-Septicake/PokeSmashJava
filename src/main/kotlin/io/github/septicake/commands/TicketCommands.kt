@file:Suppress("unused")

package io.github.septicake.commands

import dev.minn.jda.ktx.coroutines.await
import io.github.septicake.PokeSmashBot
import io.github.septicake.PokeSmashConstants
import io.github.septicake.cloud.annotations.BlacklistSensitive
import io.github.septicake.cloud.annotations.LengthMax
import io.github.septicake.cloud.annotations.PrivateOnly
import io.github.septicake.cloud.annotations.UserPermissions
import io.github.septicake.db.TicketEntity
import io.github.septicake.db.TicketIncludeEntity
import io.github.septicake.db.TicketIncludeTable
import io.github.septicake.db.TicketTable
import io.github.septicake.util.ticketEmbed
import kotlinx.datetime.Clock
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.error
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.warn

class TicketCommands(
    private val bot: PokeSmashBot
) {
    private val logger by getLogger()

    val logNonexistentUser = { user: Long, ticket: Int ->
        logger.error {
            "User $user no longer exists, related to ticket $ticket"
        }
    }

    @Command("ticket open <topic>")
    @PrivateOnly
    @BlacklistSensitive
    suspend fun ticketOpenCommand(
        interaction: JDAInteraction,
        @Argument("topic")
        @Greedy
        @LengthMax(75)
        topic: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        val userTicketInfo = bot.userTicketEntity(event.user.idLong)
        if (userTicketInfo.openTickets == userTicketInfo.maxOpenTickets) {
            event.hook.sendMessage("You already have the maximum number of tickets open").queue()
            return
        }

        val (ticket, thread) = bot.openTicket(event.user.idLong, topic)
        val id = ticket.id.value
        thread.ticketEmbed(
            id,
            "Thread $id Opened",
            topic,
            Pair("Opened By", event.user.effectiveName),
            Pair("ID", event.user.id),
            image = event.user.avatarUrl,
            timestamp = ticket.lastActive,
            color = 0x15C132,
        )

        // author is guaranteed to be the only person included, no need to use `messageTicketIncludes`
        event.hook.ticketEmbed(
            id,
            "Thread $id Opened",
            topic,
            timestamp = ticket.lastActive,
            color = 0x15C132,
        )
    }

    @Command("ticket include <id> <user>")
    @PrivateOnly
    suspend fun ticketIncludeCommand(
        interaction: JDAInteraction,
        @Argument("id")
        id: Int,
        @Argument("user")
        user: Long
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()

        val (ticket, thread) = bot.getTicket(id)
        if (ticket == null || thread == null) {
            event.hook.sendMessage(
                "Could not message ticket, " +
                        "please double check the DM history to make sure this is the correct ID " +
                        "and that the ticket is still open"
            ).queue()
            return
        }
        if (ticket.author != event.user.idLong && PokeSmashConstants.ownerId != event.user.idLong) {
            event.hook.sendMessage("This is not your ticket, you cannot include a user").queue()
            logger.warn { "User ${event.user.effectiveName} tried to include user $user in ticket ${ticket.id.value}" }
            return
        }
        if (bot.userBlacklisted(user) != null) {
            event.hook.sendMessage("User is blacklisted and cannot be included in the ticket").queue()
            logger.warn { "Attempted add of blacklisted user $user to ticket ${ticket.id.value}" }
            return
        }
        if (bot.includedInTicket(ticket.id.value, user)) {
            event.hook.sendMessage("User is already included in the ticket").queue()
            return
        }

        val name = if (PokeSmashConstants.ownerId == event.user.idLong) "owner" else event.user.effectiveName
        val username = try {
            bot.jda.retrieveUserById(user).await().effectiveName
        } catch(_: Throwable) {
            "[Unknown]" // should never be seen
        }

        bot.openDM(user, {
            val now = Clock.System.now()

            transaction(bot.db) {
                ticket.lastActive = now // only update if user exists
            }

            it.ticketEmbed(
                ticket.id.value, "Included in " + ticket.topic,
                "You've been included in ticket ${ticket.id.value} by $name.",
                Pair("To Leave", "`/ticket leave ${ticket.id.value}`"), timestamp = now
            )

            bot.messageTicketIncludes(ticket.id.value, { userId, channel ->
                if (userId == user) return@messageTicketIncludes
                channel.ticketEmbed(
                    ticket.id.value, "New included user in " + ticket.topic,
                    "User $username has been included in the ticket by $name",
                    timestamp = now
                )
            }, { user, _ -> logNonexistentUser(user, ticket.id.value) })

            thread.ticketEmbed(
                ticket.id.value, "User included in ticket",
                "User $username has been included in this ticket by ${event.user.effectiveName}",
                Pair("New User ID", user.toString()),
                timestamp = now
            )

            event.hook.sendMessage("User included").queue()
        }, { event.hook.sendMessage("User does not exist").queue() })
    }

    @Command("ticket leave <id> [silent]")
    @PrivateOnly
    fun ticketLeaveCommand(
        interaction: JDAInteraction,
        @Argument("id")
        id: Int,
        @Argument("silent")
        silent: Boolean = false
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()

        if (!bot.includedInTicket(id, event.user.idLong)) {
            event.hook.sendMessage(
                "You are not included in the specified ticket, " +
                        "double check the DM history to see if you were already unincluded or if the ticket was closed"
            ).queue()
            return
        }

        val (ticket, thread) = bot.getTicket(id)
        if (ticket == null || thread == null) {
            // the ticket not existing should already be caught by the if statement above
            event.hook.sendMessage("Specified ticket does not exist").queue()
            return
        }
        if (event.user.idLong == ticket.author) {
            event.hook.sendMessage("You are the author of this ticket, you cannot leave").queue()
            return
        }

        val now = Clock.System.now()

        transaction(bot.db) {
            ticket.lastActive = now
        }

        thread.ticketEmbed(
            ticket.id.value, "User left ticket", "User ${event.user.effectiveName} has left the ticket",
            Pair("Silent", if (silent) "Yes" else "No"), timestamp = now
        )

        transaction(bot.db) {
            TicketIncludeEntity.find { TicketIncludeTable.user eq event.user.idLong and (TicketIncludeTable.ticket eq id) }
                .singleOrNull()?.delete()
        }

        if (!silent) {
            bot.messageTicketIncludes(ticket.id.value, { _, channel ->
                channel.ticketEmbed(
                    ticket.id.value, "User left ticket",
                    "User ${event.user.effectiveName} has left the ticket",
                    timestamp = now
                )
            }, { user, _ -> logNonexistentUser(user, ticket.id.value) })
        }

        bot.openDM(event.user.idLong, {
            it.ticketEmbed(
                ticket.id.value, "You have left the ticket",
                "You left ticket ${ticket.id.value}${if (silent) " silently" else ""}",
                timestamp = now
            )
        }, {})
    }

    @Command("ticket uninclude <id> <user>")
    @PrivateOnly
    suspend fun ticketUnincludeCommand(
        interaction: JDAInteraction,
        @Argument("id")
        id: Int,
        @Argument("user")
        user: Long
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()

        val (ticket, thread) = bot.getTicket(id)
        if (ticket == null || thread == null) {
            event.hook.sendMessage(
                "Could not find ticket, " +
                        "please double check the DM history to make sure this is the correct ID " +
                        "and that the ticket is still open"
            ).queue()
            return
        }
        if (ticket.author != event.user.idLong && PokeSmashConstants.ownerId != event.user.idLong) {
            event.hook.sendMessage("This is not your ticket, you cannot uninclude users").queue()
            logger.warn { "User ${event.user.effectiveName} tried to uninclude user $user in ticket ${ticket.id.value}" }
            return
        }
        if (ticket.author == user) {
            event.hook.sendMessage("You cannot uninclude yourself").queue()
            return
        }
        if (!bot.includedInTicket(ticket.id.value, user)) {
            event.hook.sendMessage("User is not included in this ticket").queue()
            return
        }

        val now = Clock.System.now()
        val name = if (PokeSmashConstants.ownerId == event.user.idLong) "owner" else event.user.effectiveName

        transaction(bot.db) {
            TicketIncludeEntity.find { TicketIncludeTable.user eq user and (TicketIncludeTable.ticket eq id) }
                .singleOrNull()?.delete()

            ticket.lastActive = now
        }

        val username = try {
            bot.jda.retrieveUserById(user).await().effectiveName
        } catch (_: Throwable) {
            logNonexistentUser(user, ticket.id.value)
            "[Unknown]"
        }

        bot.openDM(user, {
            it.ticketEmbed(
                ticket.id.value, "Unincluded from ticket",
                "You have been unincluded from the ticket", timestamp = now
            )
        }, {})

        bot.messageTicketIncludes(ticket.id.value, { userId, channel ->
            channel.ticketEmbed(
                ticket.id.value, "User unincluded from ticket",
                "User $username has been unincluded from this ticket by ${if (userId != event.user.idLong) name else "you"}",
                timestamp = now
            )
        }, { userId, _ -> logNonexistentUser(userId, ticket.id.value) })

        thread.ticketEmbed(
            ticket.id.value, "User unincluded from ticket",
            "User $username has been unincluded from this ticket by ${event.user.effectiveName}",
            Pair("Unincluded User ID", user.toString()),
            timestamp = now
        )

        event.hook.sendMessage("User unincluded").queue()
    }

    @Command("ticket message <id> <message>")
    @PrivateOnly
    fun ticketMessageCommand(
        interaction: JDAInteraction,
        @Argument("id")
        id: Int,
        @Argument("message")
        @Greedy
        message: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()

        val (ticket, thread) = bot.getTicket(id)
        if (ticket == null || thread == null) {
            event.hook.sendMessage(
                "Could not message ticket, " +
                        "please double check the DM history to make sure this is the correct ID " +
                        "and that the ticket is still open"
            ).queue()
            return
        }
        if (!bot.includedInTicket(ticket.id.value, event.user.idLong)) {
            event.hook.sendMessage("This is not your ticket").queue()
            logger.warn { "User ${event.user.effectiveName} attempted to message ticket ${ticket.id.value}" }
            return
        }

        val now = Clock.System.now()

        transaction(bot.db) {
            ticket.lastActive = now
        }

        thread.ticketEmbed(
            id, ticket.topic, message,
            Pair("Message from", event.user.effectiveName),
            Pair("ID", event.user.id),
            timestamp = now, image = event.user.avatarUrl
        )

        bot.messageTicketIncludes(ticket.id.value, { user, channel ->
            channel.ticketEmbed(
                ticket.id.value, ticket.topic + " Messaged", message,
                Pair("From", if (user == event.user.idLong) "You" else event.user.effectiveName),
                timestamp = now, image = event.user.avatarUrl
            )
        }, { user, _ -> logNonexistentUser(user, ticket.id.value) })

        event.hook.sendMessage("Message sent").queue()
    }

    @Command("ticket reply <message>")
    @UserPermissions(botOwnerOnly = true)
    fun ticketReplyCommand(
        interaction: JDAInteraction,
        @Argument("message")
        @Greedy
        message: String
    ) {
        val event = interaction.interactionEvent() ?: return

        if (event.channelType != ChannelType.GUILD_PUBLIC_THREAD) {
            event.reply("Command must be used in a ticket thread").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        val thread = event.channel!! as ThreadChannel
        val ticket = transaction(bot.db) { TicketEntity.find { TicketTable.thread eq thread.idLong }.singleOrNull() }

        if (ticket == null) {
            event.hook.sendMessage("Current thread is not attributed to an open issue").queue()
            return
        }

        val now = Clock.System.now()

        transaction(bot.db) {
            ticket.lastActive = now
        }

        event.hook.ticketEmbed(ticket.id.value, null, message, timestamp = now)

        bot.messageTicketIncludes(ticket.id.value, { _, channel ->
            channel.ticketEmbed(
                ticket.id.value,
                "Reply to " + ticket.topic,
                message,
                timestamp = now,
                image = bot.jda.selfUser.avatarUrl
            )
        }, { user, _ -> logNonexistentUser(user, ticket.id.value) })
    }

    @Command("ticket included <id>")
    @PrivateOnly
    fun ticketIncludedCommand(
        interaction: JDAInteraction,
        @Argument("id")
        id: Int
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()

        if (!bot.includedInTicket(id, event.user.idLong) && event.user.idLong != PokeSmashConstants.ownerId) {
            event.hook.sendMessage("This is not your ticket, are you sure you used the correct ID?").queue()
            return
        }

        if(event.user.idLong != PokeSmashConstants.ownerId && event.channelType != ChannelType.PRIVATE) {
            event.hook.sendMessage("Command cannot be used here").queue()
            return
        }

        var response = "[Error obtaining included users]"

        transaction(bot.db) {
            val included = mutableListOf<Long>()
            val ticket = TicketEntity.findById(id)!!
            included += ticket.author
            TicketIncludeEntity.find { TicketIncludeTable.ticket eq id }
                .forEach {
                    included += it.user
                }

            response = included.fold("Users included in ticket $id:") { acc, l ->
                val username =
                    try {
                        bot.jda.retrieveUserById(l).complete().effectiveName
                    } catch (e: Throwable) {
                        logNonexistentUser(l, ticket.id.value)
                        "[Unknown]"
                    }
                "$acc\n$username${if (event.user.idLong == l) " [You]" else ""}"
            }
        }

        (event.channel!! as PrivateChannel).sendMessage(response).queue()
        event.hook.sendMessage("Included users sent").queue()
    }

    @Command("ticket close <id> <reason>")
    fun ticketCloseCommand(
        interaction: JDAInteraction,
        @Argument("id")
        id: Int,
        @Argument("reason")
        @Greedy
        reason: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()

        val (ticket, thread) = bot.getTicket(id)
        if (ticket == null || thread == null) {
            event.hook.sendMessage(
                "Could not close ticket, " +
                        "please double check the DM history to make sure this is the correct ID " +
                        "and that the ticket is still open"
            ).queue()
            return
        }

        val image = when (event.user.idLong) {
            ticket.author -> event.user.avatarUrl
            PokeSmashConstants.ownerId -> bot.jda.selfUser.avatarUrl
            else -> {
                event.hook.sendMessage("This is not your ticket").queue()
                logger.warn { "User ${event.user.effectiveName} attempted to close ticket ${ticket.id.value}" }
                return
            }
        }

        val name = if (event.user.idLong == PokeSmashConstants.ownerId) "the owner" else null

        val now = Clock.System.now()

        transaction(bot.db) {
            ticket.lastActive = now
        }

        thread.ticketEmbed(
            ticket.id.value,
            ticket.topic + " Closed",
            "Ticket closed by ${name ?: event.user.effectiveName} for `$reason`",
            color = 0xE53714,
            image = image,
            timestamp = now
        )

        bot.messageTicketIncludes(ticket.id.value, { user, channel ->
            channel.ticketEmbed(
                ticket.id.value,
                ticket.topic + " Closed",
                "Ticket closed by ${name ?: if (user == event.user.idLong) "you" else event.user.effectiveName} for `$reason`",
                color = 0xE53714,
                image = image,
                timestamp = now
            )
        }, { user, _ -> logNonexistentUser(user, ticket.id.value) })

        bot.closeTicket(ticket.id.value)

        event.hook.sendMessage("Ticket closed").queue()
    }
}