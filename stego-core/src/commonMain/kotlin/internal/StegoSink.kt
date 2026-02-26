package io.github.jadarma.stego.core.internal

import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import kotlinx.io.Buffer
import kotlinx.io.RawSink

/**
 * A sink that can write bytes to an [image] with a given key.
 * Note that this source does *not* perform encryption or prepend headers, it will only write the data as-is.
 *
 * @param image The carrier image to write to.
 * @param key   The key to embed with.
 */
internal class StegoSink(private val image: Image, key: Key) : RawSink {

    private val pixelOrder = image.shufflePixels(key)
    private val maskedBits = key.maskedBits()
    private val capacity = image.capacity(key)

    private var index = 0L

    override fun write(source: Buffer, byteCount: Long) {
        if (index >= capacity || byteCount < 0) return
        @Suppress("unused")
        for (i in 0 until minOf(byteCount, capacity - index)) {
            writeByte(source.readByte())
        }
    }

    private fun writeByte(value: Byte) {
        for (bit in 0 until Byte.SIZE_BITS) {
            val bitIndex = index * Byte.SIZE_BITS + bit
            val pixelIndex = pixelOrder[(bitIndex / maskedBits.size).toInt()]
            val pixelBit = maskedBits[(bitIndex % maskedBits.size).toInt()]
            val pixelValue = image.pixels[pixelIndex]

            image.pixels[pixelIndex] = when (value.isBitSet(Byte.SIZE_BITS - 1 - bit)) {
                true -> pixelValue.setBit(pixelBit)
                false -> pixelValue.clearBit(pixelBit)
            }
        }
        index++
    }

    /** Nothing to close. */
    override fun close() = Unit

    /** Nothing to flush. */
    override fun flush() = Unit
}
