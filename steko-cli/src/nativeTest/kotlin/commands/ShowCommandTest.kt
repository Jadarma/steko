package io.github.jadarma.steko.cli.commands

import io.github.jadarma.steko.cli.test.testCommandBadUsage
import io.kotest.core.spec.style.FunSpec

class ShowCommandTest : FunSpec({

    context("Validation") {
        testCommandBadUsage(
            name = "Output directory must exist.",
            args = "show --out /nope/404 image.png",
            expectedReason = "invalid value for --out: Directory does not exist: /nope/404",
        )
        testCommandBadUsage(
            name = "Output directory cannot be a file.",
            args = "show --out image.rgba image.png",
            expectedReason = "invalid value for --out: Expected directory path is a file: image.rgba",
        )
        testCommandBadUsage(
            name = "Input image must exist.",
            args = "show missing.png",
            expectedReason = "invalid value for <image>: File does not exist: missing.png",
        )
        testCommandBadUsage(
            name = "Input image must be a file.",
            args = "show /images",
            expectedReason = "invalid value for <image>: Expected file path is not a file: /images",
        )
        testCommandBadUsage(
            name = "Only one image can be decoded at a time.",
            args = "show image.png image.rgba",
            expectedReason = "got unexpected extra argument (image.rgba)",
        )
        testCommandBadUsage(
            name = "A passphrase must be provided to stdin",
            args = "show --passphrase image.png",
            expectedReason = "Could not read passphrase from STDIN.",
        )
        testCommandBadUsage(
            name = "A key must be provided to stdin",
            args = "show image.png",
            expectedReason = "Could not read key from STDIN.",
        )
    }
})
