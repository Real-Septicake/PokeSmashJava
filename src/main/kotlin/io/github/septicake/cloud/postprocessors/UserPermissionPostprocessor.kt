package io.github.septicake.cloud.postprocessors

import io.github.septicake.PokeSmashBot
import io.github.septicake.PokeSmashConstants
import io.github.septicake.cloud.PokeMeta
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.services.type.ConsumerService

class UserPermissionPostprocessor<C>(
    private val bot : PokeSmashBot
) : CommandPostprocessor<C> {

    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        if(commandMeta.getOrDefault(PokeMeta.WHITELIST_ONLY, false)) {
            val guild = context.get<Guild>("Guild")
            if(!bot.userWhitelisted(guild, interaction.user.idLong)) {
                interaction.reply("\\*racks shotgun* Do not the bot.").queue()
                ConsumerService.interrupt()
            }
        } else if(commandMeta.getOrDefault(PokeMeta.GUILD_OWNER_ONLY, false)) {
            val guild = context.get<Guild>("Guild")
            if(interaction.user.idLong != guild.ownerIdLong && interaction.user.idLong != PokeSmashConstants.ownerId) {
                interaction.reply("Command can only be used by server owner.").queue()
                ConsumerService.interrupt()
            }
        } else if(commandMeta.getOrDefault(PokeMeta.BOT_OWNER_ONLY, false)) {
            if(PokeSmashConstants.ownerId != interaction.user.idLong) {
                interaction.reply("Only the bot owner can use this command.").queue()
                ConsumerService.interrupt()
            }
        }
    }
}