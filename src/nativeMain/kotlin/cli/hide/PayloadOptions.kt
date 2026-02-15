package io.github.jadarma.steggo.cli.hide

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.io.files.Path

/** The source of the data to hide in the image. */
sealed interface PayloadSource {

    /** The payload is an immediate, plain [text] value. */
    data class Message(val text: String) : PayloadSource

    /** The payload should be read form a file with a given [path]. */
    data class FromFile(val path: Path) : PayloadSource
}

/** Creates a mutually exclusive option group that returns the [PayloadOptions]. */
@Suppress("FunctionName")
fun ParameterHolder.PayloadOptions() = mutuallyExclusiveOptions(
    name = "Payload Options",
    help = "Choose what will be hidden in the image.",
    option1 = option(
        "-m",
        "--message",
        metavar = "text",
        help = "Hide the given plain-text string.",
    ).convert { PayloadSource.Message(it) },
    option2 = option(
        "-d",
        "--data",
        metavar = "path",
        help = "Hide the contents of this file.",
    ).convert { PayloadSource.FromFile(Path(it)) },
).single().required()
