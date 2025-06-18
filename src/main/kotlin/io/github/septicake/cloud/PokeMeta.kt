package io.github.septicake.cloud

import io.github.septicake.cloud.annotations.*
import org.incendo.cloud.Command.Builder
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.kotlin.extension.cloudKey

object PokeMeta {
    val WHITELIST_ONLY: CloudKey<Boolean> = cloudKey("whitelisted-only")
    val BOT_OWNER_ONLY: CloudKey<Boolean> = cloudKey("bot-owner-only")
    val ADMIN_ONLY: CloudKey<Boolean> = cloudKey("admin-only")

    val SERVER_CHANNEL_ONLY: CloudKey<Boolean> = cloudKey("server-channel-only")
    val DEV_CHANNEL_ONLY: CloudKey<Boolean> = cloudKey("dev-channel-only")

    val GUILDS_ONLY: CloudKey<Boolean> = cloudKey("guild-only")
    val PRIVATE_ONLY: CloudKey<Boolean> = cloudKey("private-only")

    val COMMANDS_ENABLED: CloudKey<Boolean> = cloudKey("commands-enabled")

    val COMMAND_PARAMS: CloudKey<Array<out String>> = cloudKey("command-params")

    val BLACKLIST_SENSITIVE: CloudKey<Boolean> = cloudKey("blacklist-sensitive")

    val PROPER_NAME: CloudKey<String> = cloudKey("proper-name")
    val LONG_DESCRIPTION: CloudKey<String> = cloudKey("long-description")
    val CATEGORY_NAME: CloudKey<String> = cloudKey("category-name")
    val CATEGORY_DESCRIPTION: CloudKey<String> = cloudKey("category-description")

    @Suppress("UNUSED_PARAMETER")
    fun <T> blacklistSensitiveModifier(blacklistSensitive: BlacklistSensitive, builder: Builder<T>): Builder<T> {
        return builder.meta(BLACKLIST_SENSITIVE, true)
    }

    fun <T> userPermissionModifier(userPermissions: UserPermissions, builder: Builder<T>) : Builder<T> {
        return builder.meta(WHITELIST_ONLY, userPermissions.whitelistOnly)
            .meta(BOT_OWNER_ONLY, userPermissions.botOwnerOnly)
            .meta(ADMIN_ONLY, userPermissions.adminOnly)
    }

    fun <T> channelRestrictionModifier(channelRestriction: ChannelRestriction, builder: Builder<T>): Builder<T> {
        return builder.meta(SERVER_CHANNEL_ONLY, channelRestriction.serverChannel)
            .meta(DEV_CHANNEL_ONLY, channelRestriction.devChannel)
    }

    @Suppress("UNUSED_PARAMETER")
    fun <T> guildOnlyModifier(guildOnly: GuildOnly, builder: Builder<T>): Builder<T> {
        return builder.meta(GUILDS_ONLY, true)
    }

    @Suppress("UNUSED_PARAMETER")
    fun <T> privateOnlyModifier(privateOnly: PrivateOnly, builder: Builder<T>): Builder<T> {
        return builder.meta(PRIVATE_ONLY, true)
    }

    @Suppress("UNUSED_PARAMETER")
    fun <T> commandsEnabledModifier(commandsEnabled: CommandsEnabled, builder: Builder<T>): Builder<T> {
        return builder.meta(COMMANDS_ENABLED, true)
    }

    fun <T> commandParamsModifier(commandParams: CommandParams, builder: Builder<T>): Builder<T> {
        return builder.meta(COMMAND_PARAMS, commandParams.params)
    }

    fun <T> properNameModifier(properName: ProperName, builder: Builder<T>): Builder<T> {
        return builder.meta(PROPER_NAME, properName.name)
    }

    fun <T> longDescriptionModifier(longDescription: LongDescription, builder: Builder<T>): Builder<T> {
        return builder.meta(LONG_DESCRIPTION, longDescription.description)
    }

    fun <T> categoryModifier(category: Category, builder: Builder<T>): Builder<T> {
        return builder.meta(CATEGORY_NAME, category.category.categoryName)
            .meta(CATEGORY_DESCRIPTION, category.category.description)
    }
}
