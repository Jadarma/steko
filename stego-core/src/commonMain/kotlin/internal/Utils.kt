package io.github.jadarma.stego.core.internal

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
        mask = mask.shr(trailing)
        bitIndex += trailing
        this[--index] = bitIndex - 1
    }
}

/** Encode the payload to a byte array depending on its type. */
internal fun StegoPayload.encodeToByteArray(): ByteArray = when (this) {
    is RawPayload -> data
    is Payload -> cborFormat.encodeToByteArray(Payload.serializer(), this)
}

/** Returns the number equal to this binary representation with the bit at [index] as `1`. */
internal fun UInt.setBit(index: Int): UInt = this or (1u shl index)

/** Returns the number equal to this binary representation with the bit at [index] as `0`. */
internal fun UInt.clearBit(index: Int): UInt = this and (1u shl index).inv()

/** Returns whether the bit at [index] is set. */
internal fun Byte.isBitSet(index: Int): Boolean = this.toUInt() and (1u shl index) > 0u

/** Returns either `1u` or `0u` depending on whether the bit at [index] is set. */
internal fun UInt.bitAt(index: Int): UInt = (this shr index) and 1u

/** Customised CBOR format used for payloads, aimed to be as lightweight and robust as possible. */
internal val cborFormat = Cbor {
    useDefiniteLengthEncoding = true
    alwaysUseByteString = true
    encodeObjectTags = true
    verifyObjectTags = true
    encodeDefaults = true
}
