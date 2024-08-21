package io.github.septicake

import dev.minn.jda.ktx.jdabuilder.injectKTX
import dev.minn.jda.ktx.jdabuilder.intents
import io.github.septicake.util.getEnv
import io.github.septicake.util.onJvmShutdown
import io.github.septicake.util.removeJvmShutdownThread
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.kotlin.error
import org.slf4j.kotlin.toplevel.getLogger

private val logger by getLogger()

private lateinit var shutdownThread: Thread

fun main() {
    try {
        val token = getEnv("DISCORD_TOKEN")

        val jdaBuilder = JDABuilder.createLight(token).apply {
            injectKTX()

            intents += listOf(
                GatewayIntent.GUILD_MESSAGES,
                // GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGE_POLLS,
            )

            setStatus(OnlineStatus.ONLINE)
            setActivity(Activity.watching("these weirdos"))
            setGatewayEncoding(GatewayEncoding.ETF)

            setEnableShutdownHook(true)
        }
        val bot = PokeSmashBot(jdaBuilder)
        runBlocking {
            bot.start()
        }

        shutdownThread = onJvmShutdown("PokeSmashBot-Shutdown") {
            if (bot.shutdown)
                return@onJvmShutdown

            runBlocking {
                bot.shutdown()
            }

            removeJvmShutdownThread(shutdownThread)
        }
    } catch (e: Throwable) {
        logger.error(e) { "Exception occurred while running PokeSmashOrPass bot" }
    }
}
