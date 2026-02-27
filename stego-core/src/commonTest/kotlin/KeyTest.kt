package io.github.jadarma.stego.core

import io.github.jadarma.stego.core.internal.StegoAlgorithm
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class KeyTest : FunSpec({

    val exampleKeyString = "01010100b2b3b86ff88ef6c490628285f482af15ddcb29541f94bcf526a3f6c7"
    val exampleKeyPassphrase = "hunter2"
    val exampleBitmask = 0x01010100
    val exampleChallenge = 0x8e5c125c.toInt()

    context("Generation") {
        val stegoFor = suspend { key: Key -> StegoAlgorithm.createFor(intArrayOf(), key) }

        test("Can generate a random key") {
            val key = shouldNotThrowAny { Key.generate() }
            stegoFor(key).bitmask shouldBe 0x01010100
        }

        test("Can use custom bitmasks") {
            shouldThrow<IllegalArgumentException> { Key.generate(bitmask = 0) }
            val key = Key.generate(bitmask = 0x02000200)
            stegoFor(key).bitmask shouldBe 0x002000200
        }

        test("Can derive from passphrase") {
            val key = Key.generate(exampleKeyPassphrase)
            key.toHexString() shouldBe exampleKeyString
            stegoFor(key).should {
                it.bitmask shouldBe exampleBitmask
                it.challenge shouldBe exampleChallenge
            }
        }
    }

    context("Serialization") {
        val exampleKey = Key.generate(exampleKeyPassphrase)

        test("Can serialize from hex string") {
            val encoded = exampleKey.toHexString()
            val decoded = Key(encoded)
            encoded shouldBe exampleKeyString
            decoded shouldBe exampleKey
            val randomKey = Key.generate()
            Key(randomKey.toHexString()) shouldBe randomKey
        }

        test("Can serialize to byte array") {
            val encoded = exampleKey.toByteArray()
            val decoded = Key(encoded)
            encoded shouldBe exampleKeyString.hexToByteArray()
            decoded shouldBe exampleKey
            encoded.size shouldBe Key.SIZE_BYTES
            encoded.size * Byte.SIZE_BITS shouldBe Key.SIZE_BITS
            val randomKey = Key.generate()
            Key(randomKey.toByteArray()) shouldBe randomKey
        }
    }
})
