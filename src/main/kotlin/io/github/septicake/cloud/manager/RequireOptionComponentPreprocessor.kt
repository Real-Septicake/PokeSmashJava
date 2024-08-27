package io.github.septicake.cloud.manager

import org.incendo.cloud.component.preprocessor.ComponentPreprocessor
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult

class RequireOptionComponentPreprocessor<C>(
    private val options: Array<out String>
): ComponentPreprocessor<C> {
    override fun preprocess(context: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<Boolean> {
        val input: String = commandInput.peekString()
        if(input !in options) {
            return ArgumentParseResult.failure(NotInOptionsException(
                input, options
            ))
        }
        return ArgumentParseResult.success(true)
    }

    class NotInOptionsException(
        private val input: String,
        private val options: Array<out String>
    ) : IllegalArgumentException(input) {
        override val message: String
            get() = "input `$input` not in options [${options.reduce { acc, s -> "$acc, $s" }}]"
    }
}