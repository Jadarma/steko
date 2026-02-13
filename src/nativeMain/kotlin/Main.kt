package io.github.jadarma.steggo

import io.github.jadarma.steggo.core.Key
import io.github.jadarma.steggo.core.Payload
import io.github.jadarma.steggo.core.Stego
import io.github.jadarma.steggo.core.TextPayload
import io.github.jadarma.steggo.impl.StbImageHandler

fun main() {
    val inputPath = "./input.png"
    val outputPath = "./output.png"
    val payload = TextPayload("Hello, World!")

    val key = hide(inputPath, payload, outputPath)
    reveal(outputPath, key.toString())
}

private fun hide(input: String, payload: Payload, output: String): Key {
    val image = StbImageHandler.read(input)
    println(
        """
            Image Loaded:
              - Path: $input
              - Size: ${image.width}x${image.height}
        """.trimIndent()
    )
    val key = Stego.hide(image, payload, bitmask = 0x000000F0u , noise = true)
    StbImageHandler.write(image, output)
    println(
        """
            Image Written:
              - Path: $output
              - Key: $key
        """.trimIndent()
    )
    return key
}

private fun reveal(input: String, keyString: String) {
    val image = StbImageHandler.read(input)
    val key = Key(keyString)

    val payload = Stego.reveal(image, key)
    if(payload == null) {
        println("Failed to retrieve any embedded secret.")
        return
    }
    check(payload is TextPayload) { "Expected text payload" }
    println("""
        Extracted:
        ${payload.text}
    """.trimIndent())
}
