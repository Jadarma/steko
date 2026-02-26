package io.github.jadarma.stego.core.internal

import io.github.jadarma.stego.core.Payload
import io.github.jadarma.stego.core.RawPayload
import io.github.jadarma.stego.core.StegoPayload
import kotlinx.serialization.cbor.Cbor

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
