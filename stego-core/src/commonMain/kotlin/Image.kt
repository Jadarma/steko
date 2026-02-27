package io.github.jadarma.stego.core

import io.github.jadarma.stego.core.internal.StegoAlgorithm
import io.github.jadarma.stego.core.internal.cborFormat
import io.github.jadarma.stego.core.internal.encodeToByteArray
import kotlinx.io.Buffer
import kotlinx.io.readTo

/**
 * An image loaded in memory.
 *
 * @property width The width of the image in pixels.
 * @property height The width of the image in pixels.
 * @property pixels The pixel values in RGBA format.
 */
public class Image(
    public val width: Int,
    public val height: Int,
    public val pixels: IntArray,
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
    public suspend fun hide(key: Key, payload: StegoPayload, noise: Boolean = true): Image = apply {
        StegoAlgorithm.createFor(pixels, key).hide(
            payload = payload.encodeToByteArray(),
            rawPayload = payload is RawPayload,
            noise = noise,
        )
    }

    /**
     * Attempt to use the [key] to extract a payload.
     *
     * @param key The key which was used to hide the payload previously.
     *
     * @return Either type of payload, as specified by the carrier image, or `null` if the key doesn't fit.
     */
    public suspend fun show(key: Key): StegoPayload? {
        val (data, isRaw) = StegoAlgorithm.createFor(pixels, key).show() ?: return null
        return if (isRaw) RawPayload(data)
        else runCatching { cborFormat.decodeFromByteArray(Payload.serializer(), data) }.getOrNull()
    }

    /**
     * Attempt to use the [key] to extract a payload and [convert] it to an arbitrary type.
     *
     * @param T       The custom type to deserialize.
     * @param key     The key which was used to hide the payload previously.
     * @param convert A custom deserializer.
     *
     * @return The deserialized type, or `null` if the key doesn't fit. Note this does _not_ catch [convert] exceptions.
     */
    public suspend fun <T : Any> show(key: Key, convert: (ByteArray) -> T): T? {
        val data = StegoAlgorithm.createFor(pixels, key).show()?.first ?: return null
        return convert(data)
    }

    /** Create a new image with the current data. */
    public fun copy(): Image = Image(width, height, pixels.copyOf())

    /** Encodes the image into a RAW pixel sequence in RGBA format. */
    public fun encodeToRgba(): ByteArray = ByteArray(pixels.size * Int.SIZE_BYTES).apply {
        var index = 0
        val buffer = Buffer()
        for (pixel in pixels) {
            buffer.writeInt(pixel)
            buffer.readTo(this, index, index + Int.SIZE_BYTES)
            index += Int.SIZE_BYTES
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
         * @throws IllegalArgumentException If the [data] cannot be perfectly divided into 32-bit values, or the size
         *                                  does not match the pixel count.
         */
        public fun decodeFromRgba(data: ByteArray, size: Pair<Int, Int>? = null): Image {
            require(data.size % Int.SIZE_BYTES == 0) { "Image data should be multiple of 4 bytes." }

            val pixelCount = data.size / Int.SIZE_BYTES
            val width = size?.first ?: pixelCount
            val height = size?.second ?: 1
            val channels = 4

            require(width > 0 && height > 0) { "Image sizes must be positive." }
            require(width * height == pixelCount) { "Size ${width}x$height is wrong: $pixelCount total pixels." }

            val buffer = Buffer()

            return Image(
                width = width,
                height = height,
                pixels = IntArray(pixelCount) { index ->
                    repeat(4) { channel -> buffer.writeByte(data[index * channels + channel]) }
                    buffer.readInt()
                }
            )
        }
    }
}
