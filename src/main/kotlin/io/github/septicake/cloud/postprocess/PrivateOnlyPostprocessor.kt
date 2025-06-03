package io.github.septicake.cloud.postprocess

import io.github.septicake.cloud.PokeMeta
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.services.type.ConsumerService

class PrivateOnlyPostprocessor<C> : CommandPostprocessor<C> {
    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        if(commandMeta.getOrDefault(PokeMeta.PRIVATE_ONLY, false)) {
            if (interaction.channelType != ChannelType.PRIVATE) {
                interaction.reply("Command must be used in a Private Channel (DM) with the bot.").setEphemeral(true).complete()
                ConsumerService.interrupt()
            }
        }
    }
}