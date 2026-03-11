package io.github.jadarma.steko.cli.commands

import io.github.jadarma.steko.cli.test.testCommandBadUsage
import io.kotest.core.spec.style.FunSpec

class HideCommandTest : FunSpec({
    context("Validation") {
        testCommandBadUsage(
            name = "An input file must be specified.",
            args = "hide -m 'Hi' --out out/image.png",
            expectedReason = "Must specify an input image with --in or --edit.",
        )
        testCommandBadUsage(
            name = "Edit image must exist.",
            args = "hide -m 'Hi' --edit 404.png",
            expectedReason = "File does not exist: 404",
        )
        testCommandBadUsage(
            name = "Input image must exist.",
            args = "hide -m 'Hi' --in 404.png --out /out/image.png",
            expectedReason = "File does not exist: 404.png",
        )
        testCommandBadUsage(
            name = "Edit cannot be used with input.",
            args = "hide -m 'Hi' --edit image.png --in image.rgba",
            expectedReason = "The --edit flag cannot be used together with --in and --out.",
        )
        testCommandBadUsage(
            name = "Edit cannot be used with output.",
            args = "hide -m 'Hi' --edit image.png --out image.rgba",
            expectedReason = "The --edit flag cannot be used together with --in and --out.",
        )
        testCommandBadUsage(
            name = "If input is specified, so must output.",
            args = "hide -m 'Hi' --in image.png",
            expectedReason = "Must specify an output image with --out.",
        )
        testCommandBadUsage(
            name = "Input and output must be different files.",
            args = "hide -m 'Hi' --in image.png --out image.png",
            expectedReason = "Setting --in and --out to same path is considered an error.",
        )
        testCommandBadUsage(
            name = "Only valid extensions are allowed.",
            args = "hide -m 'Hi' -e image.jpg",
            expectedReason = "Unknown file extension, must be either PNG or RGBA.",
        )
        testCommandBadUsage(
            name = "RGBA inputs demand RGBA outputs.",
            args = "hide -m 'Hi' --in image.rgba --out out/image.png",
            expectedReason = "When --in is an .rgba file, so must --out.",
        )
        testCommandBadUsage(
            name = "Bitmasks must be a number.",
            args = "hide -m 'Hi' --bitmask invalid -e image.png",
            expectedReason = "invalid value for --bitmask",
        )
        testCommandBadUsage(
            name = "Bitmasks cannot be zero.",
            args = "hide -m 'Hi' --bitmask 0 -e image.png",
            expectedReason = "The bitmask must have at least one bit set.",
        )
        testCommandBadUsage(
            name = "Bitmasks cannot be used with passphrases.",
            args = "hide -m 'Hi' -b 1 --passphrase -e image.png",
            expectedReason = "Cannot set a custom --bitmask when using a --passphrase.",
        )
        testCommandBadUsage(
            name = "Bitmasks cannot be used with reused keys.",
            args = "hide -m 'Hi' -b 1 --key -e image.png",
            expectedReason = "Cannot change the --bitmask when reusing a --key.",
        )
        testCommandBadUsage(
            name = "Cannot use both keys and passphrases.",
            args = "hide -m 'Hi' --key --passphrase -e image.png",
            expectedReason = "Options --passphrase and --key are incompatible.",
        )
        testCommandBadUsage(
            name = "Cannot use messages with raw payloads.",
            args = "hide -m 'Hi' --raw -e image.png",
            expectedReason = "Options --message and --raw are incompatible.",
        )
        testCommandBadUsage(
            name = "A payload must be defined.",
            args = "hide -e image.png",
            expectedReason = "No payload given. Please define a --message and / or pass files as arguments.",
        )
        testCommandBadUsage(
            name = "Cannot use multiple attachments with raw payloads.",
            args = "hide --raw -e image.png image.rgba image.rgba",
            expectedReason = "When --raw mode is used, exactly one attachment argument is required.",
        )
    }
})
