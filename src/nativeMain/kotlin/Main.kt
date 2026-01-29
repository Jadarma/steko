package io.github.jadarma.steggo

import io.github.jadarma.steggo.impl.StbImageHandler

fun main() {

    val inputPath = "./input.png"
    val outputPath = "./output.png"

    val image = StbImageHandler.read(inputPath)

    println(
        """
            Image Loaded:
              - Path: $inputPath
              - Size: ${image.width}x${image.height}
              - Channels: ${if (image.hasAlphaChannel) "RGBA" else "RGB"}
        """.trimIndent()
    )

    StbImageHandler.write(image, outputPath)

    println(
        """
            Image Written:
              - Path: $outputPath
        """.trimIndent()
    )
}
