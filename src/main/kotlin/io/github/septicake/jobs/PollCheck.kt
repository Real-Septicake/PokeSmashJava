package io.github.septicake.jobs

import dev.minn.jda.ktx.coroutines.await
import io.github.septicake.PokeSmashBot
import io.github.septicake.db.PollEndEntity
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class PollCheck : Job {
    private val logger by getLogger()

    override fun execute(context: JobExecutionContext) {
        var waiting = 0
        var finished = 0
        var total = 0

        val now = Clock.System.now()
        val delete = mutableListOf<PollEndEntity>()
        val bot = context.scheduler.context["Bot"] as PokeSmashBot

        runBlocking(Dispatchers.IO) {
            newSuspendedTransaction(db = bot.db) {
                PollEndEntity.all().map { entity ->
                    async {
                        total++

                        if (entity.finish > now) {
                            val guild = bot.jda.getGuildById(entity.server)

                            if (guild == null) {
                                delete += entity
                                return@async
                            }

                            val channel = guild.getTextChannelById(bot.guildEntity(guild).channel!!)

                            if (channel == null) {
                                delete += entity
                                return@async
                            }

                            val poll = channel.retrieveMessageById(entity.id.value).await().poll

                            if (poll == null) {
                                delete += entity
                                return@async
                            }

                            bot.setPollResults(
                                guild.idLong,
                                bot.pokemonMap.inverse()[poll.question.text.lowercase()]!!,
                                poll.answers[0].votes.toLong(),
                                poll.answers[1].votes.toLong()
                            )

                            delete += entity

                            finished++
                        } else {
                            waiting++
                        }
                    }
                }.joinAll()
            }
        }

        transaction(bot.db) {
            for (entity in delete) {
                entity.delete()
            }
        }

        logger.debug { "Of $total polls, $finished completed in the last hour and $waiting are still waiting" }
    }
}
