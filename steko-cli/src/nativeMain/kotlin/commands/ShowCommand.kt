package io.github.jadarma.steko.cli.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import io.github.jadarma.steko.cli.util.*
import io.github.jadarma.steko.core.*
import kotlinx.io.files.Path

class ShowCommand : CliktCommand() {

    override val printHelpOnEmptyArgs: Boolean = true

    val fileSystem by requireObject<FileSystem>("fs")

    val usePassphrase: Boolean by option(
        "-p", "--passphrase",
        help = "Treats the input as a passphrase to derive from, instead of reading the actual key in hex format",
    ).nullableFlag().default(false)

    val outputDirectory: Path? by option(
        "-o", "--out",
        metavar = "path",
        help = """
            The directory to write the attachments to.
            The file(s) will be named according to it's own metadata.
        """.trimIndent(),
    )
        .convert { Path(it) }
        .validateCatching { path -> path.takeUnless { it.toString() == "-" }?.let(fileSystem::checkDirectory) }

    val imagePath: Path by argument(
        name = "image",
        help = "Path to the carrier image to attempt to extract a payload from.",
        helpTags = mapOf("Formats" to ".png, .rgba"),
        completionCandidates = CompletionCandidates.Path,
    )
        .convert { Path(it) }
        .validateCatching { fileSystem.checkFile(it) }

    override fun help(context: Context): String {
        val info = context.terminal.theme.info
        return """
            Recover previously hidden data from an image.

            Reads the key from _STDIN_ and attempts to extract the payload.
            
            If the payload has a _message_, it will always be printed to _STDOUT_.
            When in interactive mode, attachment names and sizes will also be displayed.
            To view the contents of attachments, save them to a specified directory with the ${info("--out")}
            option.
            
            _NOTE:_ It is also possible the payload doesn't contain the default Steko metadata.
            In that case, the entire raw payload will be printed to _STDOUT_.
        """.trimIndent()
    }

    override fun helpEpilog(context: Context): String {
        val header = context.terminal.theme.warning
        val example = context.terminal.theme.muted
        return """
            ${header("Examples")}:
            
            - Extract a payload:\
              ${example("steko show image.png < secret.key")}

            - Save attachments to disk:\
              ${example("steko show -o /tmp/steko image.png < secret.key")}

            - Use a passphrase:\
              ${example("steko show -p image.png")}              
        """.trimIndent()
    }

    override fun run() {
        val key = getKey()
        val image = Image.load(fileSystem, imagePath)
        val payload = image.showBlocking(key) ?: exitError("Could not find any secret using this key.", 2)

        if (payload is RawPayload) {
            printToStdOut(payload.data)
            return
        }

        val message = (payload as Payload).message
        if (message != null) {
            if (terminal.terminalInfo.outputInteractive) {
                terminal.println(Markdown(message))
            } else {
                printToStdOut(message.encodeToByteArray())
            }
        }

        val attachments = payload.attachments.takeIf { it.isNotEmpty() } ?: return

        val outDir = outputDirectory
        if (outDir != null) {
            attachments.forEach { (name, content) ->
                fileSystem.writeFile(Path(outDir, name), content)
            }
        }

        if (terminal.terminalInfo.outputInteractive) {
            if (message != null) terminal.println(Markdown("---"))
            terminal.printAttachments(attachments, previewOnly = outDir == null)
        }
    }

    /*
    * Reads the key from _STDIN_, or offers a prompt if in interactive mode.
    * If it expects a key, will parse it from hexadecimal, otherwise will generate one from the passphrase given.
    */
    private fun getKey(): Key {
        val target = if (usePassphrase) "passphrase" else "key"
        val value = when {
            usePassphrase && terminal.terminalInfo.interactive ->
                terminal
                    .prompt("Enter a passphrase", hideInput = true)
                    .also { echo() }

            else -> readlnOrNull()
        } ?: exitError("Could not read $target from STDIN.")

        return runCatching {
            if (usePassphrase) Key.generateBlocking(value) else Key(value)
        }.getOrElse {
            exitError("Could not parse $target.")
        }
    }

    /** Nicely render the attachments as a Markdown preview.*/
    private fun Terminal.printAttachments(attachments: Map<String, ByteArray>, previewOnly: Boolean) {
        if (attachments.isEmpty()) return
        with(StringBuilder()) {
            append("**")
            if (!previewOnly) append("Extracted ")
            appendLine("Attachments (${attachments.size}):**")

            for ((name, content) in attachments) {
                append(" - **$name** ")
                var size = content.size
                var unit = "B"
                if (size > 1024) {
                    size /= 1024; unit = "KiB"
                }
                if (size > 1024) {
                    size /= 1024; unit = "MiB"
                }
                appendLine(theme.muted("(${size}$unit)"))
            }

            if (previewOnly) {
                appendLine()
                append("_")
                append(theme.muted("(Hint: Attachments can be extracted by specifying the --out directory.)"))
                appendLine("_")
            }
            toString()
        }.let(::Markdown).let(::print)
    }
}
