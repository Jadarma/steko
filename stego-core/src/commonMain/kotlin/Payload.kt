package io.github.jadarma.stego.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray
import kotlinx.serialization.cbor.ObjectTags

/** Marker interface for payloads hidden by Stego. */
sealed interface StegoPayload

/**
 * The default payload the Stego manages.
 * May contain a message, attachments, or both.
 *
 * @property message     An optional short human-readable text.
 * @property attachments A collection of arbitrary byte strings, keyed by their file name.
 *                       Emulates a primitive, flat filesystem.
 */
@CborArray
@ObjectTags(0x53544547uL) // "STEG" Magic identifier for double-checking.
@Serializable
class Payload(
    val message: String? = null,
    val attachments: Map<String, ByteArray> = emptyMap(),
) : StegoPayload {

    /** Construct a payload containing only a message. */
    constructor(message: String) : this(message = message, attachments = emptyMap())

    /** Construct a payload containing only attachments. */
    constructor(attachments: Map<String, ByteArray>) : this(message = null, attachments = attachments)

    init {
        require(message != null || attachments.isNotEmpty()) { "Payload cannot be empty." }

        attachments.forEach { (name, content) ->
            require(content.isNotEmpty()) { "Attachment contents must not be empty." }
            require(name.isNotEmpty()) { "Attachments must have a name." }
            require(name.length < 256) { "Attachments names must not exceed 255 characters." }
            require(!name.contains("/") && !name.contains(":")) { "Attachments contain illegal characters." }
            require(name.trim() == name) { "Attachment names must not have surrounding whitespace." }
        }
    }
}

/**
 * Custom data, when user decided not to use the default [Payload].
 * This represents an arbitrary buffer.
 *
 * @property data The raw bytes of the payload.
 */
value class RawPayload(val data: ByteArray) : StegoPayload {

    init {
        require(data.isNotEmpty()) { "Payload cannot be empty." }
    }
}
