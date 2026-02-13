package com.mrpowergamerbr.butterscotch.console

class HelpCommand(console: DebugConsole) : DebugConsoleCommand(console, "help", "Show available commands") {
    override fun execute(args: List<String>) {
        console.addOutput("Available commands:")
        for (cmd in console.commands) {
            console.addOutput("  ${cmd.name} - ${cmd.description}")
        }
    }
}

class ClearCommand(console: DebugConsole) : DebugConsoleCommand(console, "clear", "Clear console output") {
    override fun execute(args: List<String>) {
        console.clearOutput()
    }
}

class RoomGotoCommand(console: DebugConsole) : DebugConsoleCommand(console, "room_goto", "Go to a room") {
    override fun execute(args: List<String>) {
        val arg = args.joinToString(" ")
        if (arg.isEmpty()) {
            console.addOutput("Usage: room_goto <room_name_or_index>")
            return
        }

        val runner = console.runner

        // Try as index first
        val index = arg.toIntOrNull()
        if (index != null) {
            if (index in runner.gameData.rooms.indices) {
                val roomName = runner.gameData.rooms[index].name
                console.addOutput("Going to room $index ($roomName)")
                runner.gotoRoom(index)
            } else {
                console.addOutput("Error: Room index $index out of range (0..${runner.gameData.rooms.size - 1})")
            }
            return
        }

        // Try as name
        val roomIdx = runner.gameData.rooms.indexOfFirst { it.name == arg }
        if (roomIdx >= 0) {
            console.addOutput("Going to room $roomIdx ($arg)")
            runner.gotoRoom(roomIdx)
        } else {
            // Fuzzy search - find rooms containing the search term
            val matches = runner.gameData.rooms.withIndex()
                .filter { (_, r) -> r.name.contains(arg, ignoreCase = true) }
                .take(5)
            if (matches.isNotEmpty()) {
                console.addOutput("Error: Room '$arg' not found. Did you mean:")
                for ((i, r) in matches) {
                    console.addOutput("  $i: ${r.name}")
                }
            } else {
                console.addOutput("Error: Room '$arg' not found")
            }
        }
    }
}

class RoomInfoCommand(console: DebugConsole) : DebugConsoleCommand(console, "room_info", "Show current room info") {
    override fun execute(args: List<String>) {
        val runner = console.runner
        val room = runner.currentRoom
        if (room != null) {
            console.addOutput("Room: ${room.name} (index ${runner.currentRoomIndex})")
            console.addOutput("  Size: ${room.width}x${room.height}")
            console.addOutput("  Instances: ${runner.instances.size}")
            console.addOutput("  Frame: ${runner.frameCount}")
        } else {
            console.addOutput("No room loaded")
        }
    }
}

class InstancesCommand(console: DebugConsole) : DebugConsoleCommand(console, "instances", "List active instances") {
    override fun execute(args: List<String>) {
        val runner = console.runner
        val counts = runner.instances
            .filter { !it.destroyed }
            .groupBy { it.objectIndex }
            .map { (objIdx, insts) ->
                val name = if (objIdx in runner.gameData.objects.indices) runner.gameData.objects[objIdx].name else "??($objIdx)"
                "$name: ${insts.size}"
            }
            .sorted()
        console.addOutput("Active instances (${runner.instances.count { !it.destroyed }}):")
        for (line in counts) {
            console.addOutput("  $line")
        }
    }
}

class FluffyBoiCommand(console: DebugConsole) : DebugConsoleCommand(console, "fluffyboi", "Who's a cute fluffy boi?") {
    override fun execute(args: List<String>) {
        console.addOutput("Asriel is a cute fluffy boi :3")
    }
}
