package io.github.jadarma.steko.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter
import com.github.ajalt.clikt.parameters.options.versionOption
import io.github.jadarma.steko.cli.util.FileSystem
import io.github.jadarma.steko.cli.util.RealFileSystem
import io.github.jadarma.steko.cli.BuildConfig.APP_VERSION

class StekoCommand(
    private val fileSystem: FileSystem = RealFileSystem,
) : CliktCommand("steko") {

    override val printHelpOnEmptyArgs: Boolean = true

    init {
        installMordantMarkdown()
        versionOption(APP_VERSION)

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
