package io.github.septicake.listeners

import io.github.septicake.PokeSmashBot
import io.github.septicake.db.PollEndEntity
import kotlinx.datetime.toKotlinInstant
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.getLogger

class MessageUpdateListener(
    val bot: PokeSmashBot
): ListenerAdapter() {

    private val logger by getLogger()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if(bot.jda.selfUser.idLong != event.author.idLong) return
        try {
            event.guild
        } catch (e: IllegalStateException) {
            return
        }

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