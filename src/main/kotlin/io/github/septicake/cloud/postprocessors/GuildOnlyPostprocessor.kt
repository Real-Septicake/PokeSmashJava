package io.github.septicake.cloud.postprocessors

import io.github.septicake.cloud.PokeMeta
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.services.type.ConsumerService
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info

class GuildOnlyPostprocessor<C> : CommandPostprocessor<C> {

    private val logger by getLogger()

    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        if(commandMeta.getOrDefault(PokeMeta.GUILDS_ONLY, false)) {
            logger.info { "Marked as guild only" }
            if(interaction.guild == null) {
                interaction.reply("Command must be used in a guild.").queue()
                ConsumerService.interrupt()
            }
        }
    }
}