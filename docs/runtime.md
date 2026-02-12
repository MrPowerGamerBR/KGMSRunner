# GameMaker: Studio 1.x Runtime Architecture

## Game Loop

GameMaker uses a fixed-timestep game loop. Each step (frame) runs at `room_speed` FPS (30 for Undertale). The loop order per frame:

1. **Begin Step** event for all instances (sorted by creation order)
2. **Alarm** events for all instances (alarm[0] through alarm[11], decremented each step)
3. **Keyboard/KeyPress/KeyRelease** events
4. **Mouse** events
5. **Step** event for all instances
6. **Collision** events (check all collision pairs)
7. **End Step** event for all instances
8. **Drawing**:
   a. Clear screen with room background color
   b. Draw background layers (non-foreground)
   c. For each view (or full room if no views):
      - Draw all instances sorted by **depth** (highest first = furthest back)
      - Each instance's **Draw** event is called
      - If no Draw event, default draws `sprite_index` at `(x, y)`
   d. Draw foreground layers
   e. Draw GUI events (Draw GUI, etc.)
9. **Swap buffers** / present

## Instance System

- Each object definition (OBJT) is a template
- Instances are created from objects and placed in rooms or created at runtime
- Each instance has a unique ID (starting from 100000 in Undertale)
- Instance variables: `x`, `y`, `sprite_index`, `image_index`, `image_speed`, `image_xscale`, `image_yscale`, `image_angle`, `image_blend`, `image_alpha`, `depth`, `visible`, `solid`, `persistent`, `alarm[0..11]`, etc.

### Built-in Instance Variables

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| x | real | from room | X position |
| y | real | from room | Y position |
| xprevious | real | x | Previous X |
| yprevious | real | y | Previous Y |
| xstart | real | x | Starting X |
| ystart | real | y | Starting Y |
| hspeed | real | 0 | Horizontal speed |
| vspeed | real | 0 | Vertical speed |
| speed | real | 0 | Movement speed |
| direction | real | 0 | Movement direction (degrees) |
| friction | real | 0 | Friction |
| gravity | real | 0 | Gravity strength |
| gravity_direction | real | 270 | Gravity direction |
| sprite_index | int | from OBJT | Current sprite |
| image_index | real | 0 | Current animation frame |
| image_speed | real | 1 | Animation speed (frames per step) |
| image_xscale | real | 1 | Horizontal scale |
| image_yscale | real | 1 | Vertical scale |
| image_angle | real | 0 | Rotation (degrees) |
| image_blend | int | 0xFFFFFF | Color blend |
| image_alpha | real | 1 | Alpha (0-1) |
| depth | int | from OBJT | Drawing depth |
| visible | bool | from OBJT | Whether drawn |
| solid | bool | from OBJT | Whether solid |
| persistent | bool | from OBJT | Survives room changes |
| alarm[0..11] | int | -1 | Alarm timers |
| object_index | int | N/A | Object definition index (read-only) |
| id | int | N/A | Unique instance ID (read-only) |
| mask_index | int | from OBJT | Collision mask sprite |

### Global Built-in Variables

| Variable | Description |
|----------|-------------|
| room | Current room index |
| room_speed | Current room speed (FPS) |
| room_width | Current room width |
| room_height | Current room height |
| score | Game score |
| health | Player health |
| lives | Player lives |
| mouse_x | Mouse X in room |
| mouse_y | Mouse Y in room |
| view_xview[0..7] | View X positions |
| view_yview[0..7] | View Y positions |
| view_wview[0..7] | View widths |
| view_hview[0..7] | View heights |
| view_enabled[0..7] | View enabled flags |
| view_visible[0..7] | View visible flags |
| view_current | Currently rendering view |
| fps | Current FPS |
| current_time | Milliseconds since start |
| instance_count | Number of active instances |
| keyboard_key | Last key pressed |
| keyboard_lastkey | Previous key pressed |

## Room System

- First room in room order is loaded on game start
- `room_goto(room_index)` transitions to a new room
- `room_goto_next()` / `room_goto_previous()` for sequential navigation
- On room change:
  1. **Room End** event fires for all instances
  2. Non-persistent instances are destroyed (no Destroy event)
  3. New room data is loaded (backgrounds, views, tiles)
  4. Room instances are created
  5. Instance creation code runs
  6. Room creation code runs
  7. **Room Start** event fires for all instances
  8. Persistent instances from previous room are carried over

## Drawing System

### Coordinate System
- Origin (0,0) at top-left
- Y increases downward
- Drawing functions use room coordinates (transformed by view)

### Key Drawing Functions (needed for Undertale intro)

```
draw_sprite(sprite, subimg, x, y)
draw_sprite_ext(sprite, subimg, x, y, xscale, yscale, rot, color, alpha)
draw_set_color(color)
draw_set_alpha(alpha)
draw_set_font(font)
draw_text(x, y, string)
draw_text_ext(x, y, string, sep, w)
draw_rectangle(x1, y1, x2, y2, outline)
draw_set_halign(halign)
draw_set_valign(valign)
draw_background(bg, x, y)
draw_background_ext(bg, x, y, xscale, yscale, rot, color, alpha)
```

### Color Format
- Colors are 24-bit BGR (not RGB!): `color = blue << 16 | green << 8 | red`
- Common: `c_white=0xFFFFFF`, `c_black=0x000000`, `c_red=0x0000FF`, `c_yellow=0x00FFFF`
- `make_color_rgb(r, g, b)` creates a color

### Blend Modes
- Default: `bm_normal` (alpha blending)
- `draw_set_blend_mode(mode)` / `draw_set_blend_mode_ext(src, dest)`

## Data Types at Runtime

- **Real**: 64-bit double (all numbers)
- **String**: Reference-counted string
- **Array**: 2D arrays indexed as `array[index]` or `array[index1, index2]`
- **Boolean**: Reals where 0.5+ = true, <0.5 = false
- **undefined**: Special value for uninitialized variables

## Object Inheritance

- Objects can have a parent object (`parentId` in OBJT)
- Events propagate up the parent chain
- `event_inherited()` calls the parent's version of the current event
- Collision checks work with parent types

## Key Functions for Undertale Intro

Based on the code entries found for the intro sequence:

- `instance_create(x, y, obj)` - Create instance
- `instance_destroy()` - Destroy current instance
- `alarm[n] = value` - Set alarm timer
- `room_goto(room)` - Change room
- `keyboard_check(key)` / `keyboard_check_pressed(key)` - Input
- `draw_sprite` / `draw_sprite_ext` - Sprite rendering
- `draw_text` / `draw_text_ext` - Text rendering
- `draw_set_color` / `draw_set_font` - Drawing state
- `draw_rectangle` - Rectangle drawing
- `string_*` functions - String manipulation
- `random(n)` / `irandom(n)` - Random numbers
- `file_exists` / `ini_*` functions - Save file handling
- `script_execute(script, args...)` - Dynamic script calls

## References

- [OpenGMK](https://github.com/OpenGMK/OpenGMK) - GM8 runner in Rust (architecture reference)
- [OpenGML](https://github.com/maiple/opengml) - GML 1.4 interpreter in C++
- [UndertaleModTool](https://github.com/UnderminersTeam/UndertaleModTool) - Data format reference
- [GM:S 1.4 Manual](https://docs2.yoyogames.com/) - Official documentation
