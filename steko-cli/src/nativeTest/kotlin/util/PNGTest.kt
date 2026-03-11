package io.github.jadarma.steko.cli.util

import com.goncalossilva.resources.Resource
import io.github.jadarma.steko.core.Image
import io.github.jadarma.steko.core.Image.Companion.decodeFromRgba
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PNGTest : FunSpec({

        val rgbaImage = decodeFromRgba(Resource("images/image.rgba").readBytes(), 512 to 512)
        val pngFile = Resource("images/image.png").readBytes()

        test("Can encode") {
            rgbaImage.encodeToPng() contentEquals pngFile shouldBe true
        }
        test("Can decode") {
            val decoded = Image.decodeFromPng(pngFile)
            decoded.width shouldBe 512
            decoded.height shouldBe 512
            decoded.pixels contentEquals rgbaImage.pixels shouldBe true
        }
})
