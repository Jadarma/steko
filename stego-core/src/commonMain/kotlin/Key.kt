package io.github.jadarma.stego.core

import dev.whyoleg.cryptography.CryptographySystem
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readTo
import kotlinx.io.readUInt
import kotlinx.io.writeUInt
import kotlin.random.Random

/**
 * A key that can be used to embed or extract a hidden, encrypted payload inside pixel data.
 *
 * @property bytes The key's binary representation.
 */
class Key(bytes: ByteArray) {

    /** Construct a key by reading a copy of given [bytes]. */
    constructor(bytes: ByteString) : this(bytes.toByteArray())

    /** Construct a key by decoding it from its [hexString] representation. */
    constructor(hexString: String) : this(hexString.hexToByteArray())

    /** The bits to use from the RGBA pixel data. */
    internal val bitmask: UInt

    /** The seed to generate pseudo-random pixel order from. */
    internal val seed: Long

    /** The cryptographic key used to encrypt and decrypt the payload. */
    internal val aesKey: AES.GCM.Key

    /** The windowed XOR over the hash of the key, used as a preflight check. */
    internal val challenge: Int

    init {
        require(bytes.size == SIZE_BYTES) { "Invalid key format. Got ${bytes.size} instead of $SIZE_BYTES bytes." }
        Buffer().use { buffer ->
            buffer.write(bytes)
            bitmask = buffer.readUInt()
            buffer.write(bytes, UInt.SIZE_BYTES, Long.SIZE_BYTES)
            seed = buffer.readLong()
        }
        require(bitmask != 0u) { "The bitmask requires at least one bit to be set." }
        Buffer().use { buffer ->
            buffer.write(hasher.hashBlocking(bytes))
            var acc = 0
            repeat(HASH_SIZE_BYTES / Int.SIZE_BITS) {
                acc = acc xor buffer.readInt()
            }
            challenge = acc
        }
        aesKey = keyDecoder.decodeFromByteArrayBlocking(AES.Key.Format.RAW, bytes)
    }

    /** Encodes the key in its raw binary representation. */
    fun toByteArray(): ByteArray = aesKey.encodeToByteArrayBlocking(AES.Key.Format.RAW)

    /** Encodes the key in its hex-string representation. */
    fun toHexString(): String = toByteArray().toHexString()

    override fun hashCode(): Int = challenge

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other == null || this::class != other::class -> false
        this.challenge != (other as Key).challenge -> false
        else -> this.toHexString() == other.toHexString()
    }

    companion object {
        const val SIZE_BYTES: Int = 32
        const val SIZE_BITS: Int = 256
        internal const val HASH_SIZE_BYTES: Int = 32

        internal const val DEFAULT_BITMASK: UInt = 0x01010100u

        /**
         * Generates a new, random key.
         *
         * @param bitmask The bits to use from the RGBA pixel data when performing steganographic operations.
         *                By default, the least significant bit of the R, G, and B channels will be used.
         *                The value cannot be zero, as then no bits would be used.
         * @param random  The source of random data.
         *                By default, a platform-specific, cryptographically secure random source is used.
         *                It is not recommended to use pseudo-random instances here except for testing.
         */
        fun generate(
            bitmask: UInt = DEFAULT_BITMASK,
            random: Random = CryptographySystem.getDefaultRandom(),
        ): Key {
            val bytes = random.nextBytes(SIZE_BYTES)
            Buffer().use {
                it.writeUInt(bitmask)
                it.readTo(bytes, 0, UInt.SIZE_BYTES)
            }
            return Key(ByteString(bytes))
        }

        /**
         * Derive a key from the [passphrase].
         * This allows human-friendly interaction.
         * Note that when using this mode, the bitmask cannot be changed from the default.
         */
        fun generate(passphrase: String): Key {
            val bytes = hasher.hashBlocking(passphrase.encodeToByteArray())
            Buffer().use {
                it.writeUInt(DEFAULT_BITMASK)
                it.readTo(bytes, 0, UInt.SIZE_BYTES)
            }
            return Key(ByteString(bytes))
        }

        private val keyDecoder by lazy { CryptographySystem.getDefaultProvider().get(AES.GCM).keyDecoder() }
        private val hasher by lazy { CryptographySystem.getDefaultProvider().get(SHA256).hasher() }
    }
}
