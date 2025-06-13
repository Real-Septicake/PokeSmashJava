package io.github.septicake.cloud.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProperName(
    val name: String
)
