package io.github.septicake.cloud

import io.github.septicake.cloud.annotations.ChannelRestriction
import io.github.septicake.cloud.annotations.CommandName
import io.github.septicake.cloud.annotations.CommandParams
import io.github.septicake.cloud.annotations.CommandsEnabled
import io.github.septicake.cloud.annotations.GuildOnly
import io.github.septicake.cloud.annotations.UserPermissions
import org.incendo.cloud.Command.Builder
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.kotlin.extension.cloudKey

object PokeMeta {
    val WHITELIST_ONLY: CloudKey<Boolean> = cloudKey("whitelisted-only")
    val BOT_OWNER_ONLY: CloudKey<Boolean> = cloudKey("bot-owner-only")
    val GUILD_OWNER_ONLY: CloudKey<Boolean> = cloudKey("guild-owner-only")

    val SERVER_CHANNEL_ONLY: CloudKey<Boolean> = cloudKey("server-channel-only")
    val DEV_CHANNEL_ONLY: CloudKey<Boolean> = cloudKey("dev-channel-only")

    val GUILDS_ONLY: CloudKey<Boolean> = cloudKey("guild-only")

    val COMMANDS_ENABLED: CloudKey<Boolean> = cloudKey("commands-enabled")

    val COMMAND_NAME: CloudKey<String> = cloudKey("command-name")
    val COMMAND_PARAMS: CloudKey<Array<out String>> = cloudKey("command-params")

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
