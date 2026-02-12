# KGMSRuntime

A GameMaker: Studio 1.x runtime (Bytecode 16) in Kotlin, targeting Undertale v1.08.

## Project Goal

Run Undertale's intro sequence, title screen, and main menu using LWJGL/OpenGL.

## Technology

- Kotlin/JVM 21, Gradle build
- LWJGL 3.4.1 (GLFW, OpenGL, STB)
- JOML for math
- No audio support needed

## Key Facts

- **Data file**: `undertale/game.unx` (60 MB IFF/FORM container)
- **Bytecode version**: 16 (confirmed in GEN8 chunk)
- **Compiled with**: GameMaker Studio 1.4.1539
- **Window size**: 640x480 (320x240 game scaled 2x via views)
- **Frame rate**: 30 FPS (room_speed = 30)
- **First room**: room_start (index 0) containing obj_time + obj_screen

## Research Documentation

- [Data File Format](docs/data-format.md) - IFF chunk format for game.unx (GEN8, STRG, SPRT, TPAG, TXTR, OBJT, ROOM, CODE, VARI, FUNC, etc.)
- [Bytecode 16](docs/bytecode.md) - Instruction set, encoding, opcodes, stack-based VM
- [Runtime Architecture](docs/runtime.md) - Game loop, instance system, events, drawing, variables
- [Undertale Analysis](docs/undertale-analysis.md) - Rooms, objects, sprites specific to the intro/menu sequence
- [KGMSRuntime Architecture](docs/architecture.md) - Our implementation plan, module structure, phases

## Key References

- [UndertaleModTool](https://github.com/UnderminersTeam/UndertaleModTool) - Definitive data format reference (C#)
- [OpenGMK](https://github.com/OpenGMK/OpenGMK) - GM8 runner in Rust (architecture inspiration)
- [OpenGML](https://github.com/maiple/opengml) - GML 1.4 interpreter in C++
- [Altar.NET](https://github.com/PoroCYon/Altar.NET) - GM:S data.win unpacker
- [GM:S 1.4 Manual](https://docs2.yoyogames.com/) - Official GML documentation

## Undertale Intro Room Flow

```
room_start (640x480) -> room_introstory (320x240, 2x view) -> room_introimage (320x240) -> room_intromenu (320x240)
```

Key objects: `obj_time` (persistent controller), `obj_screen` (persistent), `obj_introimage`, `obj_titleimage`, `obj_intromenu`, `obj_unfader`

## Decompiled GML Code

- **Folder**: `undertale_gml_code/` - All 6272 code entries decompiled to GML via UndertaleModTool
- **Naming**: `gml_Object_<name>_<event>.gml`, `gml_Script_<name>.gml`, `gml_GlobalScript_<name>.gml`, `gml_RoomCC_<name>.gml`
- **CLI Tool**: `UTMT_CLI_v0.8.4.1-Ubuntu/UndertaleModCli` - UndertaleModTool CLI (`./UndertaleModCli load ../undertale/game.unx -s Scripts/Resource\ Exporters/ExportAllCode.csx`)

## Development Commands

```bash
./gradlew run          # Run the application
./gradlew build        # Build
./gradlew test         # Run tests
```
