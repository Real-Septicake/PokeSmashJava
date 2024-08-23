package io.github.septicake.cloud.annotations

/**
 * A command annotation used to limit a command's use to a channel
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ChannelRestriction(
    /**
     * The command must be used in the server's specified channel
     */
    val serverChannel: Boolean = false,
    /**
     * The command can only be used in the dev channel
     */
    val devChannel: Boolean = false
)
