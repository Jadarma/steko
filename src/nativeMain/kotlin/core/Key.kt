package io.github.jadarma.steggo.core

import io.github.jadarma.steggo.impl.Cryptography
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
import kotlinx.io.readUByte
import kotlinx.io.readUInt
import kotlinx.io.writeUByte
import kotlinx.io.writeUInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.math.absoluteValue

/**
 * A key encoding the various settings and parameters required to extract a hidden payload.
 * It is _(mostly)_ randomly generated and provided together with the successfully altered image.
 */
@Serializable(with = KeySerializer::class)
class Key private constructor(private val value: String, internal val parameters: Parameters) {

    /** Decode the key from its string representation. */
    constructor(value: String) : this(value, parametersFromValue(value))

    /** Create a key from the parameters it represents. */
    internal constructor(parameters: Parameters) : this(valueFromParameters(parameters), parameters)

    // The string value is used as an identity.
    override fun toString(): String = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other == null || this::class != other::class -> false
        else -> value == (other as Key).value
    }

    private companion object {

        const val KEY_PREFIX = "stego_"

        /** Convert the [params] into their string representation. */
        fun valueFromParameters(params: Parameters): String =
            Buffer().use { buffer ->
                buffer.writeLong(params.seed)
                buffer.write(params.encryptionKey.toByteArray())
                buffer.writeUInt(params.bitmask)
                buffer.writeInt(params.payloadOffset * if(params.noise) -1 else 1)
                buffer.writeInt(params.payloadSize)
                buffer.writeUByte(params.payloadType.value)
                KEY_PREFIX + buffer.readByteArray().let(Base64.UrlSafe::encode)
            }

        /** Decode the parameters from the string [key]. */
        fun parametersFromValue(key: String): Parameters {
            require(key.startsWith(KEY_PREFIX)) { "Key must start with prefix." }
            val bytes = key.removePrefix(KEY_PREFIX).let(Base64.UrlSafe::decode)
            return Buffer().use { buffer ->
                buffer.write(bytes)
                val seed = buffer.readLong()
                val key = buffer.readByteArray(Cryptography.KEY_SIZE_BYTES)
                val bitmask = buffer.readUInt()
                val offsetAndNoise = buffer.readInt()
                val size = buffer.readInt()
                val type = buffer.readUByte()
                check(buffer.size == 0L) { "Key contained extraneous bytes." }
                Parameters(
                    seed = seed,
                    encryptionKey = ByteString(key),
                    bitmask = bitmask,
                    noise = offsetAndNoise < 0,
                    payloadOffset = offsetAndNoise.absoluteValue,
                    payloadSize = size,
                    payloadType = PayloadType.of(type),
                )
            }
        }
    }
}

/** Serializes the key as a string. */
object KeySerializer : KSerializer<Key> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(Key::class.qualifiedName!!, STRING)
    override fun serialize(encoder: Encoder, value: Key) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Key = Key(decoder.decodeString())
}
