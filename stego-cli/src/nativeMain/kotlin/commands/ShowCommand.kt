package io.github.jadarma.stego.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import io.github.jadarma.stego.cli.util.*
import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.github.jadarma.stego.core.Payload
import io.github.jadarma.stego.core.RawPayload
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class ShowCommand : CliktCommand() {

    override val printHelpOnEmptyArgs: Boolean = true

    override fun help(context: Context): String {
        val info = context.terminal.theme.info
        return """
            Recover previously hidden data from an image.

            Reads the key from _STDIN_ and attempts to extract the payload.
            
            If the payload has a _message_, it will always be printed to _STDOUT_.
            When in interactive mode, attachment names and sizes will also be displayed.
            To view the contents of attachments, save them to a specified directory with the ${info("--out")}
            option.
            
            _NOTE:_ It is also possible the payload doesn't contain the default Stego metadata.
            In that case, the entire raw payload will be printed to _STDOUT_.
        """.trimIndent()
    }

    override fun helpEpilog(context: Context): String {
        val header = context.terminal.theme.warning
        val example = context.terminal.theme.muted
        return """
            ${header("Examples")}:
         
            - Extract a payload:\
              ${example("stego show -i image.png < secret.key")}
            
            - Use a passphrase:\
              ${example("stego show -p -i image.png")}
              
            - Save attachments to disk:\
              ${example("stego show -i image.png -o /tmp/stego < secret.key")}
        """.trimIndent()
    }

    val usePassphrase: Boolean by option(
        "-p", "--passphrase",
        help = "Treats the input as a passphrase to derive from, instead of reading the actual key in hex format",
    ).nullableFlag().default(false)

    val imagePath: Path by option(
        "-i", "--in",
        help = "Path to the image to attempt to extract a payload from.",
    ).convert { Path(it) }.required()

    val outputDirectory: Path? by option(
        "-o", "--out",
        metavar = "path",
        help = """
            The directory to write the payload in. The file will be named according to it's own metadata.
            The special value '**-**' will instead print the payload to _STDOUT_.
        """.trimIndent(),
    )
        .convert { Path(it) }
        .validate { SystemFileSystem.checkDirectory(it) }

    override fun run() {
        val key = getKey()
        val image = Image.load(imagePath)
        val payload = image.show(key) ?: exitError("Could not find any secret using this key.", 2)

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
                SystemFileSystem.writeFile(Path(outDir, name), content)
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
        val value = when {
            !usePassphrase -> readlnOrNull()
            terminal.terminalInfo.interactive -> terminal.prompt("Enter a passphrase", hideInput = true).also { echo() }
            else -> readlnOrNull()
        } ?: exitError("Could not read credential.")

        return runCatching {
            if (usePassphrase) Key.generate(value) else Key(value)
        }.getOrElse {
            exitError("Could not parse key.")
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
