package io.github.jadarma.stego.cli.options

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import io.github.jadarma.stego.cli.util.checkFile
import io.github.jadarma.stego.cli.util.validateCatching
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class PayloadOptions : OptionGroup(
    name = "Payload Options",
    help = "Choose what will be hidden in the image.",
) {
    val message: String? by option(
        "-m",
        "--message",
        metavar = "text",
        help = "Hide the given plain-text string.",
    ).validate { text -> require(text.isNotBlank()) { "Text cannot be empty." } }

    val attachments: List<Path> by option(
        "-a",
        "--attachment",
        metavar = "path",
        help = "Path to a file to attach. Multiple attachments are allowed."
    )
        .convert { Path(it) }
        .multiple()
        .validateCatching { paths -> paths.forEach { SystemFileSystem.checkFile(it) } }

    override fun postValidate(context: Context) {
        super.postValidate(context)
        if (message == null && attachments.isEmpty()) {
            val info = context.terminal.theme.info
            throw UsageError("No payload given. Please define a ${info("--message")} and / or ${info("--attachment")}s")
        }
    }
}
