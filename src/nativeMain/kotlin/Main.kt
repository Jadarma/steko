package io.github.jadarma.steggo

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.github.jadarma.steggo.cli.hide.HideCommand
import io.github.jadarma.steggo.cli.StegoCommand
import io.github.jadarma.steggo.cli.show.ShowCommand

fun main(args: Array<String>): Unit =
    StegoCommand()
        .subcommands(HideCommand(), ShowCommand())
        .main(args)
