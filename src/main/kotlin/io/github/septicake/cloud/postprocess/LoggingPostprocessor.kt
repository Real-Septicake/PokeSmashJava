package io.github.septicake.cloud.postprocess

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.PokeMeta
import io.github.septicake.db.FilterReason
import io.github.septicake.db.UsageEntity
import kotlinx.datetime.Clock
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.days

class LoggingPostprocessor<C>(val bot: PokeSmashBot) : CommandPostprocessor<C> {
    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val command = postprocessingContext.command()
        val commandMeta = postprocessingContext.command().commandMeta()
        val context = postprocessingContext.commandContext()

        val name = commandMeta.getOrDefault(PokeMeta.PROPER_NAME, command.rootComponent().name())
        val categoryName = commandMeta.getOrDefault(PokeMeta.CATEGORY_NAME, "None")
        val now = Clock.System.now()

        val usage = transaction(bot.db) {
            UsageEntity.new {
                commandName = name
                category = categoryName
                clear = now + 30.days
                result = FilterReason.PASSED
            }
        }

        context.store("Usage", usage)
    }
}