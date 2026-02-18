package io.github.jadarma.stego.cli.util

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

/**
 * Reads all bytes contained in the file at the given [path].
 *
 * @throws ProgramResult If the path is a directory, not a regular file, or cannot be stored in a [ByteArray]
 *                       (also reports the error to _STDOUT_).
 */
context(_: BaseCliktCommand<*>)
fun FileSystem.readFile(path: Path): ByteArray {
    val meta = metadataOrNull(path) ?: exitError("Cannot read file: $path")
    if (meta.isDirectory) exitError("Expected file path is a directory: $path")
    if (meta.isRegularFile.not()) exitError("Expected file path is not a regular file: $path")
    if (meta.size >= Int.MAX_VALUE) exitError("File exceeds 2GB: $path")

    return runCatching {
        source(path).buffered().use {
            it.readByteArray(meta.size.toInt())
        }
    }.getOrElse { exitError("Could not read file: $path") }
}

/**
 * Attempts to write all the [data] into a file at a given [path].
 * If the file does not exist, it will be created, if the file already exists, it will be overwritten.
 *
 * @throws ProgramResult If the data was not successfully written for any reason (also reports the error to _STDOUT_).
 */
context(_: BaseCliktCommand<*>)
fun FileSystem.writeFile(path: Path, data: ByteArray) {
    runCatching {
        sink(path, append = false).buffered().use {
            it.write(data)
            it.flush()
        }
    }.onFailure {
        exitError("Could not write file to: $path")
    }
}

/**
 * Verifies that the [path] exists and is a _(regular)_ file.
 *
 * @throws IllegalArgumentException If the path doesn't exist or is not a file.
 */
fun FileSystem.checkFile(path: Path) {
    val meta = metadataOrNull(path)
    require(meta != null) { "File does not exist: $path" }
    require(meta.isRegularFile) { "Expected file path is not a file: $path" }
}

/**
 * Verifies that the [path] exists and is a directory.
 *
 * @throws IllegalArgumentException If the path doesn't exist or is not a directory.
 */
fun FileSystem.checkDirectory(path: Path) {
    val meta = metadataOrNull(path)
    require(meta != null) { "Directory does not exist: $path" }
    require(meta.isDirectory) { "Expected directory path is a file: $path" }
}
