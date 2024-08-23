package io.github.septicake.cloud.manager

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.incendo.cloud.execution.preprocessor.CommandPreprocessingContext
import org.incendo.cloud.execution.preprocessor.CommandPreprocessor
import org.incendo.cloud.services.type.ConsumerService

class PokeCommandPreprocessor : CommandPreprocessor<JDAInteraction> {
    override fun accept(context: CommandPreprocessingContext<JDAInteraction>) {

        val interaction = context.commandContext().sender().interactionEvent()

        if(interaction == null) {
            ConsumerService.interrupt()
            return
        }

        context.commandContext().store("JDA", interaction.jda)
        context.commandContext().store("Interaction", interaction)
        context.commandContext().store("InteractionChannel", interaction.channel!!)

        if(interaction.isFromGuild) {
            context.commandContext().store("Guild", interaction.guild!!)
            if(interaction.channel is MessageChannel) {
                val channel = interaction.channel as MessageChannel
                context.commandContext().store("MessageChannel", channel)
            }
        }
    }
}