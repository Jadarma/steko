package io.github.jadarma.steko.core

import com.goncalossilva.resources.Resource
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ImageTest : FunSpec({

    test("Constructor is validated") {
        shouldThrow<IllegalArgumentException> { Image(0, 1, intArrayOf(0)) }
        shouldThrow<IllegalArgumentException> { Image(1, 0, intArrayOf(0)) }
        shouldThrow<IllegalArgumentException> { Image(-1, -1, intArrayOf(0)) }
        shouldThrow<IllegalArgumentException> { Image(1, 1, intArrayOf()) }
        shouldThrow<IllegalArgumentException> { Image(2, 1, intArrayOf(0)) }
        shouldNotThrowAny { Image(1, 2, intArrayOf(0, 1)) }
    }

    test("Copying the image copies the buffer") {
        val imageA = Image(1, 2, intArrayOf(0, 1))
        val imageB = imageA.copy()
        imageB.width shouldBe imageA.width
        imageB.height shouldBe imageA.height
        imageB.pixels shouldHaveSize imageA.pixels.size
        imageB.pixels[0] shouldBe 0
        imageA.pixels[0] = 42
        imageB.pixels[0] shouldBe 0
    }

    context("Serialization") {

        test("Can read from and write to RGBA") {
            val raw = Resource("examples/original.rgba").readBytes()
            val decoded = shouldNotThrowAny { Image.decodeFromRgba(raw) }

            withClue("Wrong default size values") {
                decoded.width shouldBe 512 * 512
                decoded.height shouldBe 1
            }

            val encoded = decoded.encodeToRgba()

            withClue("Codec changed the data") {
                encoded shouldHaveSize raw.size
                raw contentEquals encoded shouldBe true
            }
        }

        test("Pixel data is validated") {
            shouldThrow<IllegalArgumentException> { Image.decodeFromRgba(byteArrayOf()) }
            shouldThrow<IllegalArgumentException> { Image.decodeFromRgba(byteArrayOf(1)) }
            shouldThrow<IllegalArgumentException> { Image.decodeFromRgba(byteArrayOf(1, 2)) }
            shouldThrow<IllegalArgumentException> { Image.decodeFromRgba(byteArrayOf(1, 2, 3)) }
            val image = shouldNotThrowAny { Image.decodeFromRgba(byteArrayOf(1, 2, 3, 4)) }
            image.width shouldBe 1
            image.height shouldBe 1
            image.pixels.single().shouldBe(0x01020304)
        }

        test("Optional size parameter is validated") {
            val raw = Resource("examples/original.rgba").readBytes()
            shouldThrow<IllegalArgumentException> {
                Image.decodeFromRgba(raw, 42 to 1337)
            }
            shouldNotThrowAny {
                Image.decodeFromRgba(raw, 512 to 512)
            }
        }
    }
})
