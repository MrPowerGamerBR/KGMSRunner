package com.mrpowergamerbr.butterscotch.console

abstract class DebugConsoleCommand(val console: DebugConsole, val name: String, val description: String) {
    abstract fun execute(args: List<String>)
}