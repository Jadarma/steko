@file:OptIn(ExperimentalSerializationApi::class)

package io.github.jadarma.steggo.core

import io.github.jadarma.steggo.impl.Cryptography
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.random.Random
import kotlin.random.nextUInt

interface Stego {

    /**
     * Hide a payload in an image.
     *
     * @param image   The image to hide the payload in. The image will be modified in-place.
     * @param payload The payload to hide in the image.
     * @param bitmask The bits of the pixel value to overwrite with the payload.
     * @param noise   Whether to add random data in the unused pixels.
     *
     * @return The key needed to reveal the payload later.
     *
     * @throws IllegalArgumentException if the [bitmask] is invalid or the [payload] cannot fit in the [image].
     */
    fun hide(
        image: Image,
        payload: Payload,
        bitmask: UInt = 0x01010100u,
        noise: Boolean = true,
    ): Key

    /**
     * Attempt to extract a previously hidden payload.
     *
     * @param image The image containing a hidden payload.
     * @param key   The associated key that was generated when the file was hidden.
     *
     * @return The payload that was hidden, or `null` if the image didn't include one or the incorrect key was used.
     */
    fun reveal(image: Image, key: Key): Payload?

    companion object : Stego {

        override fun hide(image: Image, payload: Payload, bitmask: UInt, noise: Boolean): Key {
            require(bitmask.countOneBits() > 0) { "No bits specified for overwriting." }

            val encryptionKey = Cryptography.generateKey()
            val payloadBytes = payload.encodeToByteString(encryptionKey)
            val parameters = Parameters.generate(
                pixelCount = image.pixels.size,
                encryptionKey = encryptionKey,
                size = payloadBytes.size,
                type = PayloadType.of(payload),
                bitmask = bitmask,
                noise = noise,
            )

            hide(image.pixels, payloadBytes, parameters)
            return Key(parameters)
        }

        override fun reveal(image: Image, key: Key): Payload? = runCatching {
            val params = key.parameters
            val payloadBytes = reveal(image.pixels, params)
            Payload.decodeFromByteString(
                params.payloadType,
                payloadBytes,
                params.encryptionKey,
            )
        }.getOrNull()

        internal fun hide(pixels: UIntArray, payload: ByteString, params: Parameters) {
            val random = Random(params.seed)
            val maskedIndices = maskedBitsIndices(params.bitmask)
            val pixelOrder = pixels.indices.shuffled(random).toIntArray()

            for (bit in 0 until payload.size.toLong() * UByte.SIZE_BITS) {
                val pixelIndex = params.payloadOffset + (bit / maskedIndices.size).toInt()
                val pixelToUpdate = pixelOrder[pixelIndex]

                val pixelBitIndex = (bit % maskedIndices.size).toInt()
                val payloadIndex = (bit / UByte.SIZE_BITS).toInt()
                val payloadBitIndex = UByte.SIZE_BITS - 1 - (bit % UByte.SIZE_BITS).toInt()
                val bitToUpdate = maskedIndices[pixelBitIndex]
                val payloadBit = payload[payloadIndex].isBitSet(payloadBitIndex)
                val pixelValue = pixels[pixelToUpdate]

                pixels[pixelToUpdate] = when (payloadBit) {
                    true -> pixelValue.setBit(bitToUpdate)
                    false -> pixelValue.clearBit(bitToUpdate)
                }
            }

            if (!params.noise) return

            val invMask = params.bitmask.inv()
            val pixelsUsed = (payload.size.toLong() * Byte.SIZE_BITS / maskedIndices.size + 1).toInt()
            val afterPayload = minOf(pixels.size, params.payloadOffset + pixelsUsed)

            @Suppress("NOTHING_TO_INLINE")
            inline fun generateNoise(slice: IntRange) {
                for (index in slice) {
                    val pixelToUpdate = pixelOrder[index]
                    val original = pixels[pixelToUpdate]
                    pixels[pixelToUpdate] = (original and invMask) or (random.nextUInt() and params.bitmask)
                }
            }

            generateNoise(0 until params.payloadOffset)
            generateNoise(afterPayload..pixels.lastIndex)
        }

        internal fun reveal(pixels: UIntArray, params: Parameters): ByteString {
            val random = Random(params.seed)
            val maskedIndices = maskedBitsIndices(params.bitmask)
            val payloadSizeInPixels = (params.payloadSize.toLong() * Byte.SIZE_BITS / maskedIndices.size).toInt()
            val pixelOrder = pixels.indices
                .shuffled(random)
                .slice(params.payloadOffset..params.payloadOffset + payloadSizeInPixels)
                .toIntArray()

            return ByteArray(params.payloadSize) { byte ->
                var value = 0u
                repeat(Byte.SIZE_BITS) { bit ->
                    val bitIndex = byte.toLong() * Byte.SIZE_BITS + bit
                    val pixelIndex = (bitIndex / maskedIndices.size).toInt()
                    val pixelBit = maskedIndices[(bitIndex % maskedIndices.size).toInt()]
                    val original = pixels[pixelOrder[pixelIndex]]
                    value = (value shl 1) or original.bitAt(pixelBit)
                }
                value.toByte()
            }.let(::ByteString)
        }

        /**
         * Calculates the indices of the bits that are set in the [bitmask].
         * The index counts from least to most significant, but they are given in order from most to least significant.
         * For example: `0x01010100` would be `[24, 16, 8]`.
         */
        internal fun maskedBitsIndices(bitmask: UInt): IntArray = IntArray(bitmask.countOneBits()).apply {
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
