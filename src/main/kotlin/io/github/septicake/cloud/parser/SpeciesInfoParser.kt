package io.github.septicake.cloud.parser

import io.github.reactivecircus.cache4k.Cache
import io.github.septicake.PokeSmashBot
import io.github.septicake.pokeapi.PokeApi
import io.github.septicake.pokeapi.PokemonSpeciesInfo
import io.github.septicake.util.argumentParseFailure
import io.github.septicake.util.argumentParseSuccess
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.discord.jda5.JDA5CommandManager
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.slf4j.kotlin.debug
import org.slf4j.kotlin.getLogger
import kotlin.time.Duration.Companion.days

class SpeciesInfoParser<C : Any>(
    private val bot: PokeSmashBot,
) : ArgumentParser<C, PokemonSpeciesInfo> {
    val logger by getLogger()
    private val nameToId: Map<String, Int>
    private val pokemonCache = Cache.Builder<Int, PokemonSpeciesInfo>().expireAfterWrite(7.days).build()

    init {
        val namesFile = this::class.java.getResourceAsStream("/species_map.txt") ?: error("species_map.txt should exist")

        nameToId = buildMap {
            namesFile.bufferedReader().useLines { lines ->
                lines.withIndex().forEach {
                    put(it.value.lowercase(), it.index.inc())
                }
            }
        }
    }

    override fun parse(context: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<PokemonSpeciesInfo> {
        val guild = context[JDA5CommandManager.CONTEXT_JDA_INTERACTION].guild()

        val input = commandInput.readString().lowercase()

        val parsedId = input.toIntOrNull() ?: nameToId[input]
        val pokemonId = parsedId ?: if (input == "current" && guild != null) bot.guildEntity(guild).offset else null

        if (pokemonId == null || pokemonId <= 0 || pokemonId > bot.speciesMap.size)
            return ArgumentParseResult.failure(IllegalArgumentException("Could not find a species with the name or id '$input'. The name/id is either incorrect or was not specified"))

        return runCatching { speciesById(pokemonId) }.fold(
            onSuccess = { argumentParseSuccess(it) },
            onFailure = { argumentParseFailure(it) }
        )
    }

    private fun speciesById(pokemonId: Int) = runBlocking {
        pokemonCache.get(pokemonId) {
            logger.debug { "Species $pokemonId missed in cache" }
            PokeApi.species(pokemonId)
        }
    }
}
