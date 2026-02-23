package io.github.jadarma.stego.cli.options

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parsers.OptionInvocation
import io.github.jadarma.stego.cli.util.checkFile
import io.github.jadarma.stego.cli.util.extension
import io.github.jadarma.stego.cli.util.validateCatching
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** Provides paths for image IO operations. */
class ImageFileOptions : OptionGroup(
    name = "Image File Options",
    help = "Choose what image to read, and where to write the updated file.",
) {
    private val inputFile: Path? by option(
        "-i", "--in",
        metavar = "path",
        completionCandidates = CompletionCandidates.Path,
        help = "The path to the original image. _(readonly)_",
        helpTags = mapOf("Formats" to ".png, .rgba"),
    )
        .convert { Path(it) }
        .validateCatching { SystemFileSystem.checkFile(it) }

    private val outputFile: Path? by option(
        "-o", "--out",
        metavar = "path",
        completionCandidates = CompletionCandidates.Path,
        help = """
            The path where to write the modified image. _(Overwrites previous data!)_.
            The file extension must match that of the input image.
        """.trimIndent(),
    ).convert { Path(it) }

    private val editFile: Path? by option(
        "-e", "--edit",
        metavar = "path",
        completionCandidates = CompletionCandidates.Path,
        help = """
            Hides the payload in-place, reading the original image and writing it back to the same path.
            Doubles as explicit user consent, and is mutually exclusive with **--in** and **--out**.
        """.trimIndent(),
    )
        .convert { Path(it) }
        .validateCatching { SystemFileSystem.checkFile(it) }

    override fun finalize(
        context: Context,
        invocationsByOption: Map<Option, List<OptionInvocation>>,
    ) {
        super.finalize(context, invocationsByOption)
        val info = context.terminal.theme.info
        if (editFile != null) {
            if (inputFile != null || outputFile != null) {
                throw UsageError("the ${info("--edit")} flag cannot be used together with ${info("--in")} and ${info("--out")}")
            }
        } else {
            if (inputFile == null) {
                throw UsageError("must specify an input image with ${info("--in")} or ${info("--edit")}")
            }
            if (outputFile == null) {
                throw UsageError("must specify an output image with ${info("--out")}")
            }
            if (inputFile == outputFile) {
                throw UsageError("setting ${info("--in")} and ${info("--out")} to same path is considered an error. Did you mean to use ${info("--edit")}?")
            }
        }
        if(input.extension !in setOf("png","rgba")) {
            throw UsageError("Unknown file extension, must be either PNG or RGBA.")
        }
        if(input.extension != output.extension) {
            throw UsageError("The ${info("--in")} and ${info("--out")} formats must match.")
        }
    }

    /** The path to read the original image from. */
    val input: Path get() = editFile ?: inputFile ?: throw ProgramResult(9)

    /** The path to write the modified image to. */
    val output: Path get() = editFile ?: outputFile ?: throw ProgramResult(9)
}
