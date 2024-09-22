package io.github.septicake.cloud.postprocessors

import io.github.septicake.PokeSmashBot
import io.github.septicake.PokeSmashConstants
import io.github.septicake.cloud.PokeMeta
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.services.type.ConsumerService

class CommandsEnabledPostprocessor<C>(
    val bot: PokeSmashBot
) : CommandPostprocessor<C> {
    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        if (commandMeta.getOrDefault(PokeMeta.COMMANDS_ENABLED, false)) {
            if(!bot.commandsEnabled) {
                if (interaction.user.idLong != PokeSmashConstants.ownerId) {
                    interaction.reply("Commands are not currently enabled.").queue()
                    ConsumerService.interrupt()
                }
            }
        }
    }
}