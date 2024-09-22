package io.github.septicake.cloud.postprocess

import io.github.septicake.PokeSmashBot
import io.github.septicake.PokeSmashConstants
import io.github.septicake.cloud.PokeMeta
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.kotlin.extension.getOrNull
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.services.type.ConsumerService
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.warn

class UserPermissionPostprocessor<C>(
    private val bot : PokeSmashBot
) : CommandPostprocessor<C> {
    private val logger by getLogger()

    override fun accept(postprocessingContext: CommandPostprocessingContext<C>) {
        val context = postprocessingContext.commandContext()
        val commandMeta = postprocessingContext.command().commandMeta()
        val interaction = context.get<GenericCommandInteractionEvent>("Interaction")

        logFailedUse(commandMeta, context, interaction)

        if(commandMeta.getOrDefault(PokeMeta.WHITELIST_ONLY, false)) {
            val guild = context.get<Guild>("Guild")
            if(!bot.userWhitelisted(guild, interaction.user.idLong)) {
                interaction.reply("\\*racks shotgun* Do not the bot.").queue()
                logFailedUse(commandMeta, context, interaction)
                ConsumerService.interrupt()
            }
        } else if(commandMeta.getOrDefault(PokeMeta.GUILD_OWNER_ONLY, false)) {
            val guild = context.get<Guild>("Guild")
            if(interaction.user.idLong != guild.ownerIdLong && interaction.user.idLong != PokeSmashConstants.ownerId) {
                interaction.reply("Command can only be used by server owner.").queue()
                logFailedUse(commandMeta, context, interaction)
                ConsumerService.interrupt()
            }
        } else if(commandMeta.getOrDefault(PokeMeta.BOT_OWNER_ONLY, false)) {
            if(PokeSmashConstants.ownerId != interaction.user.idLong) {
                interaction.reply("Only the bot owner can use this command.").queue()
                logFailedUse(commandMeta, context, interaction)
                ConsumerService.interrupt()
            }
        }
    }

    private fun logFailedUse(meta: CommandMeta, context: CommandContext<C & Any>, interaction: GenericCommandInteractionEvent) {
        logger.warn {
            val commandParameters = meta.getOrNull(PokeMeta.COMMAND_PARAMS)?.reduce { acc, s ->
                val get = context.get<Any>(s)
                "$acc$s: $get\n"
            }.orEmpty()

            val commandName = meta.get(PokeMeta.COMMAND_NAME)
            val userId = interaction.user.idLong

            "User \"$userId\" attempted to use command \"$commandName\" with params:\n$commandParameters"
        }
    }
}
