package io.github.jadarma.stego.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.github.jadarma.stego.cli.commands.HideCommand
import io.github.jadarma.stego.cli.commands.StegoCommand
import io.github.jadarma.stego.cli.commands.ShowCommand

fun main(args: Array<String>): Unit =
    StegoCommand()
        .subcommands(HideCommand(), ShowCommand())
        .main(args)
