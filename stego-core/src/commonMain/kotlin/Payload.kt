package io.github.jadarma.stego.core

import kotlinx.serialization.Serializable

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
@Serializable
data class Payload(
    val message: String? = null,
    val attachments: Map<String, ByteArray> = emptyMap(),
) : StegoPayload

/**
 * Custom data, when user decided not to use the default [Payload].
 * This represents an arbitrary buffer.
 *
 * @property data The raw bytes of the payload.
 */
value class RawPayload(val data: ByteArray) : StegoPayload
