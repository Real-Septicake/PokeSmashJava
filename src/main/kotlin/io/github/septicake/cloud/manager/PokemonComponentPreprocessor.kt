package io.github.septicake.cloud.manager

import io.github.septicake.PokeSmashBot
import org.incendo.cloud.component.preprocessor.ComponentPreprocessor
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult

class PokemonComponentPreprocessor<C>(
    val bot: PokeSmashBot
) : ComponentPreprocessor<C> {
    override fun preprocess(context: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<Boolean> {
        val input = commandInput.peekString()

        try {
            val num = input.toInt()
            bot.map[num]
                ?: return ArgumentParseResult.failure(
                    InvalidNumberException(input, bot.map.size)
                )

            return ArgumentParseResult.success(true)
        } catch(e : NumberFormatException) {
            bot.map.inverse()[input.lowercase()]
                ?: return ArgumentParseResult.failure(
                    InvalidPokemonNameException(input)
                )

            return ArgumentParseResult.success(true)
        }
    }

    class InvalidNumberException(
        private val input: String,
        private val maxPokemon: Int
    ) : IllegalArgumentException(input) {
        override val message: String
            get() = "ID `$input` is not a valid number (valid range is 1 to $maxPokemon)"
    }

    class InvalidPokemonNameException(
        private val input: String
    ) : IllegalArgumentException(input) {
        override val message: String
            get() = "Unrecognized pokemon: `$input`"
    }
}