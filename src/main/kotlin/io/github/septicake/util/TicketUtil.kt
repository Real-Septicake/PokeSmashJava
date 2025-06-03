package io.github.septicake.util

import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.WebhookClient
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

fun <T : MessageChannel> T.ticketEmbed(
    id: Int,
    title: String?,
    message: String?,
    vararg fields: Pair<String, String>,
    image: String? = null,
    timestamp: Instant = Clock.System.now(),
    color: Int? = null
) =
    this.sendMessage(MessageCreate {
        embed {
            this.color = color
            this.title = title
            this.description = message
            this.thumbnail = image
            this.timestamp = timestamp.toJavaInstant()

            for (f in fields) {
                field(f.first, f.second)
            }

            footer { name = "Ticket ID: $id" }
        }
    }).queue()

fun WebhookClient<Message>.ticketEmbed(
    id: Int,
    title: String?,
    message: String,
    vararg fields: Pair<String, String>,
    image: String? = null,
    timestamp: Instant = Clock.System.now(),
    color: Int? = null
) =
    this.sendMessage(MessageCreate {
        embed {
            this.color = color
            this.title = title
            this.description = message
            this.thumbnail = image
            this.timestamp = timestamp.toJavaInstant()

            for (f in fields) {
                field(f.first, f.second)
            }

            footer { name = "Ticket ID: $id" }
        }
    }).queue()
