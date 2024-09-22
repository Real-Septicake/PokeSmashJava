package io.github.septicake.util

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.Mentions
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.WebhookClient
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData

suspend inline fun <T> WebhookClient<T>.sendMessage(
    content: String = "",
    embeds: Collection<MessageEmbed> = emptyList(),
    files: Collection<FileUpload> = emptyList(),
    components: Collection<LayoutComponent> = emptyList(),
    tts: Boolean = false,
    mentions: Mentions = Mentions.default(),
    builder: InlineMessage<MessageCreateData>.() -> Unit = {},
): T = sendMessage(MessageCreate(content, embeds, files, components, tts, mentions, builder)).await()

suspend inline fun MessageChannel.sendMessage(
    content: String = "",
    embeds: Collection<MessageEmbed> = emptyList(),
    files: Collection<FileUpload> = emptyList(),
    components: Collection<LayoutComponent> = emptyList(),
    tts: Boolean = false,
    mentions: Mentions = Mentions.default(),
    builder: InlineMessage<MessageCreateData>.() -> Unit = {},
): Message? = sendMessage(MessageCreate(content, embeds, files, components, tts, mentions, builder)).await()
