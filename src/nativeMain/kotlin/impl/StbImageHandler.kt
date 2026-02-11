@file:OptIn(ExperimentalForeignApi::class)

package io.github.jadarma.steggo.impl

import io.github.jadarma.steggo.core.Image
import io.github.jadarma.steggo.core.ImageHandler
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import stbImage.stbi_load
import stbImage.stbi_write_png

object StbImageHandler : ImageHandler {

    override fun read(path: String): Image = memScoped {
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val channels = alloc<IntVar>()

        val image = stbi_load(
            filename = path,
            x = width.ptr,
            y = height.ptr,
            channels_in_file = channels.ptr,
            desired_channels = 4,
        ) ?: error("Could not read image.")

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

    override fun write(image: Image, path: String) = memScoped {
        val requiresAlphaChannel: Boolean = image.pixels.any { it and 0xFFu != 0xFFu }
        val channels = if (requiresAlphaChannel) 4 else 3

        val bytes = UByteArray(image.width * image.height * channels) { index ->
            val pixelValue = image.pixels[index / channels]
            val pixelChannel = index % channels
            pixelValue.shr(UByte.SIZE_BITS * (3 - pixelChannel)).toUByte()
        }

        stbi_write_png(
            filename = path,
            w = image.width,
            h = image.height,
            comp = channels,
            data = bytes.toCValues(),
            stride_in_bytes = image.width * channels,
        )
        Unit
    }
}
