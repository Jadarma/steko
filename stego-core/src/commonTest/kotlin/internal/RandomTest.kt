package io.github.jadarma.stego.core.internal

import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlin.repeat

class RandomTest : FunSpec({

    test("The key can be used as a seed") {
        val key = Key.generate()
        val r1 = key.toRandom()
        val r2 = key.toRandom()
        r1 shouldNotBeSameInstanceAs r2

        repeat(10_000) { iteration ->
            withClue("RNG broke after $iteration") {
                r1.nextInt() shouldBe r2.nextInt()
            }
        }
    }

    test("The PRNG is deterministic") {
        //@formatter:off
        val expected = intArrayOf(
            -97388091, 2020014945, 311697746, -721525266, 1645815958, 337449879, -1974754915, 648063352, 1551910527,
            -1273279137, 237974108, 2078218432, -679752009, 2072614431, -1909816457, 1292476557, -850456412, -67396796,
            857101768, 1963863238, 753240837, -1758357294, 43191297, 1063637179, 1780874062, -524054682, -2039776968,
            -679866712, 1021764591, -1500592882, -1861979346, 1540646649, -14432219, 580014023, -533472139, 1997913666,
            1828437334, -546702054, -1645810581, 115635970, 784196604, -7431629, 284796544, 683807921, 279666819,
            -10302208, -589997229, -39656568, -1665887772, -40605797,
        )
        //@formatter:on

        val random = Key.generate("hunter2").toRandom()
        expected.forEachIndexed { index, n ->
            withClue("Did not produce expected value for the index $index.") {
                random.nextInt() shouldBe n
            }
        }
    }

    test("The image pixel order is the same when using the same key.") {
        val image = Image(100, 100, IntArray(100 * 100))
        val keyA = Key.generate()
        val keyB = Key(keyA.toHexString())
        val keyC = Key.generate()
        val shuffledByA = image.shufflePixels(keyA)
        val shuffledByB = image.shufflePixels(keyB)
        val shuffledByC = image.shufflePixels(keyC)

        shuffledByA shouldNotBeSameInstanceAs shuffledByB
        shuffledByB shouldNotBeSameInstanceAs shuffledByC

        shuffledByA contentEquals shuffledByB shouldBe true
        shuffledByA contentEquals shuffledByC shouldBe false

        shuffledByA.size shouldBe image.pixels.size
        shuffledByB.size shouldBe image.pixels.size
        shuffledByC.size shouldBe image.pixels.size
    }
})
