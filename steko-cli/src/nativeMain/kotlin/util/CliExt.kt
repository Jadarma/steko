package io.github.jadarma.steko.cli.util

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgValidator
import com.github.ajalt.clikt.parameters.arguments.ArgumentDelegate
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.ProcessedArgument
import com.github.ajalt.clikt.parameters.options.OptionDelegate
import com.github.ajalt.clikt.parameters.options.OptionTransformContext
import com.github.ajalt.clikt.parameters.options.OptionValidator
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import io.github.jadarma.steko.core.Image
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.toCValues
import kotlinx.io.files.Path
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
 * as expected and wraps them in usage errors, like `.convert()` does.
 */
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

/**
 * The [ArgumentTransformContext] provides a custom implementation for `require`, but it makes it difficult to use any
 * helper functions that don't have access to the same context. This function considers any error inside the [validator]
 * as expected and wraps them in usage errors, like `.convert()` does.
 */
inline fun <AllT, ValueT> ProcessedArgument<AllT, ValueT>.validateCatching(
    crossinline validator: ArgValidator<AllT & Any>,
): ArgumentDelegate<AllT> = copy(
    validator = {
        if (it != null) runCatching { validator(it) }.getOrElse { cause ->
            throw BadParameterValue(cause.message.orEmpty(), argument)
        }
    }
)

/**
 * Reads all bytes contained in the file at the given [path].
 *
 * @throws ProgramResult If the path is a directory, not a regular file, or cannot be stored in a [ByteArray]
 *                       (also reports the error to _STDOUT_).
 */
context(_: BaseCliktCommand<*>)
fun FileSystem.readFile(path: Path): ByteArray {
    val meta = stat(path) ?: exitError("Cannot read file: $path")
    if (meta.isDirectory) exitError("Expected file path is a directory: $path")
    if (meta.isRegularFile.not()) exitError("Expected file path is not a regular file: $path")
    if (meta.size >= Int.MAX_VALUE) exitError("File exceeds 2GB: $path")
    return runCatching { read(path) }.getOrElse { exitError("Could not read file: $path") }
}

/**
 * Attempts to write all the [data] into a file at a given [path].
 * If the file does not exist, it will be created, if the file already exists, it will be overwritten.
 *
 * @throws ProgramResult If the data was not successfully written for any reason (also reports the error to _STDOUT_).
 */
context(_: BaseCliktCommand<*>)
fun FileSystem.writeFile(path: Path, data: ByteArray) {
    runCatching { write(path, data) }.onFailure { exitError("Could not write file to: $path") }
}

/**
 * Loads and decodes the image from the given [path] on disk.
 *
 * @throws ProgramResult If the data was not successfully loaded for any reason (also reports the error to _STDOUT_).
 */
@OptIn(ExperimentalForeignApi::class)
context(_: BaseCliktCommand<*>)
fun Image.Companion.load(fileSystem: FileSystem, path: Path): Image = runCatching {
    when (val extension = path.extension) {
        "rgba" -> Image.decodeFromRgba(fileSystem.readFile(path))
        "png" -> Image.decodeFromPng(fileSystem.readFile(path))
        else -> exitError("Unknown image extension: $extension")
    }
}.getOrElse { exitError("Could not load image: $path") }

/**
 * Encodes and writes this image to the given [path] on disk.
 *
 * @throws ProgramResult If the data was not successfully written for any reason (also reports the error to _STDOUT_).
 */
@OptIn(ExperimentalForeignApi::class)
context(_: BaseCliktCommand<*>)
fun Image.write(fileSystem: FileSystem, path: Path): Unit = runCatching {
    val encoded = when (val extension = path.extension) {
        "rgba" -> this.encodeToRgba()
        "png" -> this.encodeToPng()
        else -> exitError("Unknown image extension: $extension")
    }
    fileSystem.writeFile(path, encoded)
}.getOrElse { exitError("Could not write image: $path") }
