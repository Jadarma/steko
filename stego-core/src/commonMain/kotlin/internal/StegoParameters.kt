package io.github.jadarma.stego.core.internal

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographySystem
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.SHA256
import io.github.jadarma.stego.core.Key
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.write
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class StegoAlgorithm(
    val pixels: IntArray,
    val pixelOrder: IntArray,
    val bitmask: Int,
    val challenge: Int,
    encryptionKey: AES.GCM.Key,
    val random: Random = CryptographySystem.getDefaultRandom(),
) {
    val maskedBits: IntArray = maskedBitsOf(bitmask)
    val capacity: Int = capacityFor(pixels.size, bitmask)
    private val cipher = encryptionKey.cipher(tagSize = 128.bits)

    suspend fun hide(payload: ByteArray, rawPayload: Boolean = true, noise: Boolean = false) {
        val encrypted = cipher.encrypt(payload)
        val requiredCapacity = Int.SIZE_BYTES * 2 + encrypted.size
        if (requiredCapacity > capacity) {
            throw IndexOutOfBoundsException("(${requiredCapacity}B > ${capacity}B)")
        }

        if (noise) addNoise()

        val length = if (rawPayload) -encrypted.size else encrypted.size

        StegoSink(this).buffered().use { sink ->
            sink.writeInt(challenge)
            sink.writeInt(length)
            sink.write(encrypted)
        }
    }

    suspend fun show(): Pair<ByteArray, Boolean>? = runCatching {
        val source = StegoSource(this).buffered()
        check(source.readInt() == challenge) { "Challenge failed. Image does not contain payload for this key." }
        val length = source.readInt()
        val isRaw = length < 0
        val data = source.readByteArray(length.absoluteValue)
        cipher.decrypt(data) to isRaw
    }.getOrNull()

    /** Modifies the current image, adding random data over bits specified by the [bitmask]. */
    fun addNoise() {
        val invMask = bitmask.inv()
        for (index in pixels.indices) {
            pixels[index] = (pixels[index] and invMask) or (random.nextInt() and bitmask)
        }
    }

    companion object {

        suspend fun createFor(pixels: IntArray, key: Key): StegoAlgorithm {

            val aesKey = CryptographySystem.getDefaultProvider()
                .get(AES.GCM).keyDecoder()
                .decodeFromByteString(AES.Key.Format.RAW, key.bytes)

            val bitmask = Buffer().use { buffer ->
                buffer.write(key.bytes, 0, Int.SIZE_BYTES)
                buffer.readInt()
            }

            val sha256 = CryptographySystem.getDefaultProvider().get(SHA256).hasher()
            val digest = sha256.hash(key.bytes)

            val challenge = Buffer().use { buffer ->
                buffer.write(digest)
                IntArray(32 / Int.SIZE_BYTES) { buffer.readInt() }.reduce(Int::xor)
            }

            val pixelOrder = Buffer().use { buffer ->
                buffer.write(digest)
                val random = Xoshiro256PlusPlus(
                    s0 = buffer.readLong(),
                    s1 = buffer.readLong(),
                    s2 = buffer.readLong(),
                    s3 = buffer.readLong(),
                )
                shufflePixels(pixels.size, random)
            }

            return StegoAlgorithm(
                pixels = pixels,
                pixelOrder = pixelOrder,
                bitmask = bitmask,
                challenge = challenge,
                encryptionKey = aesKey,
                random = CryptographySystem.getDefaultRandom(),
            )
        }

        /**
         * Calculates the maximum number of bytes that can be stored in an image having this [pixelCount] using this
         * [bitmask].
         */
        fun capacityFor(pixelCount: Int, bitmask: Int): Int = minOf(
            pixelCount.toLong() * bitmask.countOneBits() / Byte.SIZE_BITS,
            Int.MAX_VALUE.toLong(),
        ).toInt()

        /**
         * Calculates the indices of the bits that are set in the key's bitmask.
         * The index counts from least to most significant, but they are given in order from most to least significant.
         * For example: `0x01010100` would be `[24, 16, 8]`.
         */
        fun maskedBitsOf(bitmask: Int): IntArray = IntArray(bitmask.countOneBits()).apply {
            var mask = bitmask
            var bitIndex = 0
            var index = size
            repeat(bitmask.countOneBits()) {
                val trailing = mask.countTrailingZeroBits() + 1
                mask = mask.ushr(trailing)
                bitIndex += trailing
                this[--index] = bitIndex - 1
            }
        }

        /**
         * Using the [random] instance, shuffle the pixel order of an image with [pixelCount] pixels.
         * Returns an array the same size as the image, containing the indexes of pixels to read in pseudo-random order.
         * Based on the [Fisher–Yates shuffle](https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm).
         */
        fun shufflePixels(pixelCount: Int, random: Random): IntArray {
            val indices = IntArray(pixelCount) { it }
            for (i in indices.lastIndex downTo 1) {
                val j = random.nextInt(i + 1)
                val tmp = indices[i]
                indices[i] = indices[j]
                indices[j] = tmp
            }
            return indices
        }
    }
}
