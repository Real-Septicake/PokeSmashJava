@file:Suppress("unused")

package io.github.septicake.commands

import io.github.septicake.PokeSmashBot
import io.github.septicake.cloud.HelpCommandHelper
import io.github.septicake.cloud.annotations.Category
import io.github.septicake.cloud.annotations.CategoryEnum
import io.github.septicake.cloud.annotations.LongDescription
import io.github.septicake.cloud.annotations.ProperName
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.component.DefaultValue
import org.incendo.cloud.discord.jda5.JDAInteraction


@Category(CategoryEnum.HELP)
class HelpCommands(
    bot: PokeSmashBot
) {
    private val helper = HelpCommandHelper(bot).apply {
        addTopic(
            HelpCommandHelper.Topic(
                "Category",
                "An explanation of the `Categories` section of `/help index`",
                "Categories are groupings of commands that potentially share a " +
                        "similar purpose. These groupings are slightly arbitrary, but aren't " +
                        "entirely so. You can check out the commands present in a category by " +
                        "using `/help category` with the name of the category as it appears in " +
                        "the output of the command"
            )
        )
        addTopic(
            HelpCommandHelper.Topic(
                "Topic",
                "An explanation of the `Topics` section of `/help index`",
                "Topics are bits of information that may not be immediately obvious " +
                        "that I felt the need to elaborate on. This information is not perfect, " +
                        "although I did my best to explain the topic as plainly as possible to avoid " +
                        "extreme confusion"
            ))
        addTopic(
            HelpCommandHelper.Topic(
            "Pokemon",
            "An explanation of the type `Pokemon` that appears as a parameter",
            "The `Pokemon` type that appears as a parameter type for some commands " +
                    "accepts one of two inputs; the national dex number or the name given to the " +
                    "Pokemon by [the api](https://https://pokeapi.co/). Due to the way the Pokemon " +
                    "get named by the api, it is advised to use the national dex number, but the " +
                    "name can be retrieved from the `/species info` command"
        ))
        addTopic(
            HelpCommandHelper.Topic(
            "Species",
            "An explanation of the type `Species` that appears as a parameter",
            "The `Species` type that appears as a parameter for approximately 1 " +
                    "command accepts 2 inputs; the national dex number or the name of the Pokemon " +
                    "species. Unlike the `Pokemon` type, most all of the species names are " +
                    "intuitive, consistently being the name of the Pokemon as seen in the games. " +
                    "One minor difference is that any spaces are replaced with dashes. Names are " +
                    "case-insensitive"
        ))
        addTopic(
            HelpCommandHelper.Topic(
            "ID",
            "An explanation of the type `ID` that appears as a parameter",
            "The `ID` type that appears as a parameter type for some commands accepts " +
                    "a user's ID. To access it, you need to activate `Developer Mode` and " +
                    "right-click on the user you're tying to get the ID of.\n### For more " +
                    "specific information on getting IDs, check out [this article]" +
                    "(https://support-dev.discord.com/hc/en-us/articles/360028717192-Where-can-I-find-my-Application-Team-Server-ID)" +
                    " by Discord"
        ))
        addFilter { _, _, view -> view.botOwner }
        addFilter { user, guild, view ->
            !(!view.guildOwner || (guild != null && user == guild.ownerIdLong))
        }
        addFilter { user, guild, view ->
            !(!view.whitelist || (guild != null && bot.userWhitelisted(guild, user)))
        }
        addFilter { user, _, view ->
            view.blacklistSensitive && bot.userBlacklisted(user) != null
        }
    }

    @Command("help index")
    @ProperName("Help index")
    @CommandDescription("Lists the categories and topics")
    @LongDescription("Lists the categories of commands and the topics on information")
    fun helpIndexCommand(
        interaction: JDAInteraction
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()
        var response = helper.categories(event.user.idLong, event.guild)
            .fold("### Categories: (queried with `/help category`)")
        { acc, view ->
            "$acc\n- **" + view.name + "**: " + view.description
        } + helper.topics.values.fold("\n### Topics: (queried with `/help topic`)") { acc, view ->
            "$acc\n- **" + view.name + "**: " + view.shortDescription
        }
        if(event.user.idLong == event.guild?.ownerIdLong)
            response = "For the bot to function, you must run `/setup` first. The channel chosen must be marked " +
                    "as nsfw per Discord's ToS, as the bot does kinda fall under that umbrella. The number of polls " +
                    "sent defaults to 5. To allow other users access to `/next` and a few other commands I have " +
                    "planned, whitelist them using `/whitelist add`\n" + response
        event.hook.sendMessage(response).queue()
    }

    @Command("help category <category>")
    @ProperName("Help category")
    @CommandDescription("Lists the commands present in the category")
    @LongDescription("Prints the commands that belong to the specified category, along with their descriptions")
    fun helpCategoryCommand(
        interaction: JDAInteraction,
        @Argument("category", description = "The category to fetch the commands of")
        category: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()
        val commands = helper.allCommands(event.user.idLong, event.guild)
            .filter { it.category.equals(category, true) }
        if(commands.isEmpty()) {
            event.hook.sendMessage("There is no category matching `$category`. Please make sure it is the same as it " +
                    "appears in `/help index`").queue()
            return
        }
        val response = commands.fold("Commands under **${
            category.lowercase().replaceFirstChar { it.titlecase() }
        }**: (queried with `/help command`)") { acc, view ->
            "$acc\n- __" + view.properName + "__: " + view.description
        }
        event.hook.sendMessage(response).queue()
    }

    @Command("help topic <topic>")
    @ProperName("Help topic")
    @CommandDescription("Shows the full description of a topic")
    @LongDescription("Prints the full description of the specified topic, providing information on the material " +
            "it is about")
    fun helpTopicCommand(
        interaction: JDAInteraction,
        @Argument("topic", description = "The topic to show the full description of")
        @Greedy
        topic: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()
        val view = helper.topics[topic.lowercase()]
        if(view == null) {
            event.hook.sendMessage("There is no topic matching `$topic`. Please make sure it is the same as it appears " +
                    "in `/help index`").queue()
            return
        }
        event.hook.sendMessage(
            "## Topic: ${view.name}\n${view.longDescription}"
        ).queue()
    }

    @Command("help command <command>")
    @ProperName("Help command")
    @CommandDescription("Shows information about a command")
    @LongDescription("Shows information about a command")
    fun helpCommandCommand(
        interaction: JDAInteraction,
        @Argument("command", description = "The command to check the information of")
        @Greedy
        command: String
    ) {
        val event = interaction.interactionEvent() ?: return
        event.deferReply().setEphemeral(true).queue()
        val view = helper.allCommands(event.user.idLong, event.guild)
            .find { it.properName.equals(command, true) }
        if(view == null) {
            event.hook.sendMessage("There is no command matching `$command`. Please make sure it is the same as it " +
                    "appears in `/help category`").queue()
            return
        }
        event.hook.sendMessage(
            "## Command: ${view.properName}\n${view.longDescription}\n### Format: `/${view.command}`" +
            view.components.fold("") { acc, comp ->
                if(comp.type() == CommandComponent.ComponentType.LITERAL)
                    return@fold acc
                if(comp.type() == CommandComponent.ComponentType.REQUIRED_VARIABLE)
                    return@fold "$acc\n- ${comp.name()}: ${HelpCommandHelper.toName(comp.valueType())}" +
                            "\n  - ${comp.description().textDescription().ifEmpty { "[No Description]" }}"
                return@fold "$acc\n- ${comp.name()}: ${HelpCommandHelper.toName(comp.valueType())} = " +
                        ((comp.defaultValue() as DefaultValue.ParsedDefaultValue?)?.value() ?: "[Missing Annotation]") +
                        "\n  - ${comp.description().textDescription().ifEmpty { "[No Description]" }}"
            }
        ).queue()
    }
}