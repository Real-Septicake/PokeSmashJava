package io.github.septicake.cloud

import io.github.septicake.cloud.annotations.ChannelRestriction
import io.github.septicake.cloud.annotations.GuildOnly
import io.github.septicake.cloud.annotations.UserPermissions
import org.incendo.cloud.Command.Builder
import org.incendo.cloud.key.CloudKey

object PokeMeta {
    val WHITELIST_ONLY: CloudKey<Boolean> = CloudKey.of("whitelisted-only", Boolean::class.javaObjectType)
    val BOT_OWNER_ONLY: CloudKey<Boolean> = CloudKey.of("bot-owner-only", Boolean::class.javaObjectType)

    val SERVER_CHANNEL_ONLY: CloudKey<Boolean> = CloudKey.of("server-channel-only", Boolean::class.javaObjectType)
    val DEV_CHANNEL_ONLY: CloudKey<Boolean> = CloudKey.of("dev-channel-only", Boolean::class.javaObjectType)

    val GUILDS_ONLY: CloudKey<Boolean> = CloudKey.of("guild-only", Boolean::class.javaObjectType)

    fun <T> userPermissionModifier(userPermissions: UserPermissions, builder: Builder<T>) : Builder<T> {
        return builder.meta(WHITELIST_ONLY, userPermissions.whitelistOnly)
            .meta(BOT_OWNER_ONLY, userPermissions.botOwnerOnly)
    }

    fun <T> channelRestrictionModifier(channelRestriction: ChannelRestriction, builder: Builder<T>): Builder<T> {
        return builder.meta(SERVER_CHANNEL_ONLY, channelRestriction.serverChannel)
            .meta(DEV_CHANNEL_ONLY, channelRestriction.devChannel)
    }

    @Suppress("UNUSED_PARAMETER")
    fun <T> guildOnlyModifier(guildOnly: GuildOnly, builder: Builder<T>): Builder<T> {
        return builder.meta(GUILDS_ONLY, true)
    }
}