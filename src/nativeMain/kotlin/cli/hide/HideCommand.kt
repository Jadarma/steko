package io.github.jadarma.steggo.cli.hide

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import io.github.jadarma.steggo.cli.util.load
import io.github.jadarma.steggo.cli.util.readFile
import io.github.jadarma.steggo.cli.util.write
import io.github.jadarma.steggo.core.Image
import io.github.jadarma.steggo.core.Key
import kotlinx.io.files.SystemFileSystem

/** Subcommand for hiding a payload inside an image file. */
class HideCommand : CliktCommand("hide") {

    override val printHelpOnEmptyArgs: Boolean = true

    override fun help(context: Context): String {
        return """
            Hide a message or file in an image.
            
            By default, prompts for a password (or reads from _STDIN_, when not interactive) and uses it to generate a
            steganography key to embed the payload in the image.\
            For more security, a unique random key can also be generated using **--keygen**.
            It will be printed _STDOUT_ and needs to be saved as it is the only way to recover the payload.
        """.trimIndent()
    }

    override fun helpEpilog(context: Context): String {
        val header = context.terminal.theme.warning
        val example = context.terminal.theme.muted
        return """
            ${header("Examples")}:
         
            - Hide a text message:\
              ${example("stego hide -m 'Hello' -i in.png -o out.png")}
            
            - Hide a file:\
              ${example("stego hide -d secret.md -i in.png -o out.png")}
              
            - Edit the image in-place:\
              ${example("stego hide -d secret.md -e image.png")}
              
            - Use passphrase from a file:\
              ${example("stego hide -m 'Hello' -e image.png < secret.key")}
              
            - Generate a random key:\
              ${example("stego hide --keygen -m 'Hello' -e image.png")}
        """.trimIndent()
    }

    val payloadSource by PayloadOptions()
    val imageFiles by ImageFileOptions()
    val encodingOptions by EncodingOptions()

    override fun run() {
        //TODO: Handle passphrase, currently only random key is supported.
        if (!encodingOptions.randomKey) {
            throw UsageError("Passphrase is not implemented. Please use ${terminal.theme.info("--keygen")}")
        }

        val image = Image.load(imageFiles.input)
        val payload = when (val source = payloadSource) {
            is PayloadSource.Message -> source.text.encodeToByteArray()
            is PayloadSource.FromFile -> SystemFileSystem.readFile(source.path)
        }

        val key = Key.generate(encodingOptions.bitmask)
        image.hide(key, payload, encodingOptions.noise)

        echo(key.toHexString())

        image.write(imageFiles.output)
    }
}
