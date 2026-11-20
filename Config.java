package com.ezvisuals;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class Config {
    private Config() {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 2;

    public static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("ezvisuals");
        try { Files.createDirectories(d); } catch (IOException ignored) {}
        return d;
    }

    /** Named configs for the .cfg command live here. */
    public static Path cfgDir() {
        Path d = dir().resolve("configs");
        try { Files.createDirectories(d); } catch (IOException ignored) {}
        return d;
    }

    private static Path mainFile() { return dir().resolve("main.json"); }
    private static Path named(String name) { return cfgDir().resolve(name + ".json"); }

    // ---------------- autosave ----------------
    public static boolean saveMain() { return write(mainFile()); }
    public static boolean loadMain() { return read(mainFile()); }

    // ---------------- .cfg command ----------------
    public static boolean saveNamed(String name) { return write(named(name)); }
    public static boolean loadNamed(String name) { return read(named(name)); }
    public static boolean existsNamed(String name) { return Files.exists(named(name)); }

    public static boolean deleteNamed(String name) {
        try { return Files.deleteIfExists(named(name)); } catch (IOException e) { return false; }
    }

    public static List<String> listNamed() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(cfgDir())) {
            s.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.endsWith(".json")) out.add(n.substring(0, n.length() - 5));
            });
        } catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    // ---------------- io ----------------
    private static boolean write(Path f) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("ver", VERSION);
            root.addProperty("theme", Theme.index);
            JsonObject mods = new JsonObject();
            for (Module m : Modules.ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("on", m.enabled);
                o.addProperty("bind", m.bind);
                o.addProperty("hold", m.hold);
                JsonObject s = new JsonObject();
                for (Setting st : m.settings) s.addProperty(st.id, st.value);
                o.add("s", s);
                mods.add(m.id, o);
            }
            root.add("modules", mods);
            JsonObject hud = new JsonObject();
            for (HudPos.Widget w : HudPos.ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("x", w.fx);
                o.addProperty("y", w.fy);
                hud.add(w.id, o);
            }
            root.add("hud", hud);
            JsonObject gui = new JsonObject();
            for (Setting st : GuiStyle.ALL) gui.addProperty(st.id, st.value);
            root.add("gui", gui);
            root.add("commands", actions(Binds.COMMANDS));
            root.add("swaps", actions(Binds.SWAPS));
            Scripts.syncEnabledMap();
            JsonObject sc = new JsonObject();
            for (var e : Scripts.ENABLED.entrySet()) sc.addProperty(e.getKey(), e.getValue());
            root.add("scripts", sc);
            Files.writeString(f, GSON.toJson(root), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static JsonArray actions(Binds.Action[] arr) {
        JsonArray a = new JsonArray();
        for (Binds.Action x : arr) {
            JsonObject o = new JsonObject();
            o.addProperty("text", x.text);
            o.addProperty("bind", x.bind);
            a.add(o);
        }
        return a;
    }

    private static void readActions(JsonObject root, String key, Binds.Action[] arr) {
        if (!root.has(key)) return;
        JsonArray a = root.getAsJsonArray(key);
        for (int i = 0; i < arr.length && i < a.size(); i++) {
            JsonObject o = a.get(i).getAsJsonObject();
            if (o.has("text")) arr[i].text = o.get("text").getAsString();
            if (o.has("bind")) arr[i].bind = o.get("bind").getAsInt();
        }
    }

    private static boolean read(Path f) {
        try {
            if (!Files.exists(f)) return false;
            JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            int ver = root.has("ver") ? root.get("ver").getAsInt() : 1;
            if (root.has("theme")) Theme.index = Math.max(0, Math.min(Theme.ALL.length - 1, root.get("theme").getAsInt()));
            if (root.has("modules")) {
                JsonObject mods = root.getAsJsonObject("modules");
                for (Module m : Modules.ALL) {
                    if (!mods.has(m.id)) continue;
                    JsonObject o = mods.getAsJsonObject(m.id);
                    if (o.has("on")) m.enabled = o.get("on").getAsBoolean();
                    if (o.has("bind")) m.bind = o.get("bind").getAsInt();
                    if (o.has("hold")) m.hold = o.get("hold").getAsBoolean();
                    // the watermark was redesigned in v2: keep its new default content instead of the old long bar
                    if (o.has("s") && !(ver < 2 && m == Modules.WATERMARK)) {
                        JsonObject s = o.getAsJsonObject("s");
                        for (Setting st : m.settings) if (s.has(st.id)) st.set(s.get(st.id).getAsDouble());
                    }
                    m.anim = m.enabled ? 1f : 0f;
                    for (Setting st : m.settings) st.anim = st.b() ? 1f : 0f;
                }
            }
            if (root.has("hud")) {
                JsonObject hud = root.getAsJsonObject("hud");
                for (HudPos.Widget w : HudPos.ALL) {
                    if (!hud.has(w.id)) continue;
                    JsonObject o = hud.getAsJsonObject(w.id);
                    if (o.has("x")) w.fx = Math.max(0, Math.min(1, o.get("x").getAsDouble()));
                    if (o.has("y")) w.fy = Math.max(0, Math.min(1, o.get("y").getAsDouble()));
                }
            }
            if (root.has("gui")) {
                JsonObject g = root.getAsJsonObject("gui");
                for (Setting st : GuiStyle.ALL) {
                    if (g.has(st.id)) st.set(g.get(st.id).getAsDouble());
                    st.anim = st.b() ? 1f : 0f;
                }
            }
            readActions(root, "commands", Binds.COMMANDS);
            readActions(root, "swaps", Binds.SWAPS);
            if (root.has("scripts")) {
                for (var e : root.getAsJsonObject("scripts").entrySet()) Scripts.ENABLED.put(e.getKey(), e.getValue().getAsBoolean());
                Scripts.applyEnabled();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void reset() {
        Theme.index = 0;
        for (Module m : Modules.ALL) { m.reset(); m.anim = m.enabled ? 1f : 0f; }
        for (Binds.Action a : Binds.COMMANDS) a.reset();
        for (Binds.Action a : Binds.SWAPS) a.reset();
        HudPos.resetAll();
        GuiStyle.reset();
    }
}
