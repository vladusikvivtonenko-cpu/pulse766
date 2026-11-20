# EzVisuals (Fabric 1.21.4)
GitHub -> Actions -> Build -> Artifacts -> ezvisuals-jar. Jar goes to .minecraft/mods with Fabric API.
Menu: Right Shift. Tab / Shift+Tab switch tabs inside the menu.

## Chat commands (typed in normal chat, never sent to the server)
  .cfg save <n>     save all settings as a config
  .cfg load <n>     load a config
  .cfg delete <n>   delete a config
  .cfg list            list configs          .cfg reset   reset everything to defaults
  .hud                 move HUD blocks (also: menu -> HUD -> Open editor)
Configs are in .minecraft/config/ezvisuals/configs/<n>.json

## HUD
Every block is draggable: Watermark, Armor, Buffs (effects), Cooldowns (whole inventory) and the separate
Disorient (Дезка) / Trap (Трапка) timers. In the editor (`.hud`): drag with the mouse, snaps to the center lines;
hover a block and press C (center horizontally), M (middle of the screen), arrows (nudge, Shift = 5 px), R (reset).
Disorient / Trap timers: pick the item in menu -> HUD (defaults: Ender Eye / Netherite Ingot), optional READY state.
Timer seconds appear after ~1 s of the first use of an item (the full length is learned once), until then a percent is shown.
Panels behind HUD blocks are off by default (module "HUD Panels" turns them on).

## Menu look
Menu -> Themes: 7 presets + **Custom** (hue / saturation / brightness, background tint and brightness).
Below it: **Menu fog** (soft moving clouds inside the window: intensity, count, speed, color, over/behind the content).
All of it is saved in the config (`.cfg save <n>`).

Your own files, in .minecraft/config/ezvisuals/ :
  mascot.png            -> anime girl in the menu sidebar
  pictures/1.png..4.png -> Custom Pictures tab
  scripts/*.lua         -> Lua scripts (hot reload). API.md is created there automatically.
