package io.github.jadarma.stego.cli.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.mordant.terminal.ConfirmationPrompt
import io.github.jadarma.stego.cli.options.EncodingOptions
import io.github.jadarma.stego.cli.options.ImageFileOptions
import io.github.jadarma.stego.cli.util.*
import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.github.jadarma.stego.core.Payload
import kotlinx.io.files.Path
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
              ${example("stego hide -i in.png -o out.png -m 'Hello'")}
            
            - Hide a file:\
              ${example("stego hide -i in.png -o out.png secret.md")}
              
            - Edit the image in-place:\
              ${example("stego hide -e image.png secret.md")}
              
            - Use passphrase:\
              ${example("stego hide -p -e image.png -m 'Hello'")}
        """.trimIndent()
    }

    val imageFiles by ImageFileOptions()
    val encodingOptions by EncodingOptions()

    val message: String? by option(
        "-m",
        "--message",
        metavar = "text",
        help = "Hide the given plain-text string.",
    ).validate { text -> require(text.isNotBlank()) { "Text cannot be empty." } }

    val attachments: List<Path> by argument(
        name = "attachment",
        help = "Path to a file to embed as an attachment.",
        completionCandidates = CompletionCandidates.Path,
    )
        .convert { Path(it) }
        .multiple()
        .validateCatching { paths -> paths.forEach { SystemFileSystem.checkFile(it) } }

    override fun run() {
        if (message == null && attachments.isEmpty()) {
            val info = terminal.theme.info
            throw UsageError("No payload given. Please define a ${info("--message")} and / or pass files as arguments.")
        }

        val key = getKey()
        val image = Image.load(imageFiles.input)

        val payload = Payload(
            message = message,
            attachments = attachments.associate { it.name to SystemFileSystem.readFile(it) },
        )

        image
            .runCatching { hide(key, payload, encodingOptions.noise) }
            .getOrElse { cause ->
                if (cause is IndexOutOfBoundsException) exitError(
                    "Payload cannot fit in this image" + cause.message.orEmpty() + ". Use a larger image, or bitmask.",
                    3
                )
                exitError("Unexpected error occurred.", 4)
            }
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
            terminal.terminalInfo.inputInteractive -> throw UsageError("No input given to STDIN.")
            else -> readlnOrNull()
        } ?: exitError("Could not get credential.")

        return Key.generate(passphrase = value)
    }
}
