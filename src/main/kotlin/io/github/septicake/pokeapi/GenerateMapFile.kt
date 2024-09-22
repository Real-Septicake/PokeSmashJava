package io.github.septicake.pokeapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.slf4j.kotlin.info
import org.slf4j.kotlin.toplevel.getLogger
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

private val logger by getLogger()

suspend fun main() {
    val pokemonMap = Path("src/main/resources/pokemon_map.txt")

    pokemonMap.bufferedWriter().use { writer ->
        PokeApi.listPokemon().flowOn(Dispatchers.IO).map { it.fetchInfo() }.filterNot { it.id > 10_000 }.collect { info ->
            withContext(Dispatchers.IO) {

                if (info.id % 100 == 0)
                    logger.info { "Written ${info.id} entries" }

                writer.append(info.name)
                writer.appendLine()
            }
        }
    }
}
