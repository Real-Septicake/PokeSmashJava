package io.github.septicake.cloud.postprocess

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.PokeMeta
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.services.type.ConsumerService
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.warn

class BlacklistSensitivePostprocessor<C>(
    private val bot: PokeSmashBot
) : CommandPostprocessor<C> {
    private val logger by getLogger()

    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        if(commandMeta.getOrDefault(PokeMeta::BLACKLIST_SENSITIVE, false)){
            val user = bot.userBlacklisted(interaction.user.idLong)
            if (user != null) {
                interaction.reply(
                    "You have been blacklisted and cannot use this command.\n"
                            + "Reason: `${user.reason}`"
                ).setEphemeral(true).complete()
                logger.warn { "Blacklisted user \"${interaction.user.name}\" attempted to use \"${interaction.fullCommandName}\"" }
                ConsumerService.interrupt()
            }
        }
    }
}