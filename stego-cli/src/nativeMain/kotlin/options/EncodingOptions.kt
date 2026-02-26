package io.github.jadarma.stego.cli.options

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parsers.OptionInvocation

/** Provides options for steganography encoding. */
class EncodingOptions : OptionGroup(
    name = "Encoding Options",
    help = "Customise algorithm parameters.",
) {
    val usePassphrase: Boolean by option(
        "-p", "--passphrase",
        help = """
            Prompts for a passphrase instead of generating a random key.
            More convenient, but less secure.
            Prefer not overusing the same passphrase for important files.
        """.trimIndent(),
    ).nullableFlag().default(false)

    val bitmask: Int by option(
        "-b", "--bitmask",
        help = """
            An unsigned 32-bit mask _(given as hex or decimal)_ for an RGBA pixel value, set bits will overwrite the
            original image value with data from the payload.
            It is recommended to use the least significant bits to make it more stealthy and not use the alpha channel,
            especially if the original image doesn't make use of transparency.
            One can also use many or all bits, leading to chaos resembling glitch art.
            For obvious reasons, value _cannot be zero_.
            _NOTE:_ When using a custom bitmask, the **--passphrase** option cannot be used!
        """.trimIndent(),
        helpTags = mapOf("Examples" to "0x01020100 == 1690854"),
    )
        .convert { if (it.startsWith("0x")) it.removePrefix("0x").hexToInt() else it.toInt() }
        .default(
            DEFAULT_BITMASK,
            defaultForHelp = "${DEFAULT_BITMASK.toHexString()} - least significant bit in color channels"
        )
        .validate { require(it != 0) { "The bitmask must have at least one bit set." } }

    val noise: Boolean by option(
        "--noise",
        help = """
            Choose whether to write random data in the pixels not used to encode the payload.
            The noise attempts to explain away imperfections caused by the payload bitmask as noise or compression
            artifacts in the carrier image, as well as make it more difficult to locate pixels of the payload when the
            original image is known.
        """.trimIndent()
    ).flag("--no-noise", default = true, defaultForHelp = "Enabled")

    override fun finalize(
        context: Context,
        invocationsByOption: Map<Option, List<OptionInvocation>>,
    ) {
        super.finalize(context, invocationsByOption)
        val info = context.terminal.theme.info
        if (usePassphrase && bitmask != DEFAULT_BITMASK) {
            throw UsageError("Cannot set a custom ${info("--bitmask")} when using a ${info("--passphrase")}!")
        }
    }

    private companion object {
        const val DEFAULT_BITMASK: Int = 0x01010100
    }
}
