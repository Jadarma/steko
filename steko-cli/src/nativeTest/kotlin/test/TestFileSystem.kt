package io.github.jadarma.steko.cli.test

import com.goncalossilva.resources.Resource
import io.github.jadarma.steko.cli.util.FileSystem
import kotlinx.io.IOException
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlin.test.fail

/**
 * Fakes a filesystem interface for running commands in tests without altering the actual filesystem.
 *
 * The structure is as follows:
 * - `/images` is a read-write directory that proxies to test resources `/images`. Also, the `cwd`.
 * - `/data` is a read-only directory that proxies to test resources `/data`
 * - `/err` is a private directory that will throw IO exception for both read and writes.
 * - `/out` is a read-write directory.
 */
class TestFileSystem : FileSystem {

    private val filesRead: MutableSet<Path> = mutableSetOf()
    private val filesWritten: MutableMap<Path, ByteArray> = mutableMapOf()

    private val Path.normalized: Path get() = when {
        isAbsolute -> this
        else -> Path("/images/" + toString().removePrefix("./"))
    }

    override fun stat(path: Path): FileMetadata? = when (val p = path.normalized) {
        in directories -> FileMetadata(isDirectory = true)
        in filesWritten -> FileMetadata(isRegularFile = true, size = filesWritten.getValue(p).size.toLong())
        in originalFiles -> FileMetadata(isRegularFile = true, size = originalFiles.getValue(p).size.toLong())
        else -> null
    }

    override fun read(path: Path): ByteArray {
        val p = path.normalized
        return when {
            p in directories -> throw IOException("Cannot read a directory: $path")
            p.toString().startsWith("/err") -> throw IOException("Filesystem error for $path")
            p in filesWritten -> filesWritten.getValue(p).also { filesRead.add(p) }
            p in originalFiles -> originalFiles.getValue(p).also { filesRead.add(p) }
            else -> throw FileNotFoundException("File not found: $path")
        }
    }

    override fun write(path: Path, data: ByteArray) {
        val p = path.normalized
        when {
            p in directories -> throw IOException("Cannot write a directory: $path")
            p.toString().startsWith("/err") -> throw IOException("I/O error for: $path")
            p.toString().startsWith("/data") -> throw IOException("Directory is read-only: $path")
            else -> filesWritten[p] = data
        }
    }

    fun shouldBeUnmodified() {
        if (filesWritten.isEmpty()) return
        fail("Expected filesystem to not be written to, but files were modified: $filesWritten")
    }

    fun shouldHaveWritten(path: Path, expected: ByteArray) {
        val p = path.normalized
        if (path !in filesWritten) {
            fail("Expected $path to be written to but was not.")
        }
        if ((filesWritten.getValue(path) contentEquals expected).not()) {
            fail("Actual file contents at $path differ from expected.")
        }
    }

    private companion object {
        val directories = listOf("/images", "/data", "/out").map(::Path).toSet()
        val originalFiles: Map<Path, ByteArray> = listOf(
            "/images/image.png",
            "/images/image.rgba",
        ).associate { path -> Path(path) to Resource(path.removePrefix("/")).readBytes() }
    }
}
