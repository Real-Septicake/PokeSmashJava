package io.github.septicake.jobs

import io.github.septicake.PokeSmashBot
import io.github.septicake.db.UsageTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger

class UsageClear : Job {
    private val logger by getLogger()
    override fun execute(context: JobExecutionContext) {
        val now = Clock.System.now()
        val bot = context.scheduler.context["Bot"] as PokeSmashBot

        transaction(bot.db) {
            logger.debug { "Usages cleared: " +
                UsageTable.deleteWhere {
                    this.clear less now
                }
            }
        }
    }
}