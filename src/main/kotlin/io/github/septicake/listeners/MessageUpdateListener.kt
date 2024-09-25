package io.github.septicake.listeners

import io.github.septicake.PokeSmashBot
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info

class MessageUpdateListener(
    val bot: PokeSmashBot
): ListenerAdapter() {

    private val logger by getLogger()

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        val poll = event.message.poll

        when {
            bot.jda.selfUser.idLong != event.author.idLong -> return
            poll == null || !poll.isFinalizedVotes -> return
        }

        if(event.channel.id != bot.testingChannel)
            bot.setPollResults(event.guild.idLong,
                bot.map.inverse()[poll!!.question.text.lowercase()]!!,
                poll.answers[0].votes.toLong(),
                poll.answers[1].votes.toLong())
        else {
            logger.info { "Test poll results: id-${bot.map.inverse()[poll!!.question.text.lowercase()]!!}, smashes-${poll.answers[0].votes}, passes-${poll.answers[1].votes.toLong()}" }
        }
    }
}