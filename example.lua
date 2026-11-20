-- Example ezvisuals script. Edit or replace this file: it hot-reloads automatically.

function on_load()
  ez.log("example.lua loaded")
end

function on_render()
  local text = "Lua OK | FPS " .. ez.fps() .. " | Totems " .. ez.totems()
  local x, y = 6, ez.screen_h() - 60
  ez.rrect(x, y, ez.text_width(text) + 10, 14, 3, 0xC00E0E14)
  ez.text(text, x + 5, y + 3, ez.accent(), true)
end

ez.bind("G", function()
  ez.actionbar("Pressed G from a Lua script")
end)
