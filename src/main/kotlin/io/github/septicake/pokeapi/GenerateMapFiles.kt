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
        PokeApi.listPokemon().flowOn(Dispatchers.IO).map { it.fetchInfo() }.filterNot {
            if (it.id > 10_000) {
                logger.info { "Filtered '${it.name}' (${it.id})" }
                return@filterNot true
            }
            return@filterNot false

        }.collect { info ->
            withContext(Dispatchers.IO) {
                if (info.id % 100 == 0)
                    logger.info { "Written ${info.id} pokemon" }

                writer.append(info.name)
                writer.appendLine()
            }
        }
    }

    val speciesMap = Path("src/main/resources/species_map.txt")

    speciesMap.bufferedWriter().use { writer ->
        PokeApi.listSpecies().flowOn(Dispatchers.IO).map { it.fetchInfo() }
        .collect { info ->
            withContext(Dispatchers.IO) {
                if(info.id % 100 == 0)
                    logger.info { "Written ${info.id} species" }

                writer.append(info.name)
                writer.appendLine()
            }
        }
    }
}
