package io.github.jadarma.steko.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.github.jadarma.steko.cli.commands.HideCommand
import io.github.jadarma.steko.cli.commands.StekoCommand
import io.github.jadarma.steko.cli.commands.ShowCommand

fun main(args: Array<String>): Unit =
    StekoCommand()
        .subcommands(HideCommand(), ShowCommand())
        .main(args)
