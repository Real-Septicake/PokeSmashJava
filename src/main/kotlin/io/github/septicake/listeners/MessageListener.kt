package io.github.septicake.listeners

import io.github.septicake.PokeSmashBot
import io.github.septicake.db.PollEndEntity
import kotlinx.datetime.toKotlinInstant
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger

class MessageListener(
    val bot: PokeSmashBot
): ListenerAdapter() {
    val logger by getLogger()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.isFromGuild) {
            if(bot.jda.selfUser.idLong != event.author.idLong) return

            val poll = event.message.poll ?: return
            val ends = poll.timeExpiresAt?.toInstant() ?: return

            transaction(bot.db) {
                PollEndEntity.new(event.messageIdLong) {
                    this.server = event.guild.idLong
                    this.finish = ends.toKotlinInstant()
                }
            }
        }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        event.guild.retrieveAuditLogs().queue {
            bot.openDM(it.first { entry ->
                entry.type == ActionType.BOT_ADD &&
                        entry.targetIdLong == bot.jda.selfUser.idLong
            }.userIdLong, { channel ->
                channel.sendMessage("Thank you for adding my bot to ${event.guild.name}! For information on how to " +
                        "use it, call `/help index` in your server. The command will give a bit of information regarding " +
                        "setting it up and running it").queue()
                logger.debug { event.guild.name + " owner welcomed" }
            }, {})
        }
    }
}