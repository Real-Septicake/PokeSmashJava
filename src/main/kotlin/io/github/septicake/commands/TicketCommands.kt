@file:Suppress("unused")

package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.PokeSmashConstants
import io.github.septicake.cloud.annotations.*
import io.github.septicake.db.TicketEntity
import io.github.septicake.db.TicketIncludeEntity
import io.github.septicake.db.TicketIncludeTable
import io.github.septicake.db.TicketTable
import io.github.septicake.util.EMPTY
import io.github.septicake.util.sendMessage
import io.github.septicake.util.ticketEmbed
import io.github.septicake.util.toDiscordTimestamp
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.error
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.warn

@Category(CategoryEnum.TICKET)
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
    @ProperName("Ticket open")
    @CommandDescription("Opens a ticket")
    @LongDescription("Opens a ticket, allowing for back and forth communication about a topic. " +
            "You can only have a limited amount open at a time, so if you need to open a new one, try to get an older " +
            "ticket closed first")
    suspend fun ticketOpenCommand(
        interaction: JDAInteraction,
        @Argument("topic", description = "A short blurb about the reason behind the ticket. Limited to 75 characters")
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

    @Command("ticket list")
    @PrivateOnly
    @ProperName("Ticket list")
    @CommandDescription("Lists your open tickets")
    @LongDescription("Lists the tickets that you have open at the moment")
    fun ticketListCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        val response = transaction(bot.db) {
            TicketTable.select(TicketTable.id, TicketTable.topic, TicketTable.author).orderBy(
                TicketTable.id to SortOrder.ASC
            ).where {
                TicketTable.author eq event.user.idLong
            }.foldIndexed("Open tickets:") { i, acc, r ->
                acc + "\n$i. Ticket ${r[TicketTable.id]} - `${r[TicketTable.topic]}`"
            }
        }
        event.hook.sendMessage(response).queue()
    }

    @Command("ticket info <id>")
    @PrivateOnly
    @ProperName("Ticket info")
    @CommandDescription("Lists info about the ticket")
    @LongDescription("Lists information about the ticket, including the author, topic, and when the last " +
            "activity was")
    suspend fun ticketInfoCommand(
        interaction: JDAInteraction,
        @Argument("id", description = "The id of the ticket to list information about")
        id: Int
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        val (ticket, thread) = bot.getTicket(id)
        if (ticket == null || thread == null) {
            event.hook.sendMessage(
                "Could not get ticket info, " +
                        "please double check the DM history to make sure this is the correct ID " +
                        "and that the ticket is still open"
            ).queue()
            return
        }

        if (!bot.includedInTicket(ticket.id.value, event.user.idLong) && PokeSmashConstants.ownerId != event.user.idLong) {
            event.hook.sendMessage("This is not your ticket").queue()
            logger.warn { "User ${event.user.effectiveName} tried to check ticket ${ticket.id.value} info" }
            return
        }

        event.hook.sendMessage {
            embed {
                title = "Ticket " + ticket.id.value
                description = ticket.topic
                timestamp = Clock.System.now().toJavaInstant()

                field("Author", if (event.user.idLong == ticket.author) "You" else bot.username(ticket.author) {
                    logNonexistentUser(ticket.author, ticket.id.value)
                })
                field("Opened", ticket.opened.toDiscordTimestamp())
                field("Last Active", ticket.lastActive.toDiscordTimestamp())
            }
        }
    }

    @Command("ticket include <id> <user>")
    @PrivateOnly
    @BlacklistSensitive
    @ProperName("Ticket include")
    @CommandDescription("Includes a user in the ticket")
    @LongDescription("Includes a user in the ticket, allowing them to be notified of messages in the ticket " +
            "and giving them permission to send messages as well. The user will be alerted that they were included")
    fun ticketIncludeCommand(
        interaction: JDAInteraction,
        @Argument("id", description = "The id of the ticket to include the user in")
        id: Int,
        @Argument("user", description = "The id of the user to include in the ticket")
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
        val username = bot.username(user)

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
    @ProperName("Ticket leave")
    @CommandDescription("Leaves the ticket, optionally silently")
    @LongDescription("Leaves the specified ticket, you will no longer be notified of messages sent, nor will" +
            " you be allowed to send messages in the ticket until included back. If `silent` is set to true, " +
            "the other included users will not be notified of you leaving the ticket.")
    fun ticketLeaveCommand(
        interaction: JDAInteraction,
        @Argument("id", description = "The id of the ticket to leave from")
        id: Int,
        @Argument("silent", description = "If you leaving should not be announced")
        @Default("true")
        silent: Boolean = true
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
    @BlacklistSensitive
    @ProperName("Ticket uninclude")
    @CommandDescription("Unincludes a user from the ticket")
    @LongDescription("Unincludes the user from the ticket, they will no longer receive messages about the " +
            "ticket, nor will they be allowed to message the ticket until included back. The user will be alerted" +
            " that they were unincluded from the ticket")
    fun ticketUnincludeCommand(
        interaction: JDAInteraction,
        @Argument("id", description = "The id of the ticket to uninclude the user from")
        id: Int,
        @Argument("user", description = "The id of the user to uninclude from the ticket")
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

        val username = bot.username(user) { logNonexistentUser(user, ticket.id.value) }

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
    @ProperName("Ticket message")
    @CommandDescription("Message a ticket")
    @LongDescription("Sends a message to the specified ticket, any included users also receive the message")
    fun ticketMessageCommand(
        interaction: JDAInteraction,
        @Argument("id", description = "The id of the ticket to message")
        id: Int,
        @Argument("message", description = "The message to send")
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
                if (user == ticket.author) Pair("ID", event.user.id) else EMPTY,
                timestamp = now, image = event.user.avatarUrl
            )
        }, { user, _ -> logNonexistentUser(user, ticket.id.value) })

        event.hook.sendMessage("Message sent").queue()
    }

    @Command("ticket reply <message>")
    @UserPermissions(botOwnerOnly = true)
    @ProperName("Ticket reply")
    @CommandDescription("Reply to the ticket")
    @LongDescription("Sends a message to the ticket corresponding to the thread this command is used it, " +
            "included also receive the message")
    fun ticketReplyCommand(
        interaction: JDAInteraction,
        @Argument("message", description = "The message to reply with")
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
    @ProperName("Ticket included")
    @CommandDescription("Lists users included in the ticket")
    @LongDescription("Lists all users included in the specified ticket")
    fun ticketIncludedCommand(
        interaction: JDAInteraction,
        @Argument("id", description = "The id of the ticket to list included members of")
        id: Int
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()

        if(event.user.idLong != PokeSmashConstants.ownerId && event.channelType != ChannelType.PRIVATE) {
            event.hook.sendMessage("Command must be used in a Private Channel (DM) with the bot.").queue()
            return
        }

        if (!bot.includedInTicket(id, event.user.idLong) && event.user.idLong != PokeSmashConstants.ownerId) {
            event.hook.sendMessage("This is not your ticket, are you sure you used the correct ID?").queue()
            return
        }

        var response = "[Error obtaining included users]"
        var failed = false

        transaction(bot.db) {
            val included = mutableListOf<Long>()
            val ticket = TicketEntity.findById(id)
            if(ticket == null) {
                event.hook.sendMessage("Ticket does not exist").queue()
                failed = true
                return@transaction
            }
            included += ticket.author
            TicketIncludeEntity.find { TicketIncludeTable.ticket eq id }
                .forEach {
                    included += it.user
                }

            response = included.fold("Users included in ticket $id:") { acc, l ->
                val username = bot.username(l) { logNonexistentUser(l, ticket.id.value) }
                "$acc\n$username${if (event.user.idLong == l) " [You]" else ""}"
            }
        }

        (event.channel!! as MessageChannel).sendMessage(response).queue()
        if(!failed) event.hook.sendMessage("Included users sent").queue()
    }

    @Command("ticket opened count")
    @ChannelRestriction(devChannel = true)
    @UserPermissions(botOwnerOnly = true)
    @ProperName("Ticket opened count")
    @CommandDescription("Shows the number of currently open tickets")
    @LongDescription("Shows the number of currently open tickets")
    fun ticketListOpenCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()

        val count = transaction(bot.db) {
            TicketTable.selectAll().count()
        }

        event.hook.sendMessage(
            "There " +
                    "${if(count != 1L) "are" else "is"} " +
                    "$count currently open ticket${if(count != 1L) "s" else ""}"
        ).queue()
    }

    @Command("ticket close <id> <reason>")
    @ProperName("Ticket close")
    @CommandDescription("Closes the ticket")
    @LongDescription("Closes the specified ticket. Tickets cannot be reopened so close wisely and carefully")
    fun ticketCloseCommand(
        interaction: JDAInteraction,
        @Argument("id", description = "The id of the ticket to close")
        id: Int,
        @Argument("reason", description = "The reason the ticket was closed")
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