@file:OptIn(ExperimentalSerializationApi::class)

package io.github.jadarma.steggo.core

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlin.reflect.KClass

/**
 * The different types of payload that Stego can hide.
 *
 * @property value      An 8-bit constant identifying the enum value.
 * @property type       The Kotlin class the payload deserializes to.
 * @property serializer The serializer to use for the type.
 */
enum class PayloadType(
    val value: UByte,
    internal val type: KClass<out Payload>,
    internal val serializer: KSerializer<out Payload>,
) {
    /** A simple binary payload. See [RawPayload]. */
    RAW(0x01u, RawPayload::class, RawPayload.serializer()),
    TEXT(0x02u, TextPayload::class, TextPayload.serializer());

    internal companion object {
        fun of(payload: Payload): PayloadType = entries.first { payload::class == it.type }

        fun of(value: UByte): PayloadType = entries.first { it.value == value }
    }
}

/** Marker interface for the different types of payloads. */
@Serializable
sealed interface Payload {

    companion object
}

/** A simple binary payload with no associated metadata. */
@Serializable
value class RawPayload(val data: UByteArray) : Payload

/** A simple text payload. */
@Serializable
value class TextPayload(val text: String) : Payload

/** Serialize and encrypt this payload with the given [key]. */
@Suppress("UNCHECKED_CAST")
internal fun Payload.encodeToByteString(key: ByteString): ByteString =
    cbor
        .encodeToByteArray(PayloadType.of(this).serializer as KSerializer<Payload>, this)
        .let { data -> Cryptography.encrypt(ByteString(data), key) }

/** Decrypt the payload [data] with the given [key] and deserialize it to the given [type]. */
internal fun Payload.Companion.decodeFromByteString(type: PayloadType, data: ByteString, key: ByteString): Payload =
    cbor.decodeFromByteArray(
        deserializer = type.serializer,
        bytes = Cryptography.decrypt(data, key).toByteArray(),
    )

/** Serialization settings for CBOR encoding. */
private val cbor = Cbor {
    encodeDefaults = false
    ignoreUnknownKeys = false
    encodeKeyTags = false
    encodeValueTags = false
    encodeObjectTags = false
    verifyKeyTags = false
    verifyValueTags = false
    verifyObjectTags = false
    useDefiniteLengthEncoding = true
    preferCborLabelsOverNames = true
    alwaysUseByteString = true
}
