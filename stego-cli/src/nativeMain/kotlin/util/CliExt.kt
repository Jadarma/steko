package io.github.jadarma.stego.cli.util

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
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
