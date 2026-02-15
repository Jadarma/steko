package io.github.jadarma.steggo.cli.util

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import io.github.jadarma.steggo.core.Image
import kotlinx.cinterop.*
import kotlinx.io.files.Path
import stbImage.stbi_load
import stbImage.stbi_write_png

/**
 * Loads and decodes the image from the given [path] on disk.
 *
 * @throws ProgramResult If the data was not successfully loaded for any reason (also reports the error to _STDOUT_).
 */
@OptIn(ExperimentalForeignApi::class)
context(_: BaseCliktCommand<*>)
fun Image.Companion.load(path: Path): Image = runCatching {
    memScoped {
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val channels = alloc<IntVar>()

        val image = stbi_load(
            filename = path.toString(),
            x = width.ptr,
            y = height.ptr,
            channels_in_file = channels.ptr,
            desired_channels = 4,
        ) ?: exitError("Could not load image: $path")

        val pixelCount = width.value * height.value
        val data = image.readBytes(pixelCount * 4).asUByteArray()

        val pixels = UIntArray(pixelCount) { index ->
            var value = 0u
            repeat(4) { channel ->
                val channelValue = data[index * 4 + channel].toUInt()
                value = value.shl(UByte.SIZE_BITS).or(channelValue)
            }
            value
        }

        Image(
            width = width.value,
            height = height.value,
            pixels = pixels,
        )
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
    val requiresAlphaChannel: Boolean = pixels.any { it and 0xFFu != 0xFFu }
    val channels = if (requiresAlphaChannel) 4 else 3

    val bytes = UByteArray(width * height * channels) { index ->
        val pixelValue = pixels[index / channels]
        val pixelChannel = index % channels
        pixelValue.shr(UByte.SIZE_BITS * (3 - pixelChannel)).toUByte()
    }

    memScoped {
        stbi_write_png(
            filename = path.toString(),
            w = width,
            h = height,
            comp = channels,
            data = bytes.toCValues(),
            stride_in_bytes = width * channels,
        )
    }

    Unit
}.getOrElse { exitError("Could not write image to file: $path") }
