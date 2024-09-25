package io.github.septicake.cloud.annotations

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireOptions(
    vararg val options : String
)
