package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.db.GuildEntity
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.getLogger

class CountCommands(
    private val bot: PokeSmashBot,
) {
    private val logger by getLogger()

    @Command(value = "count [count]")
    fun countCommand(
        interaction: JDAInteraction,
        @Argument(value = "count", description = "new number of polls per call, can only be used by whitelisted users, value must be within 1 and 10")
        count: Long?
    ) {
        val event = interaction.interactionEvent() ?: return
        val info = transaction(bot.db) {
            GuildEntity.findById(event.guild!!.idLong)
        }
        if(info != null){
            if(count != null) {
                val whitelisted = bot.userWhitelisted(event.guild!!.idLong, event.user.idLong)
                if(whitelisted) {
                    if(count in 1..10) {
                        transaction(bot.db) { info.polls = count }
                        event.reply("Count successfully set to `$count`").queue()
                    } else {
                        event.reply("Count must be within 1 and 10, value given was `$count`").queue()
                    }
                } else
                    event.reply(bot.notWhitelistedResponse).queue()
            } else
                event.reply("Current poll count is `${info.polls}`").queue()
        } else { event.reply("Server has not yet been populated").queue() }
    }
}