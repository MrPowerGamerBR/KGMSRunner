# KGMSRuntime Architecture Plan

## Technology Stack

- **Language**: Kotlin (JVM 21)
- **Windowing**: LWJGL 3 + GLFW
- **Rendering**: OpenGL (via LWJGL)
- **Image decoding**: STB (via LWJGL)
- **Math**: JOML

## Module Structure

```
com.mrpowergamerbr.kgmsruntime/
  KGMSRuntime.kt          - Entry point, game loop, GLFW window

  data/                    - Data file parsing
    GameData.kt            - Top-level data container
    FormReader.kt          - IFF FORM chunk reader
    ChunkReader.kt         - Individual chunk parsers
    StringTable.kt         - STRG chunk / string lookup

  assets/                  - Parsed asset types
    Sprite.kt              - Sprite definition
    Background.kt          - Background definition
    TexturePageItem.kt     - TPAG entry
    TexturePage.kt         - TXTR page (manages OpenGL texture)
    Font.kt                - Font definition + glyph data
    Sound.kt               - Sound definition (stub)
    Room.kt                - Room definition + instances/views/bgs/tiles
    GameObject.kt          - Object definition + event lists
    Script.kt              - Script definition
    Path.kt                - Path definition

  vm/                      - Virtual machine
    VM.kt                  - Bytecode interpreter loop
    Instruction.kt         - Instruction decoder
    VMStack.kt             - Value stack
    GMLValue.kt            - Runtime value type (double/string/array/undefined)
    CodeEntry.kt           - Code entry (bytecode + metadata)
    VariableScope.kt       - Variable storage (instance/global/local)

  runtime/                 - Game runtime
    GameRunner.kt          - High-level game loop / event dispatch
    Instance.kt            - Live game instance
    InstanceManager.kt     - Instance creation/destruction/lookup
    RoomManager.kt         - Room loading/transitions
    EventDispatcher.kt     - Event type routing
    AlarmManager.kt        - Alarm countdown system

  graphics/                - Rendering
    Renderer.kt            - OpenGL renderer
    SpriteBatch.kt         - Batched sprite rendering
    TextRenderer.kt        - Font/text rendering
    DrawState.kt           - Current draw color/alpha/font/halign/valign
    ViewManager.kt         - View/port transformation

  builtin/                 - Built-in GML functions
    BuiltinFunctions.kt    - Registry of all built-in functions
    DrawFunctions.kt       - draw_* functions
    MathFunctions.kt       - Math/random functions
    StringFunctions.kt     - String manipulation functions
    InstanceFunctions.kt   - instance_create/destroy/exists etc.
    RoomFunctions.kt       - room_goto etc.
    InputFunctions.kt      - keyboard_check etc.
    DataStructFunctions.kt - ds_map/ds_list/ds_grid
    FileFunctions.kt       - file_exists, ini_* functions
    AudioStubs.kt          - Stub implementations for audio
    MiscFunctions.kt       - show_message, game_end, etc.
```

## Data Loading Pipeline

```
game.unx -> FormReader (parse FORM header)
  -> iterate chunks
  -> for each chunk, delegate to ChunkReader
  -> build GameData with all parsed assets
  -> resolve cross-references (sprites -> TPAG -> TXTR, objects -> events -> CODE, etc.)
```

## VM Design

### GMLValue (tagged union)
```kotlin
sealed class GMLValue {
    data class Real(val value: Double) : GMLValue()
    data class Str(val value: String) : GMLValue()
    data class Array(val data: MutableMap<Int, MutableMap<Int, GMLValue>>) : GMLValue()
    object Undefined : GMLValue()
}
```

### Variable Storage
- **Instance variables**: HashMap per instance, keyed by variable ID
- **Global variables**: Single shared HashMap
- **Local variables**: Stack-allocated per code entry execution
- **Built-in variables**: Intercepted reads/writes mapped to instance fields (x, y, sprite_index, etc.)

### Execution Flow
```
1. Decode instruction (4 bytes)
2. Match opcode
3. Execute (push/pop/arithmetic/branch/call)
4. Advance instruction pointer
5. Repeat until Return/Exit
```

## Rendering Pipeline

```
1. Clear with room bg color
2. For each enabled view:
   a. Set up orthographic projection for view rect
   b. Set viewport to port rect
   c. Draw backgrounds (non-foreground, tiled if needed)
   d. Gather all visible instances
   e. Sort by depth (descending = highest depth drawn first)
   f. For each instance, call Draw event (or default draw)
   g. Draw foreground backgrounds
3. Swap buffers
```

### Default Draw
If an instance has no Draw event:
```kotlin
if (instance.visible && instance.spriteIndex >= 0) {
    drawSprite(instance.spriteIndex, instance.imageIndex, instance.x, instance.y)
}
```

### Sprite Drawing
```
1. Look up sprite definition -> get TPAG for current frame
2. Look up TXTR page -> get/create OpenGL texture
3. Compute UV coordinates from TPAG source rect
4. Apply target offset, scale, rotation, blend color, alpha
5. Draw textured quad
```

## Implementation Phases

### Phase 1: Data Loading
- Parse all chunks from game.unx
- Load textures into OpenGL
- Resolve all cross-references

### Phase 2: Basic VM
- Instruction decoder for all BC16 opcodes
- Stack operations (push/pop/arithmetic/comparison)
- Variable read/write (instance, global, local, builtin)
- Function calls (user scripts + built-in stubs)
- Branch instructions

### Phase 3: Instance & Room System
- Instance creation/destruction
- Room loading with instances
- Event dispatch (Create, Step, Draw, Alarm, Keyboard, Other)
- Room transitions

### Phase 4: Rendering
- Sprite rendering with TPAG/TXTR lookup
- Text rendering with font glyphs
- Draw state (color, alpha, font, alignment)
- View/port transformation
- Rectangle/primitive drawing

### Phase 5: Built-in Functions (for intro)
- Drawing functions
- Input functions (keyboard)
- Math/string functions
- Instance functions
- Room functions
- File/ini stubs
- Audio stubs (no-op)

### Phase 6: Integration & Debug
- Run room_start -> room_introstory -> room_introimage -> room_intromenu
- Debug VM execution
- Fix missing built-in functions as encountered
- Iterate until menu is functional
