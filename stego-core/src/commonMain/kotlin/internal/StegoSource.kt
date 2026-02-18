package io.github.jadarma.stego.core.internal

import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import kotlinx.io.Buffer
import kotlinx.io.RawSource

/**
 * A source that can extract the payload bytes from an [image] with a given key.
 * Note that this source does *not* perform decryption, it will only read the image as-is.
 *
 * @param image The carrier image to extract from.
 * @param key   The key to extract with.
 */
internal class StegoSource(private val image: Image, key: Key) : RawSource {

    private val pixelOrder = image.shufflePixels(key)
    private val maskedBits = key.maskedBits()
    private val capacity = image.capacity(key).toLong()
    private var index = 0L

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (index >= capacity) return -1
        var newlyRead = 0L

        @Suppress("Unused")
        for (i in 0 until byteCount) {
            if (index >= capacity) break
            sink.writeByte(readByte())
            newlyRead++
        }

        return newlyRead
    }

    /** Read the payload byte at the current [index] and increment the index. */
    private fun readByte(): Byte {
        var byte = 0u
        for (bit in 0 until Byte.SIZE_BITS) {
            val bitIndex = index * Byte.SIZE_BITS + bit
            val pixelIndex = pixelOrder[(bitIndex / maskedBits.size).toInt()]
            val pixelBit = maskedBits[(bitIndex % maskedBits.size).toInt()]
            val pixelValue = image.pixels[pixelIndex]
            byte = (byte shl 1) or pixelValue.bitAt(pixelBit)
        }

        index++
        return byte.toByte()
    }

    /** Nothing to close. */
    override fun close() = Unit
}
