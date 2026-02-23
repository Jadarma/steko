package io.github.jadarma.stego.cli.util

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import io.github.jadarma.stego.core.Image
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Loads and decodes the image from the given [path] on disk.
 *
 * @throws ProgramResult If the data was not successfully loaded for any reason (also reports the error to _STDOUT_).
 */
@OptIn(ExperimentalForeignApi::class)
context(_: BaseCliktCommand<*>)
fun Image.Companion.load(path: Path): Image = runCatching {
    when (val extension = path.extension) {
        "rgba" -> Image.decodeFromRgba(SystemFileSystem.readFile(path))
        "png" -> Image.decodeFromPng(SystemFileSystem.readFile(path))
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
fun Image.write(path: Path): Unit = runCatching {
    val encoded = when (val extension = path.extension) {
        "rgba" -> this.encodeToRgba()
        "png" -> this.encodeToPng()
        else -> exitError("Unknown image extension: $extension")
    }
    SystemFileSystem.writeFile(path, encoded)
}.getOrElse { exitError("Could not write image: $path") }
