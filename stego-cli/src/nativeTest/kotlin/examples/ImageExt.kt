package io.github.jadarma.stego.cli.examples

import com.goncalossilva.resources.Resource
import io.github.jadarma.stego.core.Image
import io.kotest.assertions.fail
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import stbImage.stbi_load_from_memory

@OptIn(ExperimentalForeignApi::class)
fun Image.Companion.loadResource(path: String): Image {
    val imageData = Resource(path)
        .also { if (!it.exists()) fail("Resource $path not found") }
        .readBytes()

    return memScoped {
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val channels = alloc<IntVar>()

        val image = stbi_load_from_memory(
            buffer = imageData.asUByteArray().toCValues(),
            len = imageData.size,
            x = width.ptr,
            y = height.ptr,
            channels_in_file = channels.ptr,
            desired_channels = 4,
        ) ?: fail("Failed to decode image.")

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
}
