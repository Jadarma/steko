package io.github.jadarma.steko.core.internal

import kotlinx.io.Buffer
import kotlinx.io.RawSource

/**
 * A source that can extract the payload bytes from pixel data using the [steko] algorithm.
 * Note that this source does *not* perform decryption, it will only read the image as-is.
 */
internal class StekoSource(private val steko: StekoAlgorithm) : RawSource {

    private var index = 0L

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (index >= steko.capacity) return -1
        var newlyRead = 0L

        @Suppress("Unused")
        for (i in 0 until byteCount) {
            if (index >= steko.capacity) break
            sink.writeByte(readByte())
            newlyRead++
        }

        return newlyRead
    }

    /** Read the payload byte at the current [index] and increment the index. */
    private fun readByte(): Byte = with(steko) {
        var byte = 0
        for (bit in 0 until Byte.SIZE_BITS) {
            val bitIndex = index * Byte.SIZE_BITS + bit
            val pixelIndex = pixelOrder[(bitIndex / maskedBits.size).toInt()]
            val pixelBit = maskedBits[(bitIndex % maskedBits.size).toInt()]
            val pixelValue = pixels[pixelIndex]
            byte = (byte shl 1) or pixelValue.bitAt(pixelBit)
        }

        index++
        return byte.toByte()
    }

    /** Nothing to close. */
    override fun close() = Unit
}
