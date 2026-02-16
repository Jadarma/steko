package io.github.jadarma.stego.core

import dev.whyoleg.cryptography.CryptographySystem
import kotlinx.io.*
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * An image loaded in memory.
 * @property width The width of the image in pixels.
 * @property height The width of the image in pixels.
 * @property pixels The pixel values in RGBA format.
 */
class Image(
    val width: Int,
    val height: Int,
    val pixels: UIntArray,
) {
    init {
        require(pixels.size == width * height) { "Pixel count does not match resolution." }
    }

    /**
     * Hides the [payload] inside this image, altering its pixels.
     * If you would rather keep the original, chain a [copy] call beforehand.
     *
     * @param key     The key which should be used to retrieve this later.
     * @param payload The bytes to hide in the image.
     * @param noise   Whether to encode random data in _all_ pixels according to the [key]'s bitmask.
     *                This can make the image blend in better, and erases all previous messages with the same mask.
     *                Enabled by default.
     *
     * @throws IndexOutOfBoundsException If the payload cannot fit inside this image.
     * @return This same instance.
     */
    fun hide(key: Key, payload: ByteArray, noise: Boolean = true): Image {
        val encrypted = key.aesKey.cipher().encryptBlocking(payload)
        if (HEADER_SIZE_BYTES + encrypted.size > capacityFor(key.bitmask)) {
            throw IndexOutOfBoundsException("Payload too large to fit in the image.")
        }

        if (noise) noise(key.bitmask)

        stegoWrite(key).buffered().use { buffer ->
            buffer.writeInt(key.challenge)
            buffer.writeInt(encrypted.size)
            buffer.write(encrypted)
            buffer.flush()
        }

        return this
    }

    /**
     * Attempt to use the [key] to extract a payload.
     * If the key does not fit, returns `null` instead.
     * Otherwise, returns the bytes that were previously hidden.
     */
    fun show(key: Key): ByteArray? = runCatching {
        key.aesKey.cipher()
            .decryptingSource(stegoRead(key))
            .buffered()
            .readByteArray()
    }.getOrNull()

    /** Modifies the current image, adding random data over bits specified by the [bitmask]. */
    private fun noise(bitmask: UInt) {
        val invMask = bitmask.inv()
        val random = CryptographySystem.getDefaultRandom()
        for (index in pixels.indices) {
            pixels[index] = (pixels[index] and invMask) or (random.nextUInt() and bitmask)
        }
    }

    /** Create a new image with the current data. */
    fun copy(): Image = Image(width, height, pixels.copyOf())

    /**
     * Extracts the raw, encrypted payload associated to the [key].
     * If the preflight check did not pass, returns an empty source.
     */
    private fun stegoRead(key: Key): RawSource = object : RawSource {

        private val size: Long

        private var index = 0L

        /** The bit indices of the RGBA pixel that should be extracted, in order. */
        private val bitIndices: IntArray = maskedBitsIndices(key.bitmask)

        /** The order the pixels should be in to read the image correctly. */
        private val pixelOrder: IntArray = shufflePixels(pixels.size, key.seed)

        init {
            val capacity = capacityFor(key.bitmask)
            size = if (capacity < HEADER_SIZE_BYTES) 0L else {
                val buffer = Buffer()
                repeat(HEADER_SIZE_BYTES) { buffer.writeByte(readByte()) }
                val foundChallenge = buffer.readInt()
                val encryptedLength = buffer.readInt()
                    .takeIf { foundChallenge == key.challenge }
                    ?.takeIf { it < capacity - HEADER_SIZE_BYTES }
                    ?.toLong()
                    ?: 0L

                HEADER_SIZE_BYTES + encryptedLength
            }
        }

        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
            if (index >= size) return -1
            var newlyRead = 0L

            @Suppress("Unused")
            for (i in 0 until byteCount) {
                if (index >= size) break
                sink.writeByte(readByte())
                newlyRead++
            }

            return newlyRead
        }

        private fun readByte(): Byte {
            var byte = 0u

            for (bit in 0 until Byte.SIZE_BITS) {
                val bitIndex = index * Byte.SIZE_BITS + bit
                val pixelIndex = pixelOrder[(bitIndex / bitIndices.size).toInt()]
                val pixelBit = bitIndices[(bitIndex % bitIndices.size).toInt()]
                val pixelValue = pixels[pixelIndex]
                byte = (byte shl 1) or pixelValue.bitAt(pixelBit)
            }

            index++
            return byte.toByte()
        }

        /** This source doesn't need to be closed. */
        override fun close() = Unit
    }

    private fun stegoWrite(key: Key): RawSink = object : RawSink {

        /** The bit indices of the RGBA pixel that should be extracted, in order. */
        private val bitIndices: IntArray = maskedBitsIndices(key.bitmask)

        /** The order the pixels should be in to read the image correctly. */
        private val pixelOrder: IntArray = shufflePixels(pixels.size, key.seed)

        override fun close() = Unit

        override fun flush() = Unit

        val capacity = capacityFor(key.bitmask).toLong()

        var index = 0L
        val remaining: Long get() = capacity - index - 1

        override fun write(source: Buffer, byteCount: Long) {
            if (remaining < 0) return
            val bytesToRead = minOf(byteCount, remaining).toInt()
            for (byte in source.readByteArray(bytesToRead)) {
                writeByte(byte)
            }
        }

        private fun writeByte(value: Byte) {
            for (bit in 0 until Byte.SIZE_BITS) {
                val bitIndex = index * Byte.SIZE_BITS + bit
                val pixelIndex = pixelOrder[(bitIndex / bitIndices.size).toInt()]
                val pixelBit = bitIndices[(bitIndex % bitIndices.size).toInt()]
                val pixelValue = pixels[pixelIndex]

                pixels[pixelIndex] = when (value.isBitSet(UByte.SIZE_BITS - 1 - bit)) {
                    true -> pixelValue.setBit(pixelBit)
                    false -> pixelValue.clearBit(pixelBit)
                }
            }
            index++
        }
    }

    /** Returns the maximum capacity this image has for the [bitmask]. */
    private fun capacityFor(bitmask: UInt): Int = minOf(
        pixels.size.toLong() * bitmask.countOneBits() / Byte.SIZE_BITS,
        Int.MAX_VALUE.toLong(),
    ).toInt()

    companion object {

        /**
         * Images
         */
        private const val HEADER_SIZE_BYTES = Int.SIZE_BYTES * 2

        /**
         * Calculates the indices of the bits that are set in the [bitmask].
         * The index counts from least to most significant, but they are given in order from most to least significant.
         * For example: `0x01010100` would be `[24, 16, 8]`.
         */
        private fun maskedBitsIndices(bitmask: UInt): IntArray = IntArray(bitmask.countOneBits()).apply {
            var mask = bitmask
            var bitIndex = 0
            var index = size
            repeat(bitmask.countOneBits()) {
                val trailing = mask.countTrailingZeroBits() + 1
                mask = mask.shr(trailing)
                bitIndex += trailing
                this[--index] = bitIndex - 1
            }
        }

        private fun shufflePixels(size: Int, seed: Long): IntArray {
            val indices = IntArray(size) { it }
            indices.shuffle(Random(seed))
            return indices
        }

        /** Returns the number equal to this binary representation with the bit at [index] as `1`. */
        private fun UInt.setBit(index: Int): UInt = this or (1u shl index)

        /** Returns the number equal to this binary representation with the bit at [index] as `0`. */
        private fun UInt.clearBit(index: Int): UInt = this and (1u shl index).inv()

        /** Returns whether the bit at [index] is set. */
        private fun Byte.isBitSet(index: Int): Boolean = this.toUInt() and (1u shl index) > 0u

        /** Returns either `1u` or `0u` depending on whether the bit at [index] is set. */
        private fun UInt.bitAt(index: Int): UInt = (this shr index) and 1u
    }
}
