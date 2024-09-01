package io.github.septicake

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.classgraph.ClassGraph
import io.github.septicake.cloud.PokeMeta
import io.github.septicake.cloud.annotations.*
import io.github.septicake.cloud.manager.PokeCloudCommandManager
import io.github.septicake.cloud.manager.PokemonComponentPreprocessor
import io.github.septicake.cloud.manager.RequireOptionComponentPreprocessor
import io.github.septicake.db.GuildTable
import io.github.septicake.db.PokemonTable
import io.github.septicake.db.PollTable
import io.github.septicake.db.WhitelistTable
import io.github.septicake.util.*
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.incendo.cloud.discord.jda5.annotation.ReplySettingBuilderModifier
import org.incendo.cloud.discord.slash.annotation.CommandScopeBuilderModifier
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info
import java.util.concurrent.ThreadFactory
import kotlin.collections.set
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader
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

    val map: BiMap<Int, String> = HashBiMap.create(1000)

    val commandManager = PokeCloudCommandManager(this)

    val annotationParser = AnnotationParser(commandManager, JDAInteraction::class.java).apply {
        installCoroutineSupport()
        ReplySettingBuilderModifier.install(this)
        CommandScopeBuilderModifier.install(this)

        registerBuilderModifier(ChannelRestriction::class.java, PokeMeta::channelRestrictionModifier)
        registerBuilderModifier(UserPermissions::class.java, PokeMeta::userPermissionModifier)
        registerBuilderModifier(GuildOnly::class.java, PokeMeta::guildOnlyModifier)

        registerPreprocessorMapper(RequireOptions::class.java) { annotation ->
            RequireOptionComponentPreprocessor(annotation.options)
        }
        registerPreprocessorMapper(Pokemon::class.java) { _ ->
            PokemonComponentPreprocessor(this@PokeSmashBot)
        }
    }

    val jda = builder.apply {
        addEventListeners(commandManager.createListener())
    }.build()

    lateinit var db: Database
    private lateinit var hikari: HikariDataSource

    var shutdown: Boolean = false

    suspend fun start() {
        logger.info { "Starting PokeSmashOrPass bot" }

        loadMap()

        ClassGraph()
            .enableAllInfo()
            .acceptPackages("io.github.septicake.commands")
            .scan().use { results ->
                val commandContainers = results.allClasses.filter {
                    !it.loadClass().kotlin.constructors.isEmpty()
                }.map { classInfo ->
                    val clazz = classInfo.loadClass().kotlin
                    clazz.constructors.single().call(this)
                }
                annotationParser.parse(commandContainers)
            }

        logger.info { "Connecting to bot database" }

        val hikariConfig = HikariConfig().apply {
            val sqlite = getEnv("SQLITE_ENABLED")
            if (sqlite != null) {
                jdbcUrl = "jdbc:sqlite:./test.sqlite"
                driverClassName = "org.sqlite.JDBC" // TODO: sqlite
            } else {
                val dbHost = getEnv("DB_HOST")
                val dbPort = getEnv("DB_PORT")
                val dbName = getEnv("DB_NAME")

                jdbcUrl = "jdbc:mariadb://$dbHost:$dbPort/$dbName?allowPublicKeyRetrieval=true"
                driverClassName = "org.mariadb.jdbc.Driver" // TODO: sqlite
                username = getEnv("DB_USER")
                password = getEnv("DB_PASSWORD")
            }
        }

        hikari = HikariDataSource(hikariConfig)

        val dbConfig = DatabaseConfig {
            useNestedTransactions = true
        }

        db = Database.connect(datasource = hikari, databaseConfig = dbConfig)

        transaction(db) {
            SchemaUtils.create(GuildTable, PokemonTable, PollTable, WhitelistTable)
        }

        commandManager.registerGlobalCommands(jda)
        jda.awaitReady()

        logger.info { "Bot successfully started" }
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

    private fun loadMap() {
        logger.info { "Loading pokemon map" }

        val pokemonMap = Path("src/main/resources/pokemon_map.txt")

        pokemonMap.bufferedReader().useLines { lines ->
            lines.withIndex().forEach {
                map[it.index + 1] = it.value
            }
        }

        logger.info { "Pokemon map loaded" }
    }

    fun userWhitelisted(guild: Guild, user: Long): Boolean {
        if (user == PokeSmashConstants.ownerId)
            return true
        if (user in PokeSmashConstants.whitelist)
            return true
        if (guild.ownerIdLong == user)
            return true

        return userServerWhitelisted(guild.idLong, user)
    }

    fun userServerWhitelisted(guild: Long, user: Long): Boolean {
        return !transaction(db) {
            WhitelistTable.selectAll()
                .where { WhitelistTable.guild eq guild and (WhitelistTable.user eq user) }
                .empty()
        }
    }

    object PokeSmashThreadFactory : ThreadFactory {
        private val threadGroup: ThreadGroup = currentThread.threadGroup
        private var threadCount: Int = 0

        override fun newThread(runnable: Runnable): Thread = Thread(threadGroup, runnable, "PokeSmash-Worker-${threadCount++}", 0)
    }

}
