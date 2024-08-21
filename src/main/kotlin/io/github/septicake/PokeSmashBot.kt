package io.github.septicake

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.classgraph.ClassGraph
import io.github.septicake.db.GuildTable
import io.github.septicake.db.PokemonTable
import io.github.septicake.db.PollTable
import io.github.septicake.db.WhitelistTable
import io.github.septicake.util.ScheduledThreadPool
import io.github.septicake.util.currentThread
import io.github.septicake.util.getEnv
import io.github.septicake.util.processors
import io.github.septicake.util.runtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import net.dv8tion.jda.api.JDABuilder
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.discord.jda5.JDA5CommandManager
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.incendo.cloud.discord.jda5.annotation.ReplySettingBuilderModifier
import org.incendo.cloud.discord.slash.annotation.CommandScopeBuilderModifier
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info
import java.util.concurrent.ThreadFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class PokeSmashBot(builder: JDABuilder) {
    private val logger by getLogger()

    val scheduledThreadPool = ScheduledThreadPool((runtime.processors - 1).coerceAtLeast(1), PokeSmashThreadFactory)

    val coroutineDispatcher = scheduledThreadPool.asCoroutineDispatcher()

    val scope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

    val homeServer = getEnv("HOME_SERVER")
    val testingChannel = getEnv("TESTING_CHANNEL")
    val replyChannel = getEnv("REPLY_CHANNEL")

    val notWhitelistedResponse = "\\*racks shotgun* Do not the bot."

    val whitelist = longArrayOf(
        687780591818899515,
        734183824950427690,
        400477735811809284
    )

    val commandManager = JDA5CommandManager(
        ExecutionCoordinator.asyncCoordinator(),
        JDAInteraction.InteractionMapper.identity()
    )

    val annotationParser = AnnotationParser(commandManager, JDAInteraction::class.java).apply {
        installCoroutineSupport()
        ReplySettingBuilderModifier.install(this)
        CommandScopeBuilderModifier.install(this)
    }

    val jda = builder.apply {
        addEventListeners(commandManager.createListener())
    }.build()

    lateinit var db: Database
    private lateinit var hikari: HikariDataSource

    var shutdown: Boolean = false

    suspend fun start() {
        logger.info { "Starting PokeSmashOrPass bot" }

        ClassGraph()
            .enableAllInfo()
            .acceptPackages("io.github.septicake.commands")
            .scan().use { results ->
                val commandContainers = results.allClasses.map { classInfo ->
                    val clazz = classInfo.loadClass().kotlin
                    clazz.constructors.single().call(this)
                }
                annotationParser.parse(commandContainers)
            }

        logger.info { "Connecting to bot database" }

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mariadb://" + getEnv("DB_HOST") + ":" + getEnv("DB_PORT") + "/" + getEnv("DB_NAME")
            driverClassName = "org.mariadb.jdbc.Driver" // TODO: sqlite
            username = getEnv("DB_USER")
            password = getEnv("DB_PASSWORD")
        }

        hikari = HikariDataSource(hikariConfig)

        val dbConfig = DatabaseConfig {
            useNestedTransactions = true
        }

        db = Database.connect(datasource = hikari, databaseConfig = dbConfig)

        transaction(db) {
            SchemaUtils.create(GuildTable, PokemonTable, PollTable)
        }

        commandManager.registerGlobalCommands(jda)
        jda.awaitReady()
    }

    suspend fun shutdown() {
        shutdown = true
        logger.info { "Shutting down PokeSmashOrPass bot" }
        hikari.close()
        logger.info { "Shutdown successfully" }

        jda.shutdown()
        if (!jda.awaitShutdown(10.seconds.toJavaDuration())) {
            jda.shutdownNow()
            jda.awaitShutdown()
        }
    }

    fun userWhitelisted(guild: Long, user: Long): Boolean {
        val exists = transaction(db) {
            WhitelistTable.selectAll()
                .where { WhitelistTable.guild eq guild and (WhitelistTable.user eq user) }
                .count()
        }
        return if (exists == 1L) true else whitelist.contains(user)
    }

    object PokeSmashThreadFactory : ThreadFactory {
        private val threadGroup: ThreadGroup = currentThread.threadGroup
        private var threadCount: Int = 0

        override fun newThread(runnable: Runnable): Thread = Thread(threadGroup, runnable, "PokeSmash-Worker-${threadCount++}", 0)
    }

}
