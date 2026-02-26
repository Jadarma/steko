package io.github.jadarma.stego.core.internal

import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray

class SinkTest : FunSpec({

    test("Only writes to bit-masked bits") {
        val blackImage = Image(width = 8, height = 12, pixels = IntArray(96))
        val whiteImage = Image(width = 8, height = 12, pixels = IntArray(96) { 0xFFFFFFFF.toInt() })
        val lsbKey = Key.generate("hunter2")

        val blackStego = StegoAlgorithm.createFor(blackImage.copy().pixels, lsbKey)
        val whiteStego = StegoAlgorithm.createFor(whiteImage.copy().pixels, lsbKey)

        val (blackSink, blackSource) = StegoSink(blackStego) to StegoSource(blackStego)
        val (whiteSink, whiteSource) = StegoSink(whiteStego) to StegoSource(whiteStego)

        val capacity = withClue("Image capacity not calculated correctly") {
            blackStego.capacity
                .shouldBe(whiteStego.capacity)
                .shouldBe(36)
        }

        val allZero = ByteArray(capacity) { 0x00 }
        val allOnes = ByteArray(capacity) { 0xFF.toByte() }

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
            blackStego.pixels.forEachIndexed { index, value ->
                val original = blackImage.pixels[index]
                original shouldBe 0
                value xor original shouldBe blackStego.bitmask
            }
            whiteStego.pixels.forEachIndexed { index, value ->
                val original = whiteImage.pixels[index]
                original shouldBe 0xFFFFFFFF.toInt()
                value xor original shouldBe whiteStego.bitmask
            }
        }
    }

    test("Preserves bit order") {
        val image = Image(width = 1, height = 1, pixels = intArrayOf(0))
        val payload = 0b11010010.toByte()
        val bitmask = 0x0000CCCC
        val pixel = 0b1100010000001000
        val key = Key.generate(bitmask)
        val buffer = Buffer().apply { writeByte(payload) }
        val stego = StegoAlgorithm.createFor(image.pixels, key)

        StegoSink(stego).write(buffer, 1)
        buffer.size shouldBe 0L
        StegoSource(stego).readAtMostTo(buffer, 1) shouldBe 1L
        buffer.size shouldBe 1L
        buffer.readByte() shouldBe payload
        image.pixels.single() shouldBe pixel
    }
})
