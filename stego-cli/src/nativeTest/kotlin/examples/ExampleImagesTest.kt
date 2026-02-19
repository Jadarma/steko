package io.github.jadarma.stego.cli.examples

import com.goncalossilva.resources.Resource
import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.github.jadarma.stego.core.Payload
import io.github.jadarma.stego.core.RawPayload
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ExampleImagesTest : FunSpec({

    context("Attachments") {
        val payload = Resource("examples/attachments/anImage.png").readBytes()
        val key = Key("03030300fbc58d4fe7ed6ddb52d442c8318e70b4d197e30c6f32fc6dc955d89e")

        test("Can extract normal payloads") {
            val carrier = Image.loadResource("examples/attachments/normalPayload.png")
            val (name, data) = carrier.show(key).shouldNotBeNull()
                .shouldBeInstanceOf<Payload>()
                .attachments.entries.single()
            name shouldBe "anImage.png"
            data contentEquals payload shouldBe true
        }

        test("Can extract raw payloads") {
            val carrier = Image.loadResource("examples/attachments/rawPayload.png")
            carrier.show(key).shouldNotBeNull()
                .shouldBeInstanceOf<RawPayload>()
                .data.contentEquals(payload)
                .shouldBeTrue()
        }
    }

    context("Bitmasks") {
        val lorem = Resource("examples/bitmask/lorem.txt").readText().trimEnd()
        val keyEntropy = "723089ec870a91fa5f6134abb5dfcc821611f67f0cca16031e6e4aaa"

        val samples = listOf("01010100", "00010000", "02000200", "00000001").associate { mask ->
            Key(mask + keyEntropy) to Image.loadResource("examples/bitmask/0x$mask.png")
        }

        test("Each Key can read its own image") {
            samples.forEach { (key, carrier) ->
                key.toHexString().asClue {
                    carrier.show(key)
                        .shouldNotBeNull()
                        .shouldBeInstanceOf<Payload>()
                        .message.shouldBe(lorem)
                }
            }
        }

        test("No Key can read other's image") {
            samples.keys.forEach { key ->
                key.toHexString().asClue {
                    samples.filterKeys { it != key }.values.forEach { otherCarrier ->
                        shouldNotThrowAny { otherCarrier.show(key) }.shouldBeNull()
                    }
                }
            }
        }
    }

    test("Nostradamus - Can extract different messages with different keys") {
        val nostradamus = Image.loadResource("examples/nostradamus/baboonKnowsAll.png")
        val payloadA = "I foresee that Team Red shall be victorious!"
        val payloadB = "In the starts it was written, and so it shall be: Team Blue will prevail!"
        val payloadC = "I play both sides so I always come out on top. It will be of course, a tie!"
        val keyA = Key("0100000075df56f4044d72f004d34f15e3ba12801fe5620a7a0a7246e2e29832")
        val keyB = Key("00010000c8a9e19ee0165902aa25666cd3a9c1d0dc804e90681ae458ca693ab5")
        val keyC = Key("00000100e3f970ddd747f3de8f30f5f9416b97edaa1091d167fb6801afddea89")

        withClue("Could not read payload A") {
            nostradamus.show(keyA)
                .shouldNotBeNull()
                .shouldBeInstanceOf<Payload>()
                .message.shouldBe(payloadA)
        }
        withClue("Could not read payload B") {
            nostradamus.show(keyB)
                .shouldNotBeNull()
                .shouldBeInstanceOf<Payload>()
                .message.shouldBe(payloadB)
        }
        withClue("Could not read payload C") {
            nostradamus.show(keyC)
                .shouldNotBeNull()
                .shouldBeInstanceOf<Payload>()
                .message.shouldBe(payloadC)
        }
    }
})
