package io.github.septicake.cloud

import io.github.septicake.cloud.annotations.*
import org.incendo.cloud.Command.Builder
import org.incendo.cloud.key.CloudKey

object PokeMeta {
    val WHITELIST_ONLY: CloudKey<Boolean> = CloudKey.of("whitelisted-only", Boolean::class.javaObjectType)
    val BOT_OWNER_ONLY: CloudKey<Boolean> = CloudKey.of("bot-owner-only", Boolean::class.javaObjectType)
    val GUILD_OWNER_ONLY: CloudKey<Boolean> = CloudKey.of("guild-owner-only", Boolean::class.javaObjectType)

    val SERVER_CHANNEL_ONLY: CloudKey<Boolean> = CloudKey.of("server-channel-only", Boolean::class.javaObjectType)
    val DEV_CHANNEL_ONLY: CloudKey<Boolean> = CloudKey.of("dev-channel-only", Boolean::class.javaObjectType)

    val GUILDS_ONLY: CloudKey<Boolean> = CloudKey.of("guild-only", Boolean::class.javaObjectType)

    val COMMANDS_ENABLED: CloudKey<Boolean> = CloudKey.of("commands-enabled", Boolean::class.javaObjectType)

    val COMMAND_NAME: CloudKey<String> = CloudKey.of("command-name", String::class.javaObjectType)
    val COMMAND_PARAMS: CloudKey<Array<out String>> = CloudKey.of("command-params", Array<out String>::class.javaObjectType)

    fun <T> userPermissionModifier(userPermissions: UserPermissions, builder: Builder<T>) : Builder<T> {
        return builder.meta(WHITELIST_ONLY, userPermissions.whitelistOnly)
            .meta(BOT_OWNER_ONLY, userPermissions.botOwnerOnly)
            .meta(GUILD_OWNER_ONLY, userPermissions.guildOwnerOnly)
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
    fun <T> commandsEnabledModifier(commandsEnabled: CommandsEnabled, builder: Builder<T>): Builder<T> {
        return builder.meta(COMMANDS_ENABLED, true)
    }

    fun <T> commandNameModifier(commandName: CommandName, builder: Builder<T>): Builder<T> {
        return builder.meta(COMMAND_NAME, commandName.name)
    }

    fun <T> commandParamsModifier(commandParams: CommandParams, builder: Builder<T>): Builder<T> {
        return builder.meta(COMMAND_PARAMS, commandParams.params)
    }
}