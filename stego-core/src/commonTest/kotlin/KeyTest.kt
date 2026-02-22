package io.github.jadarma.stego.core

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KeyTest : FunSpec({

    val exampleKeyString = "01010100b2b3b86ff88ef6c490628285f482af15ddcb29541f94bcf526a3f6c7"
    val exampleKeyPassphrase = "hunter2"
    val exampleKey = Key.generate(exampleKeyPassphrase)

    test("Algorithm values are extracted correctly") {
        exampleKey.bitmask shouldBe 0x01010100u
        exampleKey.challenge shouldBe "8e5c125c".hexToInt()
    }

    context("Generation") {
        test("Can generate a random key") {
            val key = shouldNotThrowAny { Key.generate() }
            key.bitmask shouldBe 0x01010100u
        }

        test("Can use custom bitmasks") {
            shouldThrow<IllegalArgumentException> { Key.generate(bitmask = 0u) }
            val key = Key.generate(bitmask = 0x02000200u)
            key.bitmask shouldBe 0x002000200u
        }

        test("Can derive from passphrase") {
            val key = Key.generate(exampleKeyPassphrase)
            key.toHexString() shouldBe exampleKeyString
            key shouldBe exampleKey
        }
    }

    context("Serialization") {
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
