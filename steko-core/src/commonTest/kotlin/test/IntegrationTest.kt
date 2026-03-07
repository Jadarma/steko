package io.github.jadarma.steko.core.test

import com.goncalossilva.resources.Resource
import io.github.jadarma.steko.core.Payload
import io.github.jadarma.steko.core.RawPayload
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec

class IntegrationTest : FunSpec({

    val original = Resource("examples/original.rgba")

    context("Bitmasks") {

        val payload = Payload("Hello, World!")
        val keys = listOf("k00_a1", "k01_g1", "k02_r1g1b1", "k03_r2b2").map { Resource("examples/keys/$it.key") }
        val expected = listOf("k00", "k01", "k02", "k03").map { Resource("examples/outputs/${it}.rgba") }

        test("Can use different bitmasks to hide payload.") {
            keys.indices.forEach { index ->
                withClue("Key k0$index did not encode to expected image.") {
                    hidingTest(
                        original = original,
                        hexKey = keys[index],
                        expected = expected[index],
                        noise = false,
                        payload = payload,
                    )
                }
            }
        }

        test("Each key may only decode its own payload.") {
            keys.indices.forEach { keyIndex ->
                expected.indices.forEach { imageIndex ->
                    withClue("Key k0$keyIndex did not decode image k0${imageIndex}.rgba properly.") {
                        showingTest(
                            carrier = expected[imageIndex],
                            hexKey = keys[keyIndex],
                            expected = payload.takeIf { keyIndex == imageIndex },
                        )
                    }
                }
            }
        }
    }

    context("Noise") {
        val payload = Payload("Hello, World!")
        val key = Resource("examples/keys/k50_hunter2.key")

        test("Can encode with noise.") {
            hidingTest(
                original = original,
                expected = Resource("examples/outputs/k50_noise.rgba"),
                hexKey = key,
                noise = true,
                payload = payload,
            )
        }

        test("Can encode without noise.") {
            hidingTest(
                original = original,
                expected = Resource("examples/outputs/k50_clean.rgba"),
                hexKey = key,
                noise = false,
                payload = payload,
            )
        }

        test("Noise does not affect decoding") {
            showingTest(
                carrier = Resource("examples/outputs/k50_clean.rgba"),
                hexKey = key,
                expected = payload,
            )
            showingTest(
                carrier = Resource("examples/outputs/k50_noise.rgba"),
                hexKey = key,
                expected = payload,
            )
        }

    }

    context("Payloads") {

        val key = Resource("examples/keys/k10_r3g3b3.key")

        test("Can hide simple messages.") {
            val expected = Resource("examples/outputs/k10_message.rgba")
            val payload = Payload("Hello, World!")
            hidingTest(
                original = original,
                expected = expected,
                hexKey = key,
                noise = true,
                payload = payload,
            )
            showingTest(
                carrier = expected,
                hexKey = key,
                expected = payload,
            )
        }

        test("Can hide attachments.") {
            val expected = Resource("examples/outputs/k10_attachments.rgba")
            val payload = Payload(mapOf("p1_lorem.md" to Resource("examples/payloads/p1_lorem.md").readBytes()))

            hidingTest(
                original = original,
                expected = expected,
                hexKey = key,
                noise = true,
                payload = payload,
            )
            showingTest(
                carrier = expected,
                hexKey = key,
                expected = payload,
            )
        }

        test("Can hide messages and attachments.") {
            val expected = Resource("examples/outputs/k10_combo.rgba")
            val payload = Payload(
                message = "Hello, World!",
                attachments = mapOf("p1_lorem.md" to Resource("examples/payloads/p1_lorem.md").readBytes()),
            )

            hidingTest(
                original = original,
                expected = expected,
                hexKey = key,
                noise = true,
                payload = payload,
            )
            showingTest(
                carrier = expected,
                hexKey = key,
                expected = payload,
            )
        }

        test("Can hide raw data.") {
            val expected = Resource("examples/outputs/k10_raw.rgba")
            val payload = RawPayload(Resource("examples/payloads/p0_image.png").readBytes())

            hidingTest(
                original = original,
                expected = expected,
                hexKey = key,
                noise = true,
                payload = payload,
            )
            showingTest(
                carrier = expected,
                hexKey = key,
                expected = payload,
            )
        }
    }

    context("Nostradamus") {

        val payloadA = Payload("I foresee that Team Red shall be victorious!")
        val payloadB = Payload("In the starts it was written, and so it shall be: Team Blue will prevail!")
        val payloadC = Payload("I play both sides so I always come out on top. It will be of course, a tie!")
        val keyA = Resource("examples/keys/k20_r1.key")
        val keyB = Resource("examples/keys/k30_g1.key")
        val keyC = Resource("examples/keys/k40_b1.key")

        test("Can encode with multiple non-overlapping keys.") {
            hidingTest(
                original = original,
                expected = Resource("examples/outputs/k20_nostradamus.rgba"),
                hexKey = keyA,
                noise = true,
                payload = payloadA,
            )
            hidingTest(
                original = Resource("examples/outputs/k20_nostradamus.rgba"),
                expected = Resource("examples/outputs/k30_nostradamus.rgba"),
                hexKey = keyB,
                noise = true,
                payload = payloadB,
            )
            hidingTest(
                original = Resource("examples/outputs/k30_nostradamus.rgba"),
                expected = Resource("examples/outputs/k40_nostradamus.rgba"),
                hexKey = keyC,
                noise = true,
                payload = payloadC,
            )
        }

        test("Can decode with multiple non-overlapping keys.") {
            showingTest(
                carrier = Resource("examples/outputs/k40_nostradamus.rgba"),
                hexKey = keyA,
                expected = payloadA,
            )
            showingTest(
                carrier = Resource("examples/outputs/k40_nostradamus.rgba"),
                hexKey = keyB,
                expected = payloadB,
            )
            showingTest(
                carrier = Resource("examples/outputs/k40_nostradamus.rgba"),
                hexKey = keyC,
                expected = payloadC,
            )
        }
    }
})
