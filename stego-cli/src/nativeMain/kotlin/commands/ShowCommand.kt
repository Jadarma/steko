package io.github.jadarma.stego.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.mordant.terminal.prompt
import io.github.jadarma.stego.cli.util.checkDirectory
import io.github.jadarma.stego.cli.util.exitError
import io.github.jadarma.stego.cli.util.load
import io.github.jadarma.stego.cli.util.printToStdOut
import io.github.jadarma.stego.cli.util.writeFile
import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import io.github.jadarma.stego.core.RawPayload
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
        val key = getKey()
        val image = Image.load(imagePath)
        val payload = image.show(key) ?: exitError("Could not find any secret using this key.", 2)

        // TODO: Handle the default payload
        if(payload !is RawPayload) exitError("Not implemented yet")

        when (val dir = outputDirectory) {
            null -> printToStdOut(payload.data)
            else -> SystemFileSystem.writeFile(Path(dir, "secret.out"), payload.data)
        }
    }

    /*
    * Reads the key from _STDIN_, or offers a prompt if in interactive mode.
    * If it expects a key, will parse it from hexadecimal, otherwise will generate one from the passphrase given.
    */
    private fun getKey(): Key {
        val value = when {
            isKey -> readlnOrNull()
            terminal.terminalInfo.interactive -> terminal.prompt("Enter a passphrase", hideInput = true)
            else -> readlnOrNull()
        } ?: exitError("Could not read credential.")

        return runCatching {
            if (isKey) Key(value) else Key.generate(value)
        }.getOrElse {
            exitError("Could not parse key.")
        }
    }
}
