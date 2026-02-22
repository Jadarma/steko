package io.github.jadarma.stego.core

import dev.whyoleg.cryptography.CryptographySystem
import io.github.jadarma.stego.core.internal.*
import kotlinx.io.*
import kotlin.math.absoluteValue
import kotlin.random.nextUInt

/**
 * An image loaded in memory.
 * @property width The width of the image in pixels.
 * @property height The width of the image in pixels.
 * @property pixels The pixel values in RGBA format.
 */
public class Image(
    public val width: Int,
    public val height: Int,
    public val pixels: UIntArray,
) {

    init {
        require(pixels.isNotEmpty()) { "Image is empty." }
        require(width > 0) { "Width must be positive." }
        require(height > 0) { "Height must be positive." }
        require(pixels.size == width * height) { "Pixel count does not match resolution." }
    }

    /**
     * Hides the [payload] inside this image, altering its pixels.
     * If you would rather keep the original, chain a [copy] call beforehand.
     *
     * @param key     The key which should be used to retrieve this later.
     * @param payload The bytes to hide in the image.
     * @param noise   Whether to encode random data in _all_ pixels according to the [key]'s bitmask.
     *                This can make the image blend in better, and erases all previous messages with the same mask.
     *                Enabled by default.
     *
     * @throws IndexOutOfBoundsException If the payload cannot fit inside this image.
     * @return This same instance.
     */
    public fun hide(key: Key, payload: StegoPayload, noise: Boolean = true): Image {
        val encrypted = key.encrypt(payload.encodeToByteArray())
        val requiredCapacity = Int.SIZE_BYTES * 2 + encrypted.size
        val thisCapacity = this.capacity(key)
        if (requiredCapacity > thisCapacity) {
            throw IndexOutOfBoundsException("(${requiredCapacity}B > ${thisCapacity}B)")
        }

        if (noise) noise(key.bitmask)
        val isRawPayload = payload is RawPayload
        val length = if (isRawPayload) -encrypted.size else encrypted.size

        StegoSink(this, key).buffered().use { sink ->
            sink.writeInt(key.challenge)
            sink.writeInt(length)
            sink.write(encrypted)
        }

        return this
    }

    /**
     * Attempt to use the [key] to extract a payload.
     *
     * @param key The key which was used to hide the payload previously.
     *
     * @return Either type of payload, as specified by the carrier image, or `null` if the key doesn't fit.
     */
    public fun show(key: Key): StegoPayload? {
        val (isRaw, data) = showInternal(key) ?: return null
        return if (isRaw) RawPayload(data)
        else runCatching { cborFormat.decodeFromByteArray(Payload.serializer(), data) }.getOrNull()
    }

    /**
     * Attempt to use the [key] to extract a payload and [convert] it to an arbitrary type.
     *
     * @param key     The key which was used to hide the payload previously.
     * @param convert A custom deserializer.
     *
     * @return The deserialized type, or `null` if the key doesn't fit. Note this does _not_ catch [convert] exceptions.
     */
    public fun <T : Any> show(key: Key, convert: (ByteArray) -> T): T? {
        val data = showInternal(key)?.second ?: return null
        return convert(data)
    }

    /** Create a new image with the current data. */
    public fun copy(): Image = Image(width, height, pixels.copyOf())

    /** Encodes the image into a RAW pixel sequence in RGBA format. */
    public fun encodeToRgba(): ByteArray = ByteArray(pixels.size * UInt.SIZE_BYTES).apply {
        var index = 0
        val buffer = Buffer()
        for (pixel in pixels) {
            buffer.writeUInt(pixel)
            buffer.readTo(this, index, index + UInt.SIZE_BYTES)
            index += UInt.SIZE_BYTES
        }
    }

    /**
     * Attempt to use the [key] to extract a payload.
     * Returns whether the payload encoded that it should be interpreted as _RAW_ and the body, or `null` if the key
     * does not fit.
     */
    private fun showInternal(key: Key): Pair<Boolean, ByteArray>? = runCatching {
        val source = StegoSource(this, key).buffered()
        check(source.readInt() == key.challenge)
        val length = source.readInt()
        val isRaw = length < 0
        val data = source.readByteArray(length.absoluteValue)
        isRaw to key.decrypt(data)
    }.getOrNull()

    /** Modifies the current image, adding random data over bits specified by the [bitmask]. */
    private fun noise(bitmask: UInt) {
        val invMask = bitmask.inv()
        val random = CryptographySystem.getDefaultRandom()
        for (index in pixels.indices) {
            pixels[index] = (pixels[index] and invMask) or (random.nextUInt() and bitmask)
        }
    }

    public companion object {

        /**
         * Decodes an image from raw RGBA values. Assumes 8-bit channel depth.
         *
         * @param data The raw RGBA values, in order, as a byte string.
         * @param size The size of the image, in width and height, if known.
         *             Setting the value helps when encoding to other formats.
         *             By default, acts like a RAW format, and assumes a single row of pixels.
         *
         * @throws IllegalArgumentException If the [data] cannot be perfectly divided into 32-bit values, or the size does not
         *                                  match the pixel count.
         */
        public fun decodeFromRgba(data: ByteArray, size: Pair<Int, Int>? = null): Image {
            require(data.size % UInt.SIZE_BYTES == 0) { "Image data should be multiple of 4 bytes." }

            val pixelCount = data.size / UInt.SIZE_BYTES
            val width = size?.first ?: pixelCount
            val height = size?.second ?: 1
            val channels = 4

            require(width > 0 && height > 0) { "Image sizes must be positive." }
            require(width * height == pixelCount) { "Size ${width}x$height is wrong: $pixelCount total pixels." }

            val buffer = Buffer()

            return Image(
                width = width,
                height = height,
                pixels = UIntArray(pixelCount) { index ->
                    repeat(4) { channel -> buffer.writeByte(data[index * channels + channel]) }
                    buffer.readUInt()
                }
            )
        }
    }
}
