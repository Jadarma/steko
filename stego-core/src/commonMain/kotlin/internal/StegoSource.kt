package io.github.jadarma.stego.core.internal

import kotlinx.io.Buffer
import kotlinx.io.RawSource

/**
 * A source that can extract the payload bytes from pixel data using the [stego] algorithm.
 * Note that this source does *not* perform decryption, it will only read the image as-is.
 */
internal class StegoSource(private val stego: StegoAlgorithm) : RawSource {

    private var index = 0L

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (index >= stego.capacity) return -1
        var newlyRead = 0L

        @Suppress("Unused")
        for (i in 0 until byteCount) {
            if (index >= stego.capacity) break
            sink.writeByte(readByte())
            newlyRead++
        }

        return newlyRead
    }

    /** Read the payload byte at the current [index] and increment the index. */
    private fun readByte(): Byte = with(stego) {
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
