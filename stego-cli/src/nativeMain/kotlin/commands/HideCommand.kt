package io.github.jadarma.stego.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.mordant.terminal.ConfirmationPrompt
import io.github.jadarma.stego.cli.options.EncodingOptions
import io.github.jadarma.stego.cli.options.ImageFileOptions
import io.github.jadarma.stego.cli.options.PayloadOptions
import io.github.jadarma.stego.cli.util.exitError
import io.github.jadarma.stego.cli.util.load
import io.github.jadarma.stego.cli.util.readFile
import io.github.jadarma.stego.cli.util.write
import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.github.jadarma.stego.core.Payload
import kotlinx.io.files.SystemFileSystem

/** Subcommand for hiding a payload inside an image file. */
class HideCommand : CliktCommand("hide") {

    override val printHelpOnEmptyArgs: Boolean = true

    override fun help(context: Context): String = """
        Hide a message and / or file attachments in an image.

        The generated recovery key is printed to _STDOUT_.
        Store it securely, on a different medium or filesystem from the resulting image.
        It is the only way to recover the original payload.
    """.trimIndent()

    override fun helpEpilog(context: Context): String {
        val header = context.terminal.theme.warning
        val example = context.terminal.theme.muted
        return """
            ${header("Examples")}:
         
            - Hide a text message:\
              ${example("stego hide -m 'Hello' -i in.png -o out.png")}
            
            - Hide a file:\
              ${example("stego hide -a secret.md -i in.png -o out.png")}
              
            - Edit the image in-place:\
              ${example("stego hide -a secret.md -e image.png")}
              
            - Use passphrase:\
              ${example("stego hide -p -m 'Hello' -e image.png")}
        """.trimIndent()
    }

    val payloads by PayloadOptions()
    val imageFiles by ImageFileOptions()
    val encodingOptions by EncodingOptions()

    override fun run() {
        val key = getKey()
        val image = Image.load(imageFiles.input)

        val payload = Payload(
            message = payloads.message,
            attachments = payloads.attachments.associate { it.name to SystemFileSystem.readFile(it) },
        )

        image
            .hide(key, payload, encodingOptions.noise)
            .write(imageFiles.output)

        echo(key.toHexString())
    }

    private fun getKey(): Key {
        if (encodingOptions.usePassphrase.not()) return Key.generate(encodingOptions.bitmask)

        val value = when {
            terminal.terminalInfo.interactive -> ConfirmationPrompt.createString(
                firstPrompt = "Enter a passphrase",
                secondPrompt = "Verify passphrase",
                terminal = terminal,
                hideInput = true,
                valueMismatchMessage = "Passphrases did not match.",
            ).ask().also { echo() }
            !terminal.terminalInfo.inputInteractive -> readlnOrNull()
            else -> exitError("No input on STDIN given.")
        } ?: exitError("Could not get credential.")

        return Key.generate(passphrase = value)
    }
}
