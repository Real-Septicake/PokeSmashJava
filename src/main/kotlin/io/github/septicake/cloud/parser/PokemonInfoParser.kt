package io.github.septicake.cloud.parser

import io.github.reactivecircus.cache4k.Cache
import io.github.septicake.PokeSmashBot
import io.github.septicake.pokeapi.PokeApi
import io.github.septicake.pokeapi.PokemonInfo
import io.github.septicake.util.argumentParseFailure
import io.github.septicake.util.argumentParseSuccess
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.discord.jda5.JDA5CommandManager
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.suggestion.SuggestionProvider
import kotlin.time.Duration.Companion.days

class PokemonInfoParser<C : Any>(
    private val bot: PokeSmashBot,
) : ArgumentParser<C, PokemonInfo> {
    private val nameToId: Map<String, Int>
    private val pokemonCache = Cache.Builder<Int, PokemonInfo>().expireAfterWrite(7.days).build()

    init {
        val namesFile = this::class.java.getResourceAsStream("/pokemon_map.txt") ?: error("pokemon_map.txt should exist")

        nameToId = buildMap {
            namesFile.bufferedReader().useLines { lines ->
                lines.withIndex().forEach {
                    put(it.value.lowercase(), it.index.inc())
                }
            }
        }
    }

    override fun parse(context: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<PokemonInfo> {
        val guild = context[JDA5CommandManager.CONTEXT_JDA_INTERACTION].guild()

        val input = commandInput.readString().lowercase()

        val parsedId = input.toIntOrNull() ?: nameToId[input]
        val pokemonId = parsedId ?: if (input == "current" && guild != null) bot.guildEntity(guild).offset else null

        if (pokemonId == null)
            return ArgumentParseResult.failure(IllegalArgumentException("Could not find a pokemon with the name or id '$input'. The name/id is either incorrect or was not specified"))

        return runCatching { pokemonById(pokemonId) }.fold(
            onSuccess = { argumentParseSuccess(it) },
            onFailure = { argumentParseFailure(it) }
        )
    }

    override fun suggestionProvider(): SuggestionProvider<C> {
        return SuggestionProvider.blockingStrings { context, _ ->
            val guild = context[JDA5CommandManager.CONTEXT_JDA_INTERACTION].guild()

            val currentId = if (guild != null) bot.guildEntity(guild).offset else null
            return@blockingStrings buildList {
                if (currentId != null) {
                    add(currentId.toString())

                    add(pokemonById(currentId).name)
                }

                add("pikachu")
            }
        }
    }

    private fun pokemonById(pokemonId: Int) = runBlocking {
        pokemonCache.get(pokemonId) {
            PokeApi.pokemon(pokemonId)
        }
    }
}
