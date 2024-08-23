package io.github.septicake.cloud.annotations

/**
 * A command annotation used to define restrictions for what users can use commands
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UserPermissions(
    /**
     * Users must be whitelisted in their server to use the command
     */
    val whitelistOnly: Boolean = false,
    /**
     * Only the bot owner can use the command
     */
    val botOwnerOnly: Boolean = false,
    /**
     * The user must be the guild owner to use the command
     */
    val guildOwnerOnly: Boolean = false
)
