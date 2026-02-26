package io.github.jadarma.stego.core.internal

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import io.github.jadarma.stego.core.*
import kotlinx.serialization.cbor.Cbor

/** Calculates the maximum number of bytes that can be stored in the image using the [key]. */
internal fun Image.capacity(key: Key): Int = minOf(
    pixels.size.toLong() * key.bitmask.countOneBits() / Byte.SIZE_BITS,
    Int.MAX_VALUE.toLong(),
).toInt()

/**
 * Calculates the indices of the bits that are set in the key's bitmask.
 * The index counts from least to most significant, but they are given in order from most to least significant.
 * For example: `0x01010100` would be `[24, 16, 8]`.
 */
internal fun Key.maskedBits(): IntArray = IntArray(bitmask.countOneBits()).apply {
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

/** Encrypts the [plainText] with this key. */
internal fun Key.encrypt(plainText: ByteArray): ByteArray =
    aesKey
        .cipher(tagSize = 128.bits)
        .encryptBlocking(plainText)

/** Decrypts the [cypherText] with this key. */
internal fun Key.decrypt(cypherText: ByteArray): ByteArray =
    aesKey
        .cipher(tagSize = 128.bits)
        .decryptBlocking(cypherText)

/** Encode the payload to a byte array depending on its type. */
internal fun StegoPayload.encodeToByteArray(): ByteArray = when (this) {
    is RawPayload -> data
    is Payload -> cborFormat.encodeToByteArray(Payload.serializer(), this)
}

/** Returns the number equal to this binary representation with the bit at [index] as `1`. */
internal fun Int.setBit(index: Int): Int = this or (1 shl index)

/** Returns the number equal to this binary representation with the bit at [index] as `0`. */
internal fun Int.clearBit(index: Int): Int = this and (1 shl index).inv()

/** Returns whether the bit at [index] is set. */
internal fun Byte.isBitSet(index: Int): Boolean = this.toInt() and (1 shl index) > 0

/** Returns either `1` or `0` depending on whether the bit at [index] is set. */
internal fun Int.bitAt(index: Int): Int = (this ushr index) and 1

/** Customised CBOR format used for payloads, aimed to be as lightweight and robust as possible. */
internal val cborFormat = Cbor {
    useDefiniteLengthEncoding = true
    alwaysUseByteString = true
    encodeObjectTags = true
    verifyObjectTags = true
    encodeDefaults = true
}
