package io.github.jadarma.steggo.cli.show

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.*
import io.github.jadarma.steggo.cli.util.*
import io.github.jadarma.steggo.core.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class ShowCommand : CliktCommand() {

    override val printHelpOnEmptyArgs: Boolean = true

    override fun help(context: Context): String {
        return """
            Prompts for a password (or reads from _STDIN_, when not interactive) and attempts to extract the payload.
            
            By default, the payload will be printed to _STDOUT_.
            When in interactive mode, non-text payloads will not be printed, instead the filename and size will be
            displayed.
            
            Alternatively, the payload can be saved as a file attachment in a specified directory, restoring its 
            original filename.
        """.trimIndent()
    }

    override fun helpEpilog(context: Context): String {
        val header = context.terminal.theme.warning
        val example = context.terminal.theme.muted
        return """
            ${header("Examples")}:
         
            - Extract a payload:\
              ${example("stego show -i image.png")}
            
            - Use a keyfile:\
              ${example("stego show -k -i image.png < secret.key")}
              
            - Save to disk _(here, current directory)_:\
              ${example("stego show -i image.png -o .")}
        """.trimIndent()
    }

    val isKey: Boolean by option(
        "-k", "--key",
        help = "Treats the input as the actual key in hex format, instead of using it as a passphrase to generate one.",
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
        // TODO: Handle passphrase. Currently only using keys is supported.
        if (!isKey) {
            throw UsageError("Passphrases are not supported yet. Please use ${terminal.theme.info("--key")}")
        }

        val image = Image.load(imagePath)
        val keyValue = runCatching { readln() }.getOrElse { exitError("No key was provided.") }
        val key = runCatching { Key(keyValue) }.getOrElse { exitError("Key is invalid.") }

        val data = image.show(key) ?: exitError("Could not find any secret using this key.", 2)

        when (val dir = outputDirectory) {
            null -> printToStdOut(data)
            else -> SystemFileSystem.writeFile(Path(dir, "secret.out"), ByteString(data))
        }
    }
}
