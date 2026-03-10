package io.github.jadarma.steko.cli.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.mordant.terminal.ConfirmationPrompt
import io.github.jadarma.steko.cli.options.EncodingOptions
import io.github.jadarma.steko.cli.options.ImageFileOptions
import io.github.jadarma.steko.cli.util.*
import io.github.jadarma.steko.core.*
import kotlinx.io.files.Path

/** Subcommand for hiding a payload inside an image file. */
class HideCommand : CliktCommand("hide") {

    override val printHelpOnEmptyArgs: Boolean = true
    val fileSystem by requireObject<FileSystem>("fs")

    val imageFiles by ImageFileOptions()
    val encodingOptions by EncodingOptions()

    val message: String? by option(
        "-m",
        "--message",
        metavar = "text",
        help = """
            Hide the given plain-text string.
            Ideal for short memos or giving instructions for file attachments.
        """.trimIndent(),
    ).validate { text -> require(text.isNotBlank()) { "Text cannot be empty." } }

    val attachments: List<Path> by argument(
        name = "attachment",
        help = "Path to a file to embed as an attachment.",
        completionCandidates = CompletionCandidates.Path,
    )
        .convert { Path(it) }
        .multiple()
        .validateCatching { paths -> paths.forEach { fileSystem.checkFile(it) } }

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
              ${example("steko hide -i in.png -o out.png -m 'Hello'")}

            - Hide a file:\
              ${example("steko hide -i in.png -o out.png secret.md")}

            - Edit the image in-place:\
              ${example("steko hide -e image.png secret.md")}

            - Use passphrase:\
              ${example("steko hide -p -e image.png -m 'Hello'")}
        """.trimIndent()
    }

    @Suppress("ThrowsCount")
    override fun run() {
        val info = terminal.theme.info
        if (message == null && attachments.isEmpty()) {
            throw UsageError("No payload given. Please define a ${info("--message")} and / or pass files as arguments.")
        }
        if (encodingOptions.rawPayload) {
            if (message != null) {
                throw UsageError("The ${info("--message")} and ${info("--raw")} options are incompatible.")
            }
            if (attachments.size != 1) {
                throw UsageError("When ${info("--raw")} mode is used, exactly one attachment argument is required.")
            }
        }

        val key = getKey()
        val image = Image.load(fileSystem, imageFiles.input)

        val payload = if (encodingOptions.rawPayload) {
            RawPayload(fileSystem.readFile(attachments.single()))
        } else {
            Payload(
                message = message,
                attachments = attachments.associate { it.name to fileSystem.readFile(it) },
            )
        }

        image
            .runCatching { hideBlocking(key, payload, encodingOptions.noise) }
            .getOrElse { cause ->
                if (cause is IndexOutOfBoundsException) exitError(
                    "Payload cannot fit in this image" + cause.message.orEmpty() + ". Use a larger image, or bitmask.",
                    3
                )
                exitError("Unexpected error occurred.", 4)
            }
            .write(fileSystem, imageFiles.output)

        echo(key.toHexString())
    }

    private fun getKey(): Key {
        if (!encodingOptions.useKey && !encodingOptions.usePassphrase) {
            return Key.generate(encodingOptions.bitmask)
        }

        if(encodingOptions.useKey) {
            if(terminal.terminalInfo.inputInteractive) throw UsageError("No input given to STDIN.")
            val value = readlnOrNull() ?: exitError("Could not get credential.")
            return Key(value)
        }

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

        return Key.generateBlocking(passphrase = value)
    }
}
