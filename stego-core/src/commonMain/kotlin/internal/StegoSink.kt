package io.github.jadarma.stego.core.internal

import kotlinx.io.Buffer
import kotlinx.io.RawSink

/**
 * A sink that can write bytes to pixel data using the [stego] algorithm.
 * Note that this source does *not* perform encryption or prepend headers, it will only write the data as-is.
 */
internal class StegoSink(private val stego: StegoAlgorithm) : RawSink {

    private var index = 0L

    override fun write(source: Buffer, byteCount: Long) {
        if (index >= stego.capacity || byteCount < 0) return
        @Suppress("unused")
        for (i in 0 until minOf(byteCount, stego.capacity - index)) {
            writeByte(source.readByte())
        }
    }

    private fun writeByte(value: Byte) = with(stego) {
        for (bit in 0 until Byte.SIZE_BITS) {
            val bitIndex = index * Byte.SIZE_BITS + bit
            val pixelIndex = pixelOrder[(bitIndex / maskedBits.size).toInt()]
            val pixelBit = maskedBits[(bitIndex % maskedBits.size).toInt()]
            val pixelValue = pixels[pixelIndex]

            pixels[pixelIndex] = when (value.isBitSet(Byte.SIZE_BITS - 1 - bit)) {
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
