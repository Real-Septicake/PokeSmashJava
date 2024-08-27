package io.github.septicake.cloud.annotations

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class RequireOptions(
    vararg val options : String
)
