package io.github.septicake.cloud.postprocessors

import io.github.septicake.cloud.PokeMeta
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.services.type.ConsumerService

class GuildOnlyPostprocessor<C> : CommandPostprocessor<C> {

    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        if(commandMeta.getOrDefault(PokeMeta.GUILDS_ONLY, false)) {
            if(interaction.guild == null) {
                interaction.reply("Command must be used in a guild.").queue()
                ConsumerService.interrupt()
            }
        }
    }
}