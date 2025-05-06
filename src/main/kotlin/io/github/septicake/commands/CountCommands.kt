@file:Suppress("unused")

package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.annotations.CommandParams
import io.github.septicake.cloud.annotations.GuildOnly
import io.github.septicake.cloud.annotations.UserPermissions
import io.github.septicake.db.GuildEntity
import org.incendo.cloud.annotation.specifier.Range
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger

class CountCommands(
    private val bot: PokeSmashBot,
) {
    private val logger by getLogger()

    @GuildOnly
    @UserPermissions(whitelistOnly = true)
    @Command(value = "set count <count>")
    @CommandParams("count")
    fun countSetCommand(
        interaction: JDAInteraction,
        @Argument(value = "count", description = "new number of polls per call, can only be used by whitelisted users, value must be within 1 and 10")
        @Range(min = "1", max = "10")
        count: Int
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        logger.debug { "Set count called with value $count in ${event.guild}" }
        val info = transaction(bot.db) {
            GuildEntity.findById(event.guild!!.idLong)
        }
        if(info != null){
            transaction(bot.db) { info.polls = count }
            event.hook.sendMessage("Count successfully set to `$count`").queue()
        } else { event.hook.sendMessage("Server has not yet been populated").queue() }
    }

    @GuildOnly
    @Command("count")
    fun countCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().queue()
        logger.debug { "Count called from ${event.guild}" }
        val info = transaction(bot.db) {
            GuildEntity.findById(event.guild!!.idLong)
        }
        if(info != null) event.hook.sendMessage("Current poll count is `${info.polls}`")
        else event.hook.sendMessage("Server has not yet been populated").queue()
    }
}