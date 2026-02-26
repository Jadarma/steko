package io.github.jadarma.stego.core

import dev.whyoleg.cryptography.CryptographySystem
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.io.Buffer
import kotlinx.io.readTo

/**
 * A key that can be used to embed or extract a hidden, encrypted payload inside pixel data.
 *
 * @param bytes The key's binary representation.
 */
public class Key(bytes: ByteArray) {

    /** Construct a key by decoding it from its [hexString] representation. */
    public constructor(hexString: String) : this(hexString.hexToByteArray())

    /** The cryptographic key used to encrypt and decrypt the payload. */
    internal val aesKey: AES.GCM.Key

    /** The bits to use from the RGBA pixel data. */
    internal val bitmask: Int

    /** The windowed XOR over the hash of the key, used as a preflight check. */
    internal val challenge: Int

    init {
        require(bytes.size == SIZE_BYTES) { "Invalid key format. Got ${bytes.size} instead of $SIZE_BYTES bytes." }
        Buffer().use { buffer ->
            buffer.write(bytes)
            bitmask = buffer.readInt()
        }
        require(bitmask != 0) { "The bitmask requires at least one bit to be set." }
        Buffer().use { buffer ->
            buffer.write(hasher.hashBlocking(bytes))
            challenge = IntArray(HASH_SIZE_BYTES / Int.SIZE_BYTES) { buffer.readInt() }.reduce(Int::xor)
        }
        aesKey = keyDecoder.decodeFromByteArrayBlocking(AES.Key.Format.RAW, bytes)
    }

    /** Encodes the key in its raw binary representation. */
    public fun toByteArray(): ByteArray = aesKey.encodeToByteArrayBlocking(AES.Key.Format.RAW)

    /** Encodes the key in its hex-string representation. */
    public fun toHexString(): String = toByteArray().toHexString()

    override fun hashCode(): Int = challenge

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other == null || this::class != other::class -> false
        this.challenge != (other as Key).challenge -> false
        else -> this.toHexString() == other.toHexString()
    }

    public companion object {
        public const val SIZE_BYTES: Int = 32
        public const val SIZE_BITS: Int = 256

        private const val HASH_SIZE_BYTES: Int = 32

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
        public fun generate(passphrase: String): Key {
            val bytes = hasher.hashBlocking(passphrase.encodeToByteArray())
            Buffer().use {
                it.writeInt(DEFAULT_BITMASK)
                it.readTo(bytes, 0, Int.SIZE_BYTES)
            }
            return Key(bytes)
        }

        private val keyDecoder by lazy { CryptographySystem.getDefaultProvider().get(AES.GCM).keyDecoder() }
        private val hasher by lazy { CryptographySystem.getDefaultProvider().get(SHA256).hasher() }
    }
}
