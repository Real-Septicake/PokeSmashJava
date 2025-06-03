package io.github.septicake.cloud.annotations

/**
 * Limit a commands use to Private Channels only
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrivateOnly
