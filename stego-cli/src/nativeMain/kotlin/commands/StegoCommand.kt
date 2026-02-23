package io.github.jadarma.stego.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter
import com.github.ajalt.clikt.parameters.options.versionOption
import io.github.jadarma.stego.cli.util.FileSystem
import io.github.jadarma.stego.cli.util.RealFileSystem

class StegoCommand(
    private val fileSystem: FileSystem = RealFileSystem,
) : CliktCommand("stego") {

    override val printHelpOnEmptyArgs: Boolean = true

    init {
        installMordantMarkdown()
        versionOption("0.1.0")

        context {
            helpFormatter = { ctx ->
                MordantMarkdownHelpFormatter(
                    ctx,
                    requiredOptionMarker = "*",
                    showRequiredTag = true,
                    showDefaultValues = true,
                )
            }
        }
    }

    override fun run() {
        currentContext.findOrSetObject("fs") { fileSystem }
    }
}
