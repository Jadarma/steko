package io.github.jadarma.steggo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter

class StegoCommand : CliktCommand("stego") {

    override val printHelpOnEmptyArgs: Boolean = true

    init {
        installMordantMarkdown()

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
    }
}
