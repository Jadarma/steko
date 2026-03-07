package io.github.jadarma.steko.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray
import kotlinx.serialization.cbor.ObjectTags

/** Marker interface for payloads hidden by Steko. */
public sealed interface StekoPayload

/**
 * The default payload the Steko manages.
 * May contain a message, attachments, or both.
 *
 * @property message     An optional short human-readable text.
 * @property attachments A collection of arbitrary byte strings, keyed by their file name.
 *                       Emulates a primitive, flat filesystem.
 */
@CborArray
@ObjectTags(Payload.PAYLOAD_TAG_MAGIC)
@Serializable
public class Payload(
    public val message: String? = null,
    public val attachments: Map<String, ByteArray> = emptyMap(),
) : StekoPayload {

    init {
        require(message != null || attachments.isNotEmpty()) { "Payload cannot be empty." }

        attachments.forEach { (name, content) ->
            require(content.isNotEmpty()) { "Attachment contents must not be empty." }
            require(name.isNotEmpty()) { "Attachments must have a name." }
            require(name.length <= MAX_ATTACHMENT_NAME_LENGTH) {
                "Attachments names must not exceed $MAX_ATTACHMENT_NAME_LENGTH characters."
            }
            require(!name.contains("/") && !name.contains(":")) { "Attachments contain illegal characters." }
            require(name.trim() == name) { "Attachment names must not have surrounding whitespace." }
        }
    }

    /** Construct a payload containing only a message. */
    public constructor(message: String) : this(message = message, attachments = emptyMap())

    /** Construct a payload containing only attachments. */
    public constructor(attachments: Map<String, ByteArray>) : this(message = null, attachments = attachments)

    public companion object {

        /** Magic identifier for double-checking: binary for "STEG". */
        private const val PAYLOAD_TAG_MAGIC = 0x53544547uL

        /** An arbitrary limit to the attachment name size, even if filesystems support longer ones, to be nice. */
        private const val MAX_ATTACHMENT_NAME_LENGTH = 255
    }
}

/**
 * Custom data, when user decided not to use the default [Payload].
 * This represents an arbitrary buffer.
 *
 * @property data The raw bytes of the payload.
 */
public value class RawPayload(public val data: ByteArray) : StekoPayload {

    init {
        require(data.isNotEmpty()) { "Payload cannot be empty." }
    }
}
