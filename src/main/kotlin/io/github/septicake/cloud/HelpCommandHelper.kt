package io.github.septicake.cloud

import io.github.septicake.PokeSmashBot
import io.github.septicake.PokeSmashConstants
import io.github.septicake.pokeapi.PokemonInfo
import io.github.septicake.pokeapi.PokemonSpeciesInfo
import io.leangen.geantyref.TypeToken
import net.dv8tion.jda.api.entities.Guild
import org.incendo.cloud.Command
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.discord.jda5.JDA5CommandManager
import org.incendo.cloud.discord.jda5.JDAInteraction

class HelpCommandHelper(
    private val bot: PokeSmashBot
) {
    private val manager: JDA5CommandManager<JDAInteraction> = bot.commandManager

    val topics = mutableMapOf<String, Topic>()

    private val filters = mutableListOf<HelpCommandHelper.(Long, Guild?, CommandView) -> Boolean>()

    private fun toView(command: Command<JDAInteraction>): CommandView {
        val meta = command.commandMeta()
        return CommandView(
            meta.getOrDefault(PokeMeta.PROPER_NAME, "[No Name]"),
            command.commandDescription().description().textDescription().ifEmpty { "[No Description]" },
            meta.getOrDefault(PokeMeta.LONG_DESCRIPTION, "[No Description]"),
            manager.commandSyntaxFormatter().apply(null, command.components(), null),
            meta.getOrDefault(PokeMeta.BLACKLIST_SENSITIVE, false),
            meta.getOrDefault(PokeMeta.SERVER_CHANNEL_ONLY, false),
            command.components(),
            meta.getOrDefault(PokeMeta.CATEGORY_NAME, "[No Category]"),
            meta.getOrDefault(PokeMeta.CATEGORY_DESCRIPTION, "[No Description]"),
            meta.getOrDefault(PokeMeta.BOT_OWNER_ONLY, false),
            meta.getOrDefault(PokeMeta.GUILD_OWNER_ONLY, false),
            meta.getOrDefault(PokeMeta.WHITELIST_ONLY, false)
        )
    }

    fun allCommands(user: Long, guild: Guild?): List<CommandView> {
        val views = mutableListOf<CommandView>()
        for(c in manager.commands()) {
            val view = toView(c)
            if(user == PokeSmashConstants.ownerId) {
                views += view
                continue
            }

            if(filters.any { it.invoke(this, user, guild, view) })
                continue

            views += view
        }
        return views
    }

    fun categories(user: Long, guild: Guild?): List<CategoryView> {
        return allCommands(user, guild).map { CategoryView(it.category, it.categoryDescription) }.distinct()
    }

    fun addTopic(topic: Topic) {
        topics += topic.name to topic
    }

    fun addFilter(filter: HelpCommandHelper.(Long, guild: Guild?, CommandView) -> Boolean) {
        filters += filter
    }

    companion object{
        fun toName(type: TypeToken<*>): String {
            return when (val clazz = type.type as Class<*>) {
                PokemonInfo::class.java -> "Pokemon"
                PokemonSpeciesInfo::class.java -> "Species"
                Int::class.java -> "Integer"
                Long::class.java -> "ID" // long is only ever used for ids
                else -> clazz.simpleName.replaceFirstChar { it.titlecase() }
            }
        }
    }

    data class Topic(
        val name: String,
        val shortDescription: String,
        val longDescription: String
    )

    data class CommandView(
        val properName: String,
        val description: String,
        val longDescription: String,
        val command: String,
        val blacklistSensitive: Boolean,
        val channelLimited: Boolean,
        val components: List<CommandComponent<JDAInteraction>>,
        val category: String,
        val categoryDescription: String,
        val botOwner: Boolean,
        val guildOwner: Boolean,
        val whitelist: Boolean
    )

    data class CategoryView(
        val name: String,
        val description: String
    )
}