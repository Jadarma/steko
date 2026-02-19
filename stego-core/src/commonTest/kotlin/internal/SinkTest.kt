package io.github.jadarma.stego.core.internal

import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.io.*

class SinkTest : FunSpec({

    test("Only writes to bit-masked bits") {
        val blackImage = Image(width = 8, height = 12, pixels = UIntArray(96)) // Values chosen to exactly fit capacity
        val whiteImage = Image(width = 8, height = 12, pixels = UIntArray(96) { 0xFFFFFFFFu }) // transparent though
        val lsbKey = Key.generate("hunter2")
        val capacity = withClue("Image capacity not calculated correctly") {
            blackImage.capacity(lsbKey)
                .shouldBe(whiteImage.capacity(lsbKey))
                .shouldBe(36)
        }

        val allZero = ByteArray(capacity) { 0x00 }
        val allOnes = ByteArray(capacity) { 0xFF.toByte() }

        val blackImageCopy = blackImage.copy()
        val whiteImageCopy = whiteImage.copy()

        val (blackSink, blackSource) = blackImageCopy.let { StegoSink(it, lsbKey) to StegoSource(it, lsbKey) }
        val (whiteSink, whiteSource) = whiteImageCopy.let { StegoSink(it, lsbKey) to StegoSource(it, lsbKey) }

        withClue("Could not write to sink(s)") {
            blackSink.buffered().use { it.write(allOnes) }
            whiteSink.buffered().use { it.write(allZero) }
        }

        val (readFromBlack, readFromWhite) = withClue("Could not read from source(s)") {
            blackSource.buffered().readByteArray() to whiteSource.buffered().readByteArray()
        }

        withClue("Did not read same data as was written") {
            readFromBlack.shouldHaveSize(capacity).contentEquals(allOnes)
            readFromWhite.shouldHaveSize(capacity).contentEquals(allZero)
        }

        withClue("Modified pixels outside of bitmask") {
            blackImageCopy.pixels.forEachIndexed { index, value ->
                val original = blackImage.pixels[index]
                original shouldBe 0u
                value xor original shouldBe lsbKey.bitmask
            }
            whiteImageCopy.pixels.forEachIndexed { index, value ->
                val original = whiteImage.pixels[index]
                original shouldBe 0xFFFFFFFFu
                value xor original shouldBe lsbKey.bitmask
            }
        }
    }

    test("Preserves bit order") {
        val image = Image(width = 1, height = 1, pixels = uintArrayOf(0u))
        val payload = 0b11010010u.toUByte()
        val bitmask = 0x0000CCCCu
        val pixel = 0b1100010000001000u
        val key = Key.generate(bitmask)
        val buffer = Buffer().apply { writeUByte(payload) }

        StegoSink(image, key).write(buffer, 1)
        buffer.size shouldBe 0L
        StegoSource(image, key).readAtMostTo(buffer, 1) shouldBe 1L
        buffer.size shouldBe 1L
        buffer.readUByte() shouldBe payload
        image.pixels.single() shouldBe pixel
    }
})
