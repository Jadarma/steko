package io.github.jadarma.stego.core

import io.github.jadarma.stego.core.internal.cborFormat
import io.github.jadarma.stego.core.internal.encodeToByteArray
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException

class PayloadTest : FunSpec({

    context("Validation") {
        test("Cannot have empty payloads") {
            shouldThrow<IllegalArgumentException> { RawPayload(byteArrayOf()) }
            shouldThrow<IllegalArgumentException> { Payload(null, emptyMap()) }
            shouldThrow<IllegalArgumentException> { Payload(mapOf("leaked.pdf" to byteArrayOf())) }
        }

        test("Attachment names must be simple files") {
            val content = "Hello".encodeToByteArray()
            shouldNotThrowAny { Payload(mapOf("valid.txt" to content)) }
            shouldThrow<IllegalArgumentException> { Payload(mapOf("" to content)) }
            shouldThrow<IllegalArgumentException> { Payload(mapOf(" padding " to content)) }
            shouldThrow<IllegalArgumentException> { Payload(mapOf("/etc/usr/sneaky.sh" to content)) }
            shouldThrow<IllegalArgumentException> { Payload(mapOf("illegal:characters" to content)) }
            shouldThrow<IllegalArgumentException> { Payload(mapOf("x".repeat(256) to content)) }
        }
    }

    context("Serialization") {
        val payloadA = Payload("Hello, World!")
        val payloadB = Payload(mapOf("hello.txt" to "Hello, World!".encodeToByteArray()))
        val payloadC = Payload("Emergency Backup", mapOf("paperkey.txt" to byteArrayOf(1, 2, 3)))
        // Inspect these with https://cbor.me
        val expectedA = "da53544547826d48656c6c6f2c20576f726c6421a0"
        val expectedB = "da5354454782f6a16968656c6c6f2e7478744d48656c6c6f2c20576f726c6421"
        val expectedC = "da535445478270456d657267656e6379204261636b7570a16c70617065726b65792e74787443010203"

        test("Payloads can be serialized into Cbor") {
            payloadA.encodeToByteArray().toHexString() shouldBe expectedA
            payloadB.encodeToByteArray().toHexString() shouldBe expectedB
            payloadC.encodeToByteArray().toHexString() shouldBe expectedC
        }

        test("Payloads can be deserialized from Cbor") {
            val decodedA = cborFormat.decodeFromByteArray(Payload.serializer(), expectedA.hexToByteArray())
            decodedA shouldHaveSameContentAs payloadA
            val decodedB = cborFormat.decodeFromByteArray(Payload.serializer(), expectedB.hexToByteArray())
            decodedB shouldHaveSameContentAs payloadB
            val decodedC = cborFormat.decodeFromByteArray(Payload.serializer(), expectedC.hexToByteArray())
            decodedC shouldHaveSameContentAs payloadC
        }

        test("Refuses to deserialize without object tag") {
            listOf(expectedA, expectedB, expectedC)
                .map { it.removePrefix("da5354454782").hexToByteArray() }
                .forEach { data ->
                    shouldThrow<SerializationException> { cborFormat.decodeFromByteArray(Payload.serializer(), data) }
                }
        }
    }
})

private infix fun Payload.shouldHaveSameContentAs(expected: Payload) {
    this.message shouldBe expected.message
    this.attachments.keys shouldBe expected.attachments.keys
    expected.attachments.forEach { (name, content) ->
        this.attachments shouldHaveKey name
        this.attachments.getValue(name).toHexString() shouldBe content.toHexString()
    }
}
