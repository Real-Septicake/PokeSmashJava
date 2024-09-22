package io.github.septicake.cloud.postprocess

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.PokeMeta
import io.github.septicake.db.GuildEntity
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.services.type.ConsumerService
import org.jetbrains.exposed.sql.transactions.transaction

class ChannelRestrictionPostprocessor<C>(
    private val bot: PokeSmashBot
) : CommandPostprocessor<C> {
    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        if(commandMeta.getOrDefault(PokeMeta.SERVER_CHANNEL_ONLY, false)) {
            val channel = context.get<Channel>("InteractionChannel")
            val guild = context.get<Guild>("Guild")
            val info = transaction(bot.db) {
                GuildEntity.findById(guild.idLong)
            }
            if(info == null) {
                interaction.reply("Server has not yet been populated.").queue()
                ConsumerService.interrupt()
            }
            if(info!!.channel == null) {
                interaction.reply("Server has not yet had a channel set.").queue()
                ConsumerService.interrupt()
            }
            if(channel.idLong != info.channel) {
                val jda = context.get<JDA>("JDA")
                interaction.reply("Command cannot be used outside " + jda.getTextChannelById(info.channel!!).toString()).queue()
                ConsumerService.interrupt()
            }
        } else if(commandMeta.getOrDefault(PokeMeta.DEV_CHANNEL_ONLY, false)) {
            val channel = context.get<Channel>("InteractionChannel")
            if(channel.idLong != bot.testingChannel!!.toLong()) {
                interaction.reply("Command cannot be used outside dev channel.").queue()
                ConsumerService.interrupt()
            }
        }
    }
}
