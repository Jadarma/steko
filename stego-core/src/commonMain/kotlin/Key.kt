package io.github.jadarma.stego.core

import dev.whyoleg.cryptography.CryptographySystem
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.hexToByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.io.readTo

/**
 * A key that can be used to embed or extract a hidden, encrypted payload inside pixel data.
 *
 * @property bytes The key's binary representation.
 */
public value class Key private constructor(internal val bytes: ByteString) {

    init {
        require(bytes.size == SIZE_BYTES) {
            "Invalid key: got ${bytes.size} bytes, expected exactly $SIZE_BYTES."
        }
        require(bytes.toByteArray(0, Int.SIZE_BYTES).any { it != 0.toByte() }) {
            "Invalid key: leading 32 bits are all zero, bitmask impossible to use."
        }
    }

    /** Construct a key from its binary representation. */
    public constructor(bytes: ByteArray) : this(ByteString(bytes))

    /** Construct a key from its hexadecimal representation. */
    public constructor(hex: String) : this(hex.hexToByteString())

    /** Encodes the key in its raw binary representation. */
    public fun toByteArray(): ByteArray = bytes.toByteArray()

    /** Encodes the key in its hex-string representation. */
    public fun toHexString(): String = bytes.toHexString()


    public companion object {

        /** The size of the binary representation of the key, in bytes. */
        public const val SIZE_BYTES: Int = 32

        /** The size of the binary representation of the key, in bits. */
        public const val SIZE_BITS: Int = SIZE_BYTES * Byte.SIZE_BITS

        /** The default bitmask to use, LSB of R,G, and B color channels. */
        private const val DEFAULT_BITMASK: Int = 0x01010100

        /**
         * Generates a new, random key.
         *
         * @param bitmask The bits to use from the RGBA pixel data when performing steganographic operations.
         *                By default, the least significant bit of the R, G, and B channels will be used.
         *                The value cannot be zero, as then no bits would be used.
         */
        public fun generate(bitmask: Int = DEFAULT_BITMASK): Key {
            val bytes = CryptographySystem.getDefaultRandom().nextBytes(SIZE_BYTES)
            Buffer().use {
                it.writeInt(bitmask)
                it.readTo(bytes, 0, Int.SIZE_BYTES)
            }
            return Key(bytes)
        }

        /**
         * Derive a key from the [passphrase].
         * This allows human-friendly interaction.
         * Note that when using this mode, the bitmask cannot be changed from the default.
         */
        public suspend fun generate(passphrase: String): Key {
            val bytes = CryptographySystem.getDefaultProvider()
                .get(SHA256).hasher()
                .hash(passphrase.encodeToByteArray())
            Buffer().use {
                it.writeInt(DEFAULT_BITMASK)
                it.readTo(bytes, 0, Int.SIZE_BYTES)
            }
            return Key(bytes)
        }
    }
}
