# GameMaker: Studio Data File Format (game.unx / data.win)

Reference: [UndertaleModTool](https://github.com/UnderminersTeam/UndertaleModTool)

## Overview

The data file uses an IFF-like container format. The outer container is a `FORM` chunk containing all sub-chunks sequentially. All integers are little-endian.

```
FORM <total_size:u32>
  <chunk1_name:4bytes> <chunk1_size:u32> <chunk1_data...>
  <chunk2_name:4bytes> <chunk2_size:u32> <chunk2_data...>
  ...
```

## Undertale v1.08 game.unx Statistics

- **Total size**: ~60 MB
- **Bytecode version**: 16
- **Compiled with**: GameMaker Studio 1.4.1539

## Chunk Types (in order as they appear)

| Chunk | Offset     | Size       | Description |
|-------|------------|------------|-------------|
| GEN8  | 0x00000008 | 1,476 B    | General info / game metadata |
| OPTN  | 0x000005D4 | 80 B       | Game options |
| LANG  | 0x0000062C | 12 B       | Language settings |
| EXTN  | 0x00000640 | 4 B        | Extensions (empty) |
| SOND  | 0x0000064C | 17,724 B   | Sound definitions |
| AGRP  | 0x00004B90 | 4 B        | Audio groups (empty) |
| SPRT  | 0x00004B9C | 1,976,504 B| Sprite definitions (2583 sprites) |
| BGND  | 0x001E745C | 5,980 B    | Background definitions (249 backgrounds) |
| PATH  | 0x001E8BC0 | 3,604 B    | Path definitions |
| SCPT  | 0x001E99DC | 2,860 B    | Script definitions (238 scripts) |
| GLOB  | 0x001EA510 | 4 B        | Global init scripts |
| SHDR  | 0x001EA51C | 4 B        | Shaders (empty) |
| FONT  | 0x001EA528 | 149,296 B  | Font definitions (20 fonts) |
| TMLN  | 0x0020EC60 | 4 B        | Timelines (empty) |
| OBJT  | 0x0020EC6C | 748,612 B  | Object definitions (1709 objects) |
| ROOM  | 0x002C58B8 | 4,506,968 B| Room definitions (336 rooms) |
| DAFL  | 0x00711E18 | 0 B        | Data files (empty) |
| TPAG  | 0x00711E20 | 170,304 B  | Texture page items (6550 items) |
| CODE  | 0x0073B768 | 5,264,628 B| Bytecode (6272 code entries) |
| VARI  | 0x00C40C64 | 218,152 B  | Variable definitions (10907 vars) |
| FUNC  | 0x00C76094 | 110,636 B  | Function references (435 functions) |
| STRG  | 0x00C910C8 | 2,858,288 B| String table (64017 strings) |
| TXTR  | 0x00F4AE00 | 12,725,340 B| Texture pages (26 pages, PNG format) |
| AUDO  | 0x01B6DA64 | 34,199,674 B| Embedded audio data |

## Common Pattern: Pointer Lists

Most chunks follow this pattern:
```
<count:u32>
<pointer_0:u32> <pointer_1:u32> ... <pointer_N:u32>
// then at each pointer address, the actual entry data
```

## GEN8 Chunk

Game metadata and settings.

```
+0x00: debug (u8), bytecodeVersion (u8), unknown (u16)
+0x04: filename string offset (u32)    -> points into STRG
+0x08: config string offset (u32)
+0x0C: lastObjId (u32)                 -> 113926
+0x10: lastTileId (u32)                -> 10068815
+0x14: gameId (u32)                    -> 864738521
+0x18: GUID (16 bytes)                 -> all zeros
+0x28: game name string offset (u32)
+0x2C: major (u32)                     -> 1
+0x30: minor (u32)                     -> 0
+0x34: release (u32)                   -> 0
+0x38: build (u32)                     -> 1539
+0x3C: defaultWindowWidth (u32)        -> 640
+0x40: defaultWindowHeight (u32)       -> 480
+0x44: infoFlags (u32)                 -> 0x9B6
+0x48: licenseCRC32 (u32)
+0x4C: licenseMD5 (16 bytes)
+0x5C: timestamp (u64)
+0x64: displayName string offset (u32)
+0x68: activeTargets (u64)
+0x70: functionClassifications (u64)
+0x78: steamAppId (u32)
+0x7C: debuggerPort (u32)              -> 6502
+0x80: roomOrderCount (u32)            -> 336
+0x84: roomOrder[0..N] (u32 each)     -> 0,1,2,...335
```

**Info flags (0x9B6)**:
- Fullscreen: false
- SyncVertex1: true
- SyncVertex2: true
- Interpolate: false
- Scale: true
- ShowCursor: true
- Sizeable: false
- ScreenKey: true

## STRG Chunk (String Table)

```
count (u32): 64017
pointers[0..count] (u32 each)
// Each string entry at pointer:
  length (u32)
  data (length bytes, UTF-8)
  null terminator (1 byte)
```

String offsets in other chunks point to the `length` field (offset - 4 from the start of the actual string data).

## SPRT Chunk (Sprites)

```
count (u32): 2583
pointers[0..count] (u32 each)
```

Each sprite entry:
```
+0x00: name offset (u32)      -> string pointer
+0x04: width (u32)
+0x08: height (u32)
+0x0C: marginLeft (i32)
+0x10: marginRight (i32)
+0x14: marginBottom (i32)
+0x18: marginTop (i32)
+0x1C: transparent (u32)
+0x20: smooth (u32)
+0x24: preload (u32)
+0x28: bboxMode (u32)
+0x2C: sepMasks (u32)
+0x30: originX (i32)
+0x34: originY (i32)
+0x38: subImageCount (u32)
// Then subImageCount TPAG entry pointers (u32 each)
// Then mask data follows (if applicable)
```

## BGND Chunk (Backgrounds)

Each entry:
```
+0x00: name offset (u32)
+0x04: transparent (u32)
+0x08: smooth (u32)
+0x0C: preload (u32)
+0x10: tpag pointer (u32) -> points to a TPAG entry
```

## TPAG Chunk (Texture Page Items)

Maps sprite frames / backgrounds to regions within texture pages.

```
count (u32): 6550
pointers[0..count] (u32 each)
```

Each TPAG entry (22 bytes):
```
+0x00: sourceX (u16)        - X in texture page
+0x02: sourceY (u16)        - Y in texture page
+0x04: sourceWidth (u16)    - Width in texture page
+0x06: sourceHeight (u16)   - Height in texture page
+0x08: targetX (u16)        - X offset in output (for trimmed sprites)
+0x0A: targetY (u16)        - Y offset in output
+0x0C: targetWidth (u16)    - Output width
+0x0E: targetHeight (u16)   - Output height
+0x10: boundingWidth (u16)  - Full original width
+0x12: boundingHeight (u16) - Full original height
+0x14: texturePageId (u16)  - Index into TXTR
```

## TXTR Chunk (Texture Pages)

```
count (u32): 26
pointers[0..count] (u32 each)
```

Each texture page entry:
```
+0x00: scaled (u32)
+0x04: dataOffset (u32) -> absolute file offset to PNG data
```

The PNG data is standard PNG format. Texture page sizes range from 512x512 to 2048x2048.

## OBJT Chunk (Game Objects)

```
count (u32): 1709
pointers[0..count] (u32 each)
```

Each object entry:
```
+0x00: name offset (u32)
+0x04: spriteIndex (i32)      - -1 if no sprite
+0x08: visible (u32)
+0x0C: solid (u32)
+0x10: depth (i32)
+0x14: persistent (u32)
+0x18: parentId (i32)          - -100 if no parent
+0x1C: maskId (i32)            - -1 if same as sprite
+0x20: physics fields...       - (20 bytes of physics data)
// Then 12 event type lists (see Event System below)
```

### Event Types (12 categories):
| Index | Name       | Description |
|-------|------------|-------------|
| 0     | Create     | On instance creation |
| 1     | Destroy    | On instance destruction |
| 2     | Alarm      | Alarm timers (subtypes 0-11) |
| 3     | Step       | Step events (0=normal, 1=begin, 2=end) |
| 4     | Collision  | Collision with another object (subtype = other object ID) |
| 5     | Keyboard   | Keyboard events (subtype = key code) |
| 6     | Mouse      | Mouse events |
| 7     | Other      | Other events (room start/end, animation end, etc.) |
| 8     | Draw       | Draw events (0=normal, 64-77=GUI/resize/etc.) |
| 9     | KeyPress   | Key press events |
| 10    | KeyRelease | Key release events |
| 11    | Trigger    | Trigger events |

Each event type list:
```
count (u32)
entries[0..count] (u32 pointers)
// Each entry: subtype (u32), action list...
```

## ROOM Chunk

```
count (u32): 336
pointers[0..count] (u32 each)
```

Each room entry:
```
+0x00: name offset (u32)
+0x04: caption offset (u32)
+0x08: width (u32)
+0x0C: height (u32)
+0x10: speed (u32)              - frames per second (30 for all Undertale rooms)
+0x14: persistent (u32)
+0x18: bgColor (u32)            - ARGB format
+0x1C: drawBgColor (u32)
+0x20: creationCodeId (i32)     - index into CODE, -1 if none
+0x24: flags (u32)
+0x28: backgroundsPtr (u32)     -> background layer list
+0x2C: viewsPtr (u32)           -> view list
+0x30: objectsPtr (u32)         -> instance list
+0x34: tilesPtr (u32)           -> tile list
```

### Room Background Entry
```
enabled (u32), foreground (u32), bgDefIndex (i32),
x (i32), y (i32), tileX (i32), tileY (i32),
speedX (i32), speedY (i32), stretch (u32)
```

### Room View Entry
```
enabled (u32),
viewX (i32), viewY (i32), viewW (i32), viewH (i32),
portX (i32), portY (i32), portW (i32), portH (i32),
borderH (i32), borderV (i32),
speedH (i32), speedV (i32),
followObjectId (i32)
```

### Room Object Instance Entry
```
x (i32), y (i32),
objectDefId (i32),     - index into OBJT
instanceId (u32),      - unique instance ID (starts at 100000)
creationCode (i32),    - index into CODE, -1 if none
scaleX (f32), scaleY (f32),
color (u32),
rotation (f32)
```

## CODE Chunk

```
count (u32): 6272
pointers[0..count] (u32 each)
```

Each code entry (20 bytes header):
```
+0x00: name offset (u32)        -> e.g. "gml_Object_obj_time_Create_0"
+0x04: length (u32)             -> bytecode length in bytes
+0x06: localsCount (u16)
+0x08: argumentsCount (u16)
+0x0A: bytecodeRelativeOffset (i32) -> relative offset to bytecode start
+0x0E: offset (u32)
```

Naming convention: `gml_Object_<objname>_<EventType>_<subtype>` or `gml_Script_<scriptname>`

## VARI Chunk (Variables)

```
+0x00: instanceVarCount (u32)    -> 3909
+0x04: instanceVarMaxId (u32)    -> 3909
+0x08: unknown (u32)             -> 40
// Then variable entries (20 bytes each):
  nameOffset (u32)
  instanceType (i32)   -> -1=self, -5=global, -7=local, -6=builtin
  varId (i32)
  occurrenceCount (u32)
  firstOccurrenceOffset (i32)
```

Built-in variable IDs use varId = -6. User variables get sequential positive IDs.

## FUNC Chunk (Function References)

```
count (u32): 435
// Each entry:
  nameOffset (u32)
  occurrenceCount (u32)
  occurrenceOffsets[0..count] (u32 each) -> bytecode patch locations
```

## SCPT Chunk (Scripts)

Maps user script names to CODE entry indices.

```
count (u32): 238
pointers[0..count] (u32 each)
// Each entry:
  nameOffset (u32)
  codeId (u32)    -> index into CODE chunk
```

## FONT Chunk

```
count (u32): 20
pointers[0..count] (u32 each)
```

Each font entry:
```
nameOffset (u32), displayNameOffset (u32),
emSize (u32), bold (u32), italic (u32),
rangeStart (u16), charset (u8), aaLevel (u8),
rangeEnd (u32),
tpagOffset (u32),     -> texture page item for the font atlas
scaleX (f32), scaleY (f32),
// Then glyph count (u32) followed by glyph entries
```

## SOND Chunk (Sounds)

Each sound entry references either embedded audio (AUDO chunk) or external files (like .ogg files next to the data file).

## AUDO Chunk (Embedded Audio)

Contains raw audio data for sounds that are embedded in the data file.
