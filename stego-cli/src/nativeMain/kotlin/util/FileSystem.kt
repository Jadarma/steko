package io.github.jadarma.stego.cli.util

import kotlinx.io.buffered
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Very rudimentary abstraction of the already rudimentary abstraction.
 * This is because the original FileSystem is a sealed class.
 */
interface FileSystem {

    /** Returns metadata about the file at the given [path], or `null` if it could not be fetched.*/
    fun stat(path: Path): FileMetadata?

    /**
     * Returns the contents of the file at the given [path].
     *
     * @throws kotlinx.io.files.FileNotFoundException If file could not be found.
     * @throws kotlinx.io.IOException When cannot read file, or path is a directory.
     */
    fun read(path: Path): ByteArray

    /**
     * Writes the [data] in the file at the given [path], creating it if not existing, and overwriting it otherwise.
     *
     * @throws kotlinx.io.IOException When the file cannot be opened for writing.
     */
    fun write(path: Path, data: ByteArray)
}

/** The real filesystem used in production. */
object RealFileSystem : FileSystem {
    override fun stat(path: Path): FileMetadata? =
        SystemFileSystem.metadataOrNull(path)

    override fun read(path: Path): ByteArray =
        SystemFileSystem.source(path).buffered().use { it.readByteArray() }

    override fun write(path: Path, data: ByteArray) =
        SystemFileSystem.sink(path, append = false).buffered().use {
            it.write(data)
            it.flush()
        }
}

/**
 * Verifies that the [path] exists and is a _(regular)_ file.
 *
 * @throws IllegalArgumentException If the path doesn't exist or is not a file.
 */
fun FileSystem.checkFile(path: Path) {
    val meta = stat(path)
    require(meta != null) { "File does not exist: $path" }
    require(meta.isRegularFile) { "Expected file path is not a file: $path" }
}

/**
 * Verifies that the [path] exists and is a directory.
 *
 * @throws IllegalArgumentException If the path doesn't exist or is not a directory.
 */
fun FileSystem.checkDirectory(path: Path) {
    val meta = stat(path)
    require(meta != null) { "Directory does not exist: $path" }
    require(meta.isDirectory) { "Expected directory path is a file: $path" }
}

/** Gets the filename extension or `null` if is not included in the name. */
val Path.extension: String?
    get() = name
        .substringAfterLast('.', "")
        .lowercase()
        .takeIf(CharSequence::isNotEmpty)
