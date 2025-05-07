package io.github.septicake.listeners

import io.github.septicake.PokeSmashBot
import io.github.septicake.db.PollEndEntity
import io.github.septicake.db.PollEndTable
import kotlinx.datetime.toKotlinLocalDateTime
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info

class MessageUpdateListener(
    val bot: PokeSmashBot
): ListenerAdapter() {

    private val logger by getLogger()

//    override fun onMessageUpdate(event: MessageUpdateEvent) {
//        val poll = event.message.poll
//
//        when {
//            bot.jda.selfUser.idLong != event.author.idLong -> return
//            poll == null || !poll.isFinalizedVotes -> return
//        }
//
//        if(event.channel.idLong != bot.testingChannel)
//            bot.setPollResults(event.guild.idLong,
//                bot.map.inverse()[poll!!.question.text.lowercase()]!!,
//                poll.answers[0].votes.toLong(),
//                poll.answers[1].votes.toLong())
//        else {
//            logger.info { "Test poll results: id-${bot.map.inverse()[poll!!.question.text.lowercase()]!!}, smashes-${poll.answers[0].votes}, passes-${poll.answers[1].votes.toLong()}" }
//        }
//    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if(bot.jda.selfUser.idLong != event.author.idLong) return
        try {
            event.guild
        } catch (e: IllegalStateException) {
            return
        }

        val poll = event.message.poll ?: return
        val ends = poll.timeExpiresAt?.toLocalDateTime() ?: return

        transaction(bot.db) {
            PollEndEntity.new(event.messageIdLong) {
                this.server = event.guild.idLong
                this.finish = ends.toKotlinLocalDateTime()
            }
        }
    }
}