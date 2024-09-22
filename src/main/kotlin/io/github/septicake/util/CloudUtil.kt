package io.github.septicake.util

import org.incendo.cloud.parser.ArgumentParseResult

fun <T : Any> argumentParseFailure(failure: Throwable): ArgumentParseResult<T> = ArgumentParseResult.failure(failure)

fun <T : Any> argumentParseSuccess(result: T): ArgumentParseResult<T> = ArgumentParseResult.success(result)
