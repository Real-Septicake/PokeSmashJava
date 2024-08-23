package io.github.septicake.cloud.annotations

annotation class UserPermissions(
    val whitelistOnly: Boolean = false,
    val botOwnerOnly: Boolean = false
)
