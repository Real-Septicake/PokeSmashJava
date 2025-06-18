package io.github.septicake

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.minn.jda.ktx.coroutines.await
import io.github.classgraph.ClassGraph
import io.github.septicake.cloud.PokeMeta
import io.github.septicake.cloud.annotations.*
import io.github.septicake.cloud.parser.PokemonInfoParser
import io.github.septicake.cloud.parser.SpeciesInfoParser
import io.github.septicake.cloud.postprocess.*
import io.github.septicake.cloud.preprocess.LengthMaxComponentPreprocessor
import io.github.septicake.cloud.preprocess.PokeCommandPreprocessor
import io.github.septicake.db.*
import io.github.septicake.jobs.PollCheck
import io.github.septicake.jobs.UsageClear
import io.github.septicake.listeners.MessageListener
import io.github.septicake.util.ScheduledThreadPool
import io.github.septicake.util.currentThread
import io.github.septicake.util.getEnv
import io.github.septicake.util.processors
import io.github.septicake.util.runtime
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.discord.jda5.JDA5CommandManager
import org.incendo.cloud.discord.jda5.JDAInteraction
import org.incendo.cloud.discord.jda5.JDAInteraction.InteractionMapper
import org.incendo.cloud.discord.slash.CommandScope
import org.incendo.cloud.discord.slash.annotation.CommandScopeBuilderModifier
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.kotlin.extension.parserDescriptor
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info
import java.util.concurrent.ThreadFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.datetime.Clock
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


class PokeSmashBot(builder: JDABuilder) : CoroutineScope {
    private val logger by getLogger()

    val scheduledThreadPool = ScheduledThreadPool((runtime.processors - 1).coerceAtLeast(1), PokeSmashThreadFactory)

    val coroutineDispatcher = scheduledThreadPool.asCoroutineDispatcher()

    override val coroutineContext = SupervisorJob() + coroutineDispatcher

    val homeServer = getEnv("HOME_SERVER")?.toLong()
    val ticketChannel = getEnv("TICKET_CHANNEL")!!.toLong()
    val testingChannel = getEnv("TESTING_CHANNEL")!!.toLong()
    val replyChannel = getEnv("REPLY_CHANNEL")!!.toLong()

    var commandsEnabled = true

    val pokemonMap: BiMap<Int, String> = HashBiMap.create(1000)
    val speciesMap: BiMap<Int, String> = HashBiMap.create(1000)

    val commandManager = JDA5CommandManager(
        ExecutionCoordinator.asyncCoordinator(),
        InteractionMapper.identity()
    ).apply {
        registerCommandPreProcessor(PokeCommandPreprocessor())

        registerCommandPostProcessor(BlacklistSensitivePostprocessor<JDAInteraction>(this@PokeSmashBot))
        registerCommandPostProcessor(ChannelRestrictionPostprocessor<JDAInteraction>(this@PokeSmashBot))
        registerCommandPostProcessor(UserPermissionPostprocessor<JDAInteraction>(this@PokeSmashBot))
        registerCommandPostProcessor(GuildOnlyPostprocessor<JDAInteraction>(this@PokeSmashBot))
        registerCommandPostProcessor(PrivateOnlyPostprocessor<JDAInteraction>(this@PokeSmashBot))
        registerCommandPostProcessor(CommandsEnabledPostprocessor<JDAInteraction>(this@PokeSmashBot))

        // Must remain last
        registerCommandPostProcessor(LoggingPostprocessor<JDAInteraction>(this@PokeSmashBot))

        parserRegistry().registerParser(parserDescriptor(PokemonInfoParser(this@PokeSmashBot)))
        parserRegistry().registerParser(parserDescriptor(SpeciesInfoParser(this@PokeSmashBot)))
    }

    val annotationParser = AnnotationParser(commandManager, JDAInteraction::class.java).apply {
        installCoroutineSupport()
        CommandScopeBuilderModifier.install(this)

        registerBuilderModifier(BlacklistSensitive::class.java, PokeMeta::blacklistSensitiveModifier)
        registerBuilderModifier(ChannelRestriction::class.java, PokeMeta::channelRestrictionModifier)
        registerBuilderModifier(UserPermissions::class.java, PokeMeta::userPermissionModifier)
        registerBuilderModifier(GuildOnly::class.java, PokeMeta::guildOnlyModifier)
        registerBuilderModifier(PrivateOnly::class.java, PokeMeta::privateOnlyModifier)
        registerBuilderModifier(CommandsEnabled::class.java, PokeMeta::commandsEnabledModifier)
        registerBuilderModifier(CommandParams::class.java, PokeMeta::commandParamsModifier)

        registerBuilderModifier(ProperName::class.java, PokeMeta::properNameModifier)
        registerBuilderModifier(LongDescription::class.java, PokeMeta::longDescriptionModifier)
        registerBuilderModifier(Category::class.java, PokeMeta::categoryModifier)

        registerPreprocessorMapper(LengthMax::class.java) { annotation ->
            LengthMaxComponentPreprocessor<JDAInteraction>(annotation.length)
        }
    }

    val jda = builder.apply {
        addEventListeners(commandManager.createListener())
        addEventListeners(MessageListener(this@PokeSmashBot))
    }.build()

    lateinit var db: Database
    private lateinit var hikari: HikariDataSource

    var shutdown: Boolean = false

    lateinit var scheduler: Scheduler

    suspend fun start() {
        logger.info { "Starting PokeSmashOrPass bot" }

        loadMaps()

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
                jdbcUrl = "jdbc:sqlite:./test.db"
                driverClassName = "org.sqlite.JDBC"
            } else {
                val dbHost = getEnv("DB_HOST")!!
                val dbPort = getEnv("DB_PORT")!!
                val dbName = getEnv("DB_NAME")!!

                jdbcUrl = "jdbc:mariadb://$dbHost:$dbPort/$dbName?allowPublicKeyRetrieval=true"
                driverClassName = "org.mariadb.jdbc.Driver"
                username = getEnv("DB_USER")!!
                password = getEnv("DB_PASSWORD")!!
            }
        }

        hikari = HikariDataSource(hikariConfig)

        val dbConfig = DatabaseConfig {
            useNestedTransactions = true
        }

        db = Database.connect(datasource = hikari, databaseConfig = dbConfig)

        transaction(db) {
            SchemaUtils.create(
                // General Tables
                GuildTable,
                PokemonTable,
                UsageTable,
                // Poll Tables
                PollEndTable,
                PollTable,
                // User Permission Tables
                WhitelistTable,
                BlacklistTable,
                // Ticket Tables
                TicketTable,
                UserTicketTable,
                TicketIncludeTable
            )
        }

        jda.updateCommands()
            .addCommands(commandManager.commandFactory().createCommands(CommandScope.global()))
            .queue {
                logger.info { "Commands added" }
            }

        jda.awaitReady()

        logger.info { "Bot successfully started with user ${jda.selfUser.name}" }

        val pollJob = JobBuilder.newJob(PollCheck::class.java)
            .withIdentity(PokeSmashConstants.PollCheckIdentity)
            .build()

        val pollTrigger = TriggerBuilder.newTrigger()
            .withIdentity("PollTrigger")
            .startNow()
            .withSchedule(
                CronScheduleBuilder
                    .cronSchedule("0 0 * * * ?")
                    .withMisfireHandlingInstructionFireAndProceed()
            )
            .forJob(PokeSmashConstants.PollCheckIdentity)
            .build()

        val usageJob = JobBuilder.newJob(UsageClear::class.java)
            .withIdentity(PokeSmashConstants.UsageClearIdentity)
            .build()

        val usageTrigger = TriggerBuilder.newTrigger()
            .withIdentity("UsageTrigger")
            .startNow()
            .withSchedule(
                CronScheduleBuilder
                    .cronSchedule("0 0 0 * * ?")
                    .withMisfireHandlingInstructionFireAndProceed()
            )
            .forJob(PokeSmashConstants.UsageClearIdentity)
            .build()

        val sf = StdSchedulerFactory()
        scheduler = sf.getScheduler()

        scheduler.context["Bot"] = this
        scheduler.scheduleJob(pollJob, pollTrigger)
        scheduler.scheduleJob(usageJob, usageTrigger)

        scheduler.start()
    }

    suspend fun shutdown(isShutdownThread: Boolean = false) {
        shutdown = true
        logger.info { "Shutting down PokeSmashOrPass bot" }
        hikari.close()
        scheduler.shutdown()
        logger.info { "Shutdown successfully" }

        jda.shutdown()

        if (!jda.awaitShutdown(10.seconds.toJavaDuration())) {
            jda.shutdownNow()
            jda.awaitShutdown()
        }

        if (!isShutdownThread)
            removeShutdownHook()
    }

    private fun loadMaps() {
        logger.info { "Loading pokemon map" }

        this::class.java.getResourceAsStream("/pokemon_map.txt")!!.bufferedReader().useLines { lines ->
            lines.withIndex().forEach {
                pokemonMap[it.index + 1] = it.value
            }
        }

        logger.info { "Pokemon map loaded" }

        logger.info { "Loading species map" }

        this::class.java.getResourceAsStream("/species_map.txt")!!.bufferedReader().useLines { lines ->
            lines.withIndex().forEach {
                speciesMap[it.index + 1] = it.value
            }
        }

        logger.info { "Species map loaded" }
    }

    fun userWhitelisted(guild: Guild, user: Long): Boolean {
        if (user == PokeSmashConstants.ownerId)
            return true
        if (user in PokeSmashConstants.whitelist)
            return true
        if (guild.getMemberById(user)?.hasPermission(Permission.ADMINISTRATOR) == true)
            return true

        return userServerWhitelisted(guild.idLong, user)
    }

     fun userBlacklisted(user: Long) = transaction(db = db) {
        BlacklistEntity.findById(user)
    }

    suspend fun setPollResults(guildId: Long, pokemonId: Int, smashVotes: Long, passVotes: Long) {
        val poll = pollEntity(guildId, pokemonId)
        newSuspendedTransaction(db = db) {
            val guildInfo = GuildEntity.findById(guildId) ?: throw ServerNotPopulatedException()
            val pokemonInfo = PokemonEntity.findById(pokemonId) ?: PokemonEntity.new(pokemonId) {
                smashWins = 0
                smashes = 0
                passWins = 0
                passes = 0
            }

            val pollResult = if (passVotes >= smashVotes) PollResult.PASSED else PollResult.SMASHED

            if (poll == null) {
                when (pollResult) {
                    PollResult.SMASHED -> {
                        guildInfo.smashes += 1
                        pokemonInfo.smashWins += 1
                    }

                    PollResult.PASSED -> {
                        guildInfo.passes += 1
                        pokemonInfo.passWins += 1
                    }
                }
            } else if (pollResult != poll.result) {
                when (poll.result) {
                    PollResult.SMASHED -> {
                        guildInfo.smashes -= 1
                        pokemonInfo.smashWins -= 1
                        guildInfo.passes += 1
                        pokemonInfo.passWins += 1
                    }

                    PollResult.PASSED -> {
                        guildInfo.passes -= 1
                        pokemonInfo.passWins -= 1
                        guildInfo.smashes += 1
                        pokemonInfo.smashWins += 1
                    }
                }
            }

            if (poll != null) {
                pokemonInfo.smashes -= poll.smashes
                pokemonInfo.passes -= poll.passes
            }

            val poll = poll ?: PollEntity.new {
                guild = guildId
                pokemon = pokemonId
                result = pollResult
            }

            poll.smashes = smashVotes
            poll.passes = passVotes

            pokemonInfo.smashes += smashVotes
            pokemonInfo.passes += passVotes
            poll.result = pollResult
        }
    }

    fun userServerWhitelisted(guild: Long, user: Long): Boolean {
        return transaction(db) {
            !WhitelistTable.selectAll()
                .where { WhitelistTable.guild eq guild and (WhitelistTable.user eq user) }
                .empty()
        }
    }

    fun guildEntity(jdaGuild: Guild) = transaction(db) {
        GuildEntity.findById(jdaGuild.idLong) ?: GuildEntity.new(jdaGuild.idLong) {
            this.name = jdaGuild.name
        }
    }

    fun pokemonEntity(pokemonId: Int) = transaction(db) {
        PokemonEntity.findById(pokemonId)
    }

    fun pollEntity(guildId: Long, pokemonId: Int) = transaction(db) {
        PollEntity.find { (PollTable.guild eq guildId) and (PollTable.pokemon eq pokemonId) }.singleOrNull()
    }

    fun userTicketEntity(user: Long) = transaction(db) {
        UserTicketEntity.findById(user) ?: UserTicketEntity.new(user) {
            openTickets = 0
            maxOpenTickets = 5
            totalTickets = 0
        }
    }

    suspend fun openTicket(author: Long, topic: String) : Pair<TicketEntity, ThreadChannel> {
        val channel = jda.getTextChannelById(this.ticketChannel)!!

        val message = channel.sendMessage("[Not Populated]").await()
        val thread = message.createThreadChannel("[Not Populated]").await()

        val entity = transaction(db) {
            TicketEntity.new {
                this.author = author
                this.thread = thread.idLong
                this.topic = topic
                this.lastActive = Clock.System.now()
            }
        }

        thread.manager.setName("[OPEN] Ticket ${entity.id.value}").await()
        message.editMessage("$topic - ${entity.id.value}").await()

        transaction(db) {
            val userTicket = userTicketEntity(author)
            userTicket.openTickets++
            userTicket.totalTickets++
        }

        return Pair(entity, thread)
    }

    fun getTicket(id: Int) : Pair<TicketEntity?, ThreadChannel?> {
        val entity = transaction(db) { TicketEntity.findById(id) } ?: return Pair(null, null)
        val thread = jda.getThreadChannelById(entity.thread)

        return Pair(entity, thread)
    }

    fun closeTicket(id: Int) : Boolean {
        val entity = transaction(db) { TicketEntity.findById(id) } ?: return false
        val user = userTicketEntity(entity.author)

        val thread = jda.getThreadChannelById(entity.thread) ?: return false
        thread.manager.setName("[CLOSED] Ticket ${entity.id.value}").queue()
        thread.manager.setArchived(true).queue()
        transaction(db) {
            TicketIncludeTable.deleteWhere { this.ticket eq entity.id.value }
            entity.delete()
            user.openTickets--
        }
        return true
    }

    fun messageTicketIncludes(id: Int, accept: (Long, Boolean, PrivateChannel) -> Unit, error: (Long, Throwable) -> Unit) {
        transaction(db) {
            val ticket = TicketEntity.findById(id) ?: return@transaction
            openDMWithID(ticket.author, { user, channel -> accept(user, false, channel) }, error)

            TicketIncludeTable.selectAll().where { TicketIncludeTable.ticket eq ticket.id.value }.forEach {
                openDMWithID(it[TicketIncludeTable.user], { user, channel ->
                        accept(user, it[TicketIncludeTable.muted], channel)
                    }, error)
            }
        }
    }

    fun includedInTicket(id: Int, user: Long) =
        transaction(db) { !TicketIncludeEntity.find {
            TicketIncludeTable.ticket eq id and (TicketIncludeTable.user eq user)
        }.empty() || TicketEntity.findById(id)?.author == user }

    private fun openDMWithID(user: Long, accept: (Long, PrivateChannel) -> Unit, error: (Long, Throwable) -> Unit) =
        jda.openPrivateChannelById(user).queue({ accept(user, it) }, { error(user, it) })

    fun openDM(user: Long, accept: (PrivateChannel) -> Unit, error: (Throwable) -> Unit) =
        jda.openPrivateChannelById(user).queue(accept, error)

    fun username(user: Long, failure: () -> Unit = {}) : String {
        return try {
            jda.retrieveUserById(user).complete().effectiveName
        } catch(_: Throwable) {
            failure()
            "[Unknown]"
        }
    }

    object PokeSmashThreadFactory : ThreadFactory {
        private val threadGroup: ThreadGroup = currentThread.threadGroup
        private var threadCount: Int = 0

        override fun newThread(runnable: Runnable): Thread =
            Thread(threadGroup, runnable, "PokeSmash-Worker-${threadCount++}", 0)
    }

    class ServerNotPopulatedException : IllegalStateException()
    class PollDoesNotExistException : IllegalStateException()
}
