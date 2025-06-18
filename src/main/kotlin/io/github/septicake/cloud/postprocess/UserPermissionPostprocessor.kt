package io.github.septicake.cloud.postprocess

import io.github.septicake.PokeSmashBot
import io.github.septicake.PokeSmashConstants
import io.github.septicake.cloud.PokeMeta
import io.github.septicake.db.FilterReason
import io.github.septicake.db.UsageEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor
import org.incendo.cloud.kotlin.extension.getOrNull
import org.incendo.cloud.meta.CommandMeta
import org.incendo.cloud.services.type.ConsumerService
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.debug
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
        val usage = context.get<UsageEntity>("Usage")

        if(commandMeta.getOrDefault(PokeMeta.WHITELIST_ONLY, false)) {
            logger.debug { "whitelist only \"${interaction.fullCommandName}\"" }
            val guild = context.get<Guild>("Guild")
            if(!bot.userWhitelisted(guild, interaction.user.idLong)) {
                interaction.reply("\\*racks shotgun* Do not the bot.").setEphemeral(true).complete()
                transaction(bot.db) { usage.result = FilterReason.NOT_WHITELISTED }
                logFailedUse(commandMeta, context, interaction)
                ConsumerService.interrupt()
            }
        } else if(commandMeta.getOrDefault(PokeMeta.ADMIN_ONLY, false)) {
            logger.debug { "admin only \"${interaction.fullCommandName}\"" }
            val guild = context.get<Guild>("Guild")
            val admin = guild.getMemberById(interaction.user.idLong)?.hasPermission(Permission.ADMINISTRATOR) ?: false
            if(!admin && interaction.user.idLong != PokeSmashConstants.ownerId) {
                interaction.reply("Command can only be used by admins.").setEphemeral(true).complete()
                transaction(bot.db) { usage.result = FilterReason.NOT_ADMIN }
                logFailedUse(commandMeta, context, interaction)
                ConsumerService.interrupt()
            }
        } else if(commandMeta.getOrDefault(PokeMeta.BOT_OWNER_ONLY, false)) {
            logger.debug { "bot owner only \"${interaction.fullCommandName}\"" }
            if(PokeSmashConstants.ownerId != interaction.user.idLong) {
                interaction.reply("Only the bot owner can use this command.").setEphemeral(true).complete()
                transaction(bot.db) { usage.result = FilterReason.NOT_BOT_OWNER }
                logFailedUse(commandMeta, context, interaction)
                ConsumerService.interrupt()
            }
        }
    }

    private fun logFailedUse(meta: CommandMeta, context: CommandContext<C & Any>, interaction: GenericCommandInteractionEvent) {
        logger.warn {
            val commandParameters = meta.getOrNull(PokeMeta.COMMAND_PARAMS)?.fold("with params:") { acc, s ->
                val get = context.getOrDefault<Any>(s, "[None]")
                "$acc\n$s: $get"
            }.orEmpty()

            val userId = interaction.user.idLong
            val guildId = interaction.guild?.id
            val guildName = interaction.guild?.name ?: "DMs"

            "User $userId in $guildName ${if(guildId != null) "($guildId) " else ""}attempted to use command \"${
                meta.getOrDefault(PokeMeta.PROPER_NAME, "[No Name]")
            }\" $commandParameters"
        }
    }
}
