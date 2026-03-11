package io.github.jadarma.steko.cli.test

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.CliktCommandTestResult
import com.github.ajalt.clikt.testing.test
import io.github.jadarma.steko.cli.commands.HideCommand
import io.github.jadarma.steko.cli.commands.ShowCommand
import io.github.jadarma.steko.cli.commands.StekoCommand
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain

/**
 * Registers a new test for running the command in a test environment.
 *
 * @param name The name of the test to register.
 * @param args The arguments passed to the command.
 * @param stdIn Data available on STDIN, if any.
 * @param interactiveInput Whether the input is interactive. Should be false when stdIn has data.
 * @param interactiveOutput Whether the output is interactive. Should be false when redirecting, piping, or scripting.
 * @param thunk Gives back the environment after the command was evaluated so assertions can be made.
 */
context(scope: FunSpecContainerScope)
suspend fun testCommand(
    name: String,
    args: String,
    stdIn: String = "",
    interactiveInput: Boolean = false,
    interactiveOutput: Boolean = false,
    thunk: (CliktCommandTestResult, TestFileSystem) -> Unit = { _, _ -> Unit },
) = scope.test(name) {
    val testFileSystem = TestFileSystem()
    val command = StekoCommand(testFileSystem).subcommands(HideCommand(), ShowCommand())
    val result = command.test(
        argv = args,
        stdin = stdIn,
        inputInteractive = interactiveInput,
        outputInteractive = interactiveOutput,
    )
    thunk(result, testFileSystem)
}

/**
 * Like [testCommand], but asserts that the [args] should result in a bad usage error with the [expectedReason].
 * The filesystem should not be modified, the output should be on STDERR, and the return code non-zero.
 */
context(scope: FunSpecContainerScope)
suspend fun testCommandBadUsage(
    name: String,
    args: String,
    expectedReason: String,
    stdIn: String = "",
    interactiveInput: Boolean = false,
    interactiveOutput: Boolean = false,
) = testCommand(name, args, stdIn, interactiveInput, interactiveOutput) { result, fileSystem ->
    withClue("Bad command did not return expected status code") {
        result.statusCode shouldBe 1
    }
    withClue("Bad command did not report expected warning") {
        result.stderr shouldContain expectedReason
    }
    withClue("Bad command usage should only print to stderr") {
        result.stdout.shouldBeEmpty()
    }
    withClue("Bad command usages should not modify filesystem.") {
        fileSystem.shouldBeUnmodified()
    }
}
