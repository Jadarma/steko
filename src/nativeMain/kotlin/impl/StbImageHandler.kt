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
            desired_channels = 0,
        ) ?: error("Could not read image.")

        val dataLength = width.value * height.value * channels.value

        Image(
            width = width.value,
            height = height.value,
            hasAlphaChannel = channels.value == 4,
            data = image.readBytes(dataLength)
        )
    }

    override fun write(image: Image, path: String) = memScoped {
        val channels = if (image.hasAlphaChannel) 4 else 3
        stbi_write_png(
            filename = path,
            w = image.width,
            h = image.height,
            comp = channels,
            data = image.data.toCValues(),
            stride_in_bytes = image.width * channels
        )
        Unit
    }
}
