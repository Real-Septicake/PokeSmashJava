package io.github.septicake.cloud.annotations

/**
 * A command annotation used to limit a command's use to guilds
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GuildOnly
