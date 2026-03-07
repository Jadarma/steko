package io.github.jadarma.steko.cli.util

import io.github.jadarma.steko.core.Image
import kotlinx.cinterop.*
import stbImage.stbi_load_from_memory
import stbImage.stbi_write_png_to_mem

/**
 * Decodes an image from PNG format.
 *
 * @param data The contents of the PNG file.
 *
 * @throws IllegalArgumentException If the [data] is not a valid PNG.
 */
@OptIn(ExperimentalForeignApi::class)
fun Image.Companion.decodeFromPng(data: ByteArray): Image = memScoped {
    val width = alloc<IntVar>()
    val height = alloc<IntVar>()
    val channels = alloc<IntVar>()

    val image = stbi_load_from_memory(
        buffer = data.asUByteArray().toCValues(),
        len = data.size,
        x = width.ptr,
        y = height.ptr,
        channels_in_file = channels.ptr,
        desired_channels = 4,
    ) ?: throw IllegalArgumentException("Failed to decode PNG image.")

    Image.decodeFromRgba(
        data = image.readBytes(width.value * height.value * 4),
        size = width.value to height.value
    )
}

/** Encodes the image into a PNG file. */
@OptIn(ExperimentalForeignApi::class)
fun Image.encodeToPng(): ByteArray {
    val requiresAlphaChannel: Boolean = pixels.any { it and 0xFF != 0xFF }
    val channels = if (requiresAlphaChannel) 4 else 3

    val bytes = ByteArray(width * height * channels) { index ->
        val pixelValue = pixels[index / channels]
        val pixelChannel = index % channels
        pixelValue.ushr(Byte.SIZE_BITS * (3 - pixelChannel)).toByte()
    }

    return memScoped {
        val size = alloc<IntVar>()
        val pointer = stbi_write_png_to_mem(
            pixels = bytes.asUByteArray().toCValues(),
            stride_bytes = width * channels,
            x = width,
            y = height,
            n = channels,
            out_len = size.ptr,
        ) ?: error("Failed to write PNG to memory.")

        pointer.readBytes(size.value)
    }
}
