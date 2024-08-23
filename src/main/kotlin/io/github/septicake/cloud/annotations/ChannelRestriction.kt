package io.github.septicake.cloud.annotations

annotation class ChannelRestriction(
    val serverChannel: Boolean = false,
    val devChannel: Boolean = false
)
