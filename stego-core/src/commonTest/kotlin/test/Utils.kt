package io.github.jadarma.stego.core.test

import com.goncalossilva.resources.Resource
import io.github.jadarma.stego.core.*
import io.github.jadarma.stego.core.internal.StegoAlgorithm
import io.github.jadarma.stego.core.internal.encodeToByteArray
import io.kotest.assertions.fail
import io.kotest.core.test.TestScope

/** Load an image from a [resource]. */
fun Image.Companion.decodeFromRgba(resource: Resource, size: Pair<Int, Int>? = null): Image =
    decodeFromRgba(resource.readBytes(), size)

/**
 * Asserts the proper functioning of hiding an image.
 *
 * @param original The RGBA image to use as input.
 * @param expected The RGBA image expected to be output.
 * @param hexKey   The key used to perform the hiding, expected in hexadecimal format.
 * @param noise    Whether random noise should be added.
 * @param payload  The secret to hide in the image.
 */
context(_: TestScope)
internal suspend fun hidingTest(
    original: Resource,
    expected: Resource,
    hexKey: Resource,
    noise: Boolean,
    payload: StegoPayload,
) {
    val originalImage = Image.decodeFromRgba(original)
    val expectedImage = Image.decodeFromRgba(expected)
    val key = Key(hex = hexKey.readText().trimEnd())
    val actualImage = originalImage.copy()

    StegoAlgorithm.createFor(actualImage.pixels, key).hide(
        payload = payload.encodeToByteArray(),
        rawPayload = payload is RawPayload,
        noise = noise,
    )

    if (!actualImage.pixels.contentEquals(expectedImage.pixels)) {
        val diff = actualImage.pixels.indices.count { actualImage.pixels[it] != expectedImage.pixels[it] }
        fail("The actual image output differs from expected by $diff pixels")
    }
}

/**
 * Asserts the proper functioning of showing a hidden payload.
 *
 * @param carrier  The RGBA image to use as input.
 * @param hexKey   The key used to perform the showing, expected in hexadecimal format.
 * @param expected The payload expected to be found, or `null` if expected to find nothing.
 */
context(_: TestScope)
internal suspend fun showingTest(
    carrier: Resource,
    hexKey: Resource,
    expected: StegoPayload?,
) {
    val carrierImage = Image.decodeFromRgba(carrier)
    val key = Key(hex = hexKey.readText().trimEnd())

    val result = StegoAlgorithm.createFor(carrierImage.pixels, key).show()

    if (expected == null) {
        if (result != null) fail("Expected not to find anything, but a payload was found.")
    } else {
        if (result == null) fail("Expected to find payload, but none was found.")
        val (data, isRaw) = result
        when (expected) {
            is RawPayload -> if (!isRaw) fail("Expected to find raw payload, but got normal payload.")
            is Payload -> if (isRaw) fail("Expected to find normal payload, but got raw payload.")
        }
        if (!data.contentEquals(expected.encodeToByteArray())) {
            fail("Actual and expected payload contents differ.")
        }
    }
}
