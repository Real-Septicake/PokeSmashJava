package io.github.septicake.cloud.preprocess

import org.incendo.cloud.component.preprocessor.ComponentPreprocessor
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult

class LengthMaxComponentPreprocessor<C>(private val length: Int) : ComponentPreprocessor<C> {
    override fun preprocess(context: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<Boolean> {
        val input = commandInput.peekString()
        if(input.length > length) {
            return ArgumentParseResult.failure(TooLongException(length, input))
        }
        return ArgumentParseResult.success(true)
    }

    class TooLongException(
        private val max: Int,
        input: String
    ) : IllegalArgumentException(input) {
        override val message: String
            get() = "Input is greater than maximum length of $max"
    }
}