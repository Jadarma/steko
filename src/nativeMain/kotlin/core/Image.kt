package io.github.jadarma.steggo.core

/**
 * An image loaded in memory.
 * @property width The width of the image in pixels.
 * @property height The width of the image in pixels.
 * @property pixels The pixel values in RGBA format.
 */
class Image(
    val width: Int,
    val height: Int,
    val pixels: UIntArray,
) {
    init {
        require(pixels.size == width * height) { "Pixel count does not match resolution." }
    }

    companion object
}
