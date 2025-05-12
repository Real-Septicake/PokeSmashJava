package io.github.septicake.jobs

import io.github.septicake.PokeSmashBot
import io.github.septicake.db.PollEndEntity
import io.github.septicake.db.PollEndTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger

class PollCheck : Job {
    private val logger by getLogger()

    override fun execute(p0: JobExecutionContext?) {
        p0 ?: return

        var waiting = 0
        var finished = 0
        var total = 0

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val delete: ArrayList<PollEndEntity> = ArrayList()

        val bot = p0.scheduler.context["Bot"] as PokeSmashBot
        transaction(bot.db) {
            PollEndTable.selectAll().forEach { end ->
                total++
                if(end[PollEndTable.finish] > now) {
                    val guild = bot.jda.getGuildById(end[PollEndTable.server]) ?: return@forEach
                    val channel = guild.getTextChannelById(bot.guildEntity(guild).channel!!) as MessageChannel? ?: return@forEach
                    val poll = channel.retrieveMessageById(end[PollEndTable.id].value).complete().poll ?: return@forEach
                    bot.setPollResults(guild.idLong,
                        bot.pokemonMap.inverse()[poll.question.text.lowercase()]!!,
                        poll.answers[0].votes.toLong(),
                        poll.answers[1].votes.toLong())
                    delete.add(PollEndEntity.findById(end[PollEndTable.id])!!)
                    finished++
                } else {
                    waiting++
                }
            }
        }

        transaction(bot.db) {
            delete.forEach {
                it.delete()
            }
        }

        logger.debug { "Of $total polls, $finished completed in the last hour and $waiting are still waiting" }
    }
}