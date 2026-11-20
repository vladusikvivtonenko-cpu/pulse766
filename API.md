# ezvisuals Lua API

Put `.lua` files into `.minecraft/config/ezvisuals/scripts/`. They load automatically and
hot-reload when the file changes (checked every second). Manage them in the menu -> Scripts tab.
Scripts are sandboxed: no `io`, `os`, `require`, `debug`, `load`. Everything goes through the global `ez` table.
`print(...)` and `ez.log(...)` write to the log shown in the Scripts tab.
A script that throws an error is stopped and shows the error in the menu until you press Reload.
Avoid endless loops (`while true do end`): they freeze the game.

## Hooks (define as global functions, all optional)
- `on_load()` - after the file is loaded
- `on_unload()` - before reload / disable
- `on_tick()` - every client tick (20/sec)
- `on_render()` - every frame, draw functions work only here
- `on_chat(text)` - on every system/game chat message (string)

## Info
- `ez.fps()`, `ez.tps()` (-1 if unknown), `ez.ping()`
- `ez.pos()` -> x, y, z    `ez.rot()` -> yaw, pitch
- `ez.health()` (with absorption), `ez.armor()` (percent, -1 if no armor), `ez.name()`
- `ez.on_ground()`, `ez.sneaking()`, `ez.sprinting()`
- `ez.in_combat()`, `ez.combat_left()` (ticks) - from the PvP Safe timer
- `ez.screen_w()`, `ez.screen_h()` (GUI scaled pixels)
- `ez.millis()`, `ez.tick()`
- `ez.held()`, `ez.offhand()`, `ez.hotbar(1..9)` -> id, name, count  (id like "minecraft:totem_of_undying")
- `ez.selected_slot()` -> 1..9,  `ez.totems()` -> count
- `ez.players()` -> list of {name,x,y,z,hp,armor,dist} (without you)
- `ez.effects()` -> list of {name,amp,ticks}  (ticks = -1 if infinite)

## Actions
- `ez.chat(text)` - message only you see;  `ez.actionbar(text)`
- `ez.command("home")` - send a command (blocked in combat if PvP Safe blocking is on)
- `ez.select_slot(1..9)`, `ez.use()` - use item in main hand
- `ez.log(text)`

## Control the client
- `ez.module(id)` -> bool;  `ez.set_module(id, bool)`
- `ez.setting(moduleId, settingId)`;  `ez.set_setting(moduleId, settingId, number_or_bool)`
- Module ids: pvpsafe fastswap cmdbind totem fthelper predict bridge zoom sprint nofire noshake esp
  nametags itemhl weather viewmodel customworld chinahat jumpfx hitfx cosmetics aspect watermark armorhud
  potionhud cooldowns lookslots crosshair panels perf autoq
- Setting ids are the short ids from the code (e.g. `fthelper`: text potion plast disor freeze cross;
  `weather`: type dens speed tint; `customworld`: preset r g b time t fog)
- `ez.accent()`, `ez.accent2()` - theme colors,  `ez.rgba(r,g,b,a)` - color number (a defaults to 255)
- `ez.set_sky(r,g,b)` (0..1), `ez.set_time(0..24000)`, `ez.reset_sky()`

## Input and timers
- `ez.key("R")` -> key code (letters, digits, F1-F12, SPACE, TAB, ENTER, ESC, arrows, LSHIFT, RSHIFT, LCTRL, LALT...)
- `ez.key_down(key)` -> bool
- `ez.bind(key, function() ... end)` - runs once on key press (only when no menu/chat is open)
- `ez.after(ticks, function() ... end)` - run later

## Drawing (only inside on_render). Colors are 0xAARRGGBB numbers.
- `ez.rect(x,y,w,h,color)`, `ez.rrect(x,y,w,h,radius,color)`, `ez.gradient(x,y,w,h,color1,color2)`
- `ez.text(str, x, y, color, shadow)`, `ez.text_width(str)`
- `ez.line(x0,y0,x1,y1,color,size)`
- `ez.item("minecraft:totem_of_undying", x, y)`
- `ez.world_to_screen(x,y,z)` -> sx, sy  (nil if behind you)
- `ez.circle(x,y,z,radius,color)` - circle on the ground in the world

## Example: show nearest player
```lua
function on_render()
  local best, bd = nil, 1e9
  for _, p in ipairs(ez.players()) do
    if p.dist < bd then best, bd = p, p.dist end
  end
  if best then
    ez.text(best.name .. " " .. math.floor(bd) .. "m", 6, 120, 0xFFFFFFFF, true)
  end
end
```
When asking Claude for a script, send this file and describe what you want.
