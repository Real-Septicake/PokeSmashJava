package io.github.septicake.cloud.annotations

/**
 * The annotated command cannot be run by blacklisted users
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlacklistSensitive
