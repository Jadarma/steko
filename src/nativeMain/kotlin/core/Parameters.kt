package io.github.jadarma.steggo.core

import io.github.jadarma.steggo.impl.Cryptography
import kotlinx.io.bytestring.ByteString
import kotlin.math.ceil

/**
 * The parameters needed to encode or decode a payload into and from an image.
 *
 * @property seed          The seed used for the pixel order randomization.
 * @property encryptionKey The `AES-GCM` key used to encrypt or decrypt the payload.
 * @property bitmask       A mask for an RGBA pixel, set bits will overwrite the original image value with bits from
 *                         the payload.
 *                         It is recommended to use the least significant bits to make it more stealthy and not use the
 *                         alpha channel, especially if the original image doesn't make use of transparency.
 *                         One can also use many or all bits, leading to chaos resembling glitch art.
 * @property noise         Whether to encode random data in the pixels not used to encode the payload.
 *                         This attempts to explain the imperfections caused by the payload bitmask as random noise in
 *                         the host image.
 *                         Not relevant for decoding.
 * @property payloadOffset How many pixels to drop from the shuffled sequence before starting reading the payload.
 * @property payloadSize   The payload size, in bytes.
 * @property payloadType   The type of payload to read from the image.
 */
data class Parameters(
    val seed: Long,
    val encryptionKey: ByteString,
    val bitmask: UInt,
    val noise: Boolean,
    val payloadOffset: Int,
    val payloadSize: Int,
    val payloadType: PayloadType,
) {

    init {
        require(payloadOffset >= 0) { "Payload offset cannot be negative." }
        require(payloadSize > 0) { "Payload size must be positive." }
        require(bitmask.countOneBits() > 0) { "No bits specified for overwriting." }
        require(Cryptography.isKeyValid(encryptionKey)) { "Encryption key not valid." }
    }

    companion object {

        /**
         * Generate parameters for encoding or decoding a payload of a given type and size into an image.
         *
         * @param image   The host image to hide the payload in.
         * @param size    The size of the payload, in bytes.
         * @param type    The data type of the payload.
         * @param bitmask The bits to override for pixel values. Last bit of every color channel by default.
         * @param noise   Whether to add noise to the remaining pixels. True by default.
         *
         * @return A set of encoding parameters valid for the specified use.
         *
         * @throws IllegalArgumentException if either the payload wouldn't fit in the image, or the bitmask is invalid.
         */
        fun generate(
            image: Image,
            size: Int,
            type: PayloadType,
            bitmask: UInt = 0x01010100u,
            noise: Boolean = true,
        ): Parameters {
            val random = Cryptography.secureRandom

            val bitCount = bitmask.countOneBits()
            require(bitCount > 0) { "No bits specified for overwriting." }

            val requiredBits = (Header.SIZE_BYTES + size.toLong()) * UByte.SIZE_BITS
            val requiredPixels = ceil(requiredBits.toFloat() / bitCount).toInt()
            val availablePixels = image.pixels.size
            require(availablePixels >= requiredPixels) {
                "Image is not large enough to hide the payload with the current settings. " +
                    "At least $requiredPixels are needed, but only $availablePixels are available."
            }

            return Parameters(
                seed = random.nextLong(),
                encryptionKey = Cryptography.generateKey(),
                bitmask = bitmask,
                noise = noise,
                payloadOffset = random.nextInt(from = 0, until = availablePixels - requiredPixels),
                payloadSize = size,
                payloadType = type,
            )
        }
    }
}
