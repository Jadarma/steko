package io.github.jadarma.stego.cli.util

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.OptionDelegate
import com.github.ajalt.clikt.parameters.options.OptionTransformContext
import com.github.ajalt.clikt.parameters.options.OptionValidator
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.toCValues
import platform.posix.write

/**
 * Shorthand for exiting the program in a controlled fashion while providing a reason for the error.
 * Prints the [message] to _STDERR_ and exits the program with the non-zero [statusCode].
 */
context(command: BaseCliktCommand<*>)
fun exitError(message: String, statusCode: Int = 1): Nothing {
    command.echo(message, err = true)
    throw ProgramResult(statusCode.coerceAtLeast(1))
}

/** Prints the raw [data] to _STDOUT_. */
@OptIn(ExperimentalForeignApi::class)
context(_: BaseCliktCommand<*>)
fun printToStdOut(data: ByteArray) {
    write(1, data.toCValues(), data.size.convert())
}

/**
 * The [OptionTransformContext] provides a custom implementation for `require`, but it makes it difficult to use any
 * helper functions that don't have access to the same context. This function considers any error inside the [validator]
 * as expected and wraps them in usage errors, like `.convert()` does. */
inline fun <AllT, EachT, ValueT> OptionWithValues<AllT, EachT, ValueT>.validateCatching(
    crossinline validator: OptionValidator<AllT & Any>,
): OptionDelegate<AllT> = copy(
    transformValue,
    transformEach,
    transformAll,
    validator = {
        if (it != null) runCatching { validator(it) }.getOrElse { cause ->
            throw BadParameterValue(cause.message.orEmpty(), option)
        }
    },
)
