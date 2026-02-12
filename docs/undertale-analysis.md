# Undertale v1.08 Analysis for KGMSRuntime

## Target: Intro Sequence -> Title Screen -> Main Menu

### Room Flow

```
room_start (0) -> room_introstory (1) -> room_introimage (2) -> room_intromenu (3)
```

### Room Details

| Room | Size | Views | Key Objects |
|------|------|-------|-------------|
| room_start | 640x480 | None | obj_time (persistent), obj_screen (persistent) |
| room_introstory | 320x240 | 320x240 -> 640x480 | obj_introimage, 2x obj_unfader |
| room_introimage | 320x240 | 320x240 -> 640x480 | obj_titleimage |
| room_intromenu | 320x240 | 320x240 -> 640x480 | obj_intromenu |

### Key Objects

#### obj_time (ID 1575) - Persistent Global Controller
- **Sprite**: none
- **Persistent**: YES (survives room changes)
- **Events**: Create, Step_2, Step_1, Other_72, Other_70, Other_4, Other_2, Draw_77/76/75/64, KeyPress_114/75
- **Purpose**: Global game timer, game state management, likely initializes global variables

#### obj_screen (ID 139) - Persistent Screen Controller
- **Sprite**: none
- **Persistent**: YES
- **Purpose**: Screen/display management, likely handles resolution scaling and borders

#### obj_introimage (ID 100) - Intro Story Sequence
- **Sprite**: spr_introimage (320x240, 11 frames)
- **Events**: Create, Alarm_0/1/2, Step_0/1
- **Purpose**: Displays the scrolling intro text and story images
- **Sprites used**: spr_introimage (11 animation frames showing the intro story)

#### obj_titleimage (ID 97) - Title Screen
- **Sprite**: spr_undertaletitle (320x240, 1 frame)
- **Purpose**: Shows the Undertale logo/title screen

#### obj_intromenu (ID 95) - Main Menu
- **Sprite**: none (draws programmatically)
- **Events**: Create, Step_0, Draw_0, KeyPress_27 (Escape)
- **Purpose**: Name selection / continue/reset menu
- **Code size**: Create=1840B, Draw=1680B (significant drawing logic)

#### obj_unfader (ID 150) - Fade Transition
- **Sprite**: spr_unfader (ID 1084)
- **Depth**: -99999 (always on top)
- **Purpose**: Handles screen fade-in/fade-out transitions between rooms

### Key Sprites for Intro

| Index | Name | Size | Frames | Description |
|-------|------|------|--------|-------------|
| 0 | spr_undertaletitle | 320x240 | 1 | Title screen image |
| 1 | spr_introlast | 320x350 | 1 | Last intro image |
| 2 | spr_introimage | 320x240 | 11 | Intro story animation frames |
| 1084 | spr_unfader | ? | ? | Fade effect sprite |

### Key Scripts

| Script | Code ID | Description |
|--------|---------|-------------|
| SCR_TEXT | 1 | Massive text system (145,980 bytes!) |
| scr_gettext | (in strings) | Text localization |
| SCR_GAMESTART | 66 | Game initialization |
| scr_namingscreen | 55 | Character naming screen |
| scr_namingscreen_check | 56 | Name validation |
| scr_namingscreen_setup | 57 | Name screen initialization |

### Key Code Entries for Intro Flow

```
gml_Object_obj_time_Create_0         (1664 bytes) - Global initialization
gml_Object_obj_time_Step_1           (6428 bytes) - Main game logic per step
gml_Object_obj_introimage_Create_0   (96 bytes)   - Setup intro sequence
gml_Object_obj_introimage_Alarm_2    (812 bytes)   - Intro timing/transitions
gml_Object_obj_introimage_Step_0     (192 bytes)   - Intro progression
gml_Object_obj_intromenu_Create_0    (1840 bytes)  - Menu setup
gml_Object_obj_intromenu_Draw_0      (1680 bytes)  - Menu rendering
gml_Object_obj_intromenu_Step_0      (68 bytes)    - Menu input handling
```

### Rendering Approach

Undertale uses a **320x240 native resolution** scaled to **640x480** via views. The view system maps:
- View: (0, 0, 320, 240) - the game world coordinates
- Port: (0, 0, 640, 480) - the window coordinates

This gives a 2x integer scale. The renderer needs to:
1. Render to a 320x240 framebuffer (or equivalent)
2. Scale up 2x to the window

### Font Usage

Key fonts for the intro/menu:
- `fnt_main` (em=24) - Main UI font
- `fnt_maintext` (em=12) - Text boxes
- `fnt_small` (em=6) - Small text
- `fnt_plain` (em=12) - Plain text

### Sound/Music (for reference, not implementing audio)

- `mus_story.ogg` - Intro story music
- `mus_menu0.ogg` through `mus_menu6.ogg` - Menu music variants
- `mus_intronoise.ogg` - Intro sound effects

### Global Variables Used in Intro

From the VARI chunk, important globals include:
- `msg` (global) - Current message text (15606 occurrences!)
- `flag` (global) - Game flags array (3612 occurrences)
- `room` (builtin) - Current room
- `choice` (global) - Menu choice
- `i` (self) - Loop counter

### Save File System

Undertale uses `ini_*` functions for save data:
- Checks for existing save file to determine Continue vs New Game
- Save data stored in ini format
- File operations: `file_exists`, `ini_open`, `ini_read_real`, `ini_close`

### Implementation Priority

To reach the main menu, we need:

1. **Data loading**: Parse FORM/GEN8/STRG/SPRT/TPAG/TXTR/OBJT/ROOM/CODE/VARI/FUNC/BGND/FONT
2. **VM**: Execute bytecode (push/pop/call/branch/compare/arithmetic)
3. **Instance system**: Create/destroy instances, variable storage, event dispatch
4. **Room system**: Load rooms, create instances, room transitions
5. **Drawing**: Sprites, text, rectangles, color/alpha/font state
6. **Input**: Keyboard state (keyboard_check, keyboard_check_pressed)
7. **View system**: 320x240 to 640x480 scaling
8. **Built-in functions**: ~50 most-used functions
9. **Alarm system**: alarm[0..11] countdown
10. **Stub audio**: Return safe defaults for all caster_*/snd_* functions
