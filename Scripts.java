package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Lua scripting. Drop .lua files into config/ezvisuals/scripts - they are loaded and hot-reloaded automatically.
 * Scripts are sandboxed: no io / os / require / debug, only the `ez` API (see API.md).
 */
public final class Scripts {
    private Scripts() {}

    public static final class Script {
        public final String name;
        final Path path;
        long mtime;
        public boolean enabled = true, failed;
        public String error = "";
        public float anim = 1f;
        Globals g;
        LuaValue onTick, onRender, onLoad, onUnload, onChat;
        final List<Timer> timers = new ArrayList<>();
        final List<Bind> binds = new ArrayList<>();

        Script(String name, Path path) { this.name = name; this.path = path; }
    }

    private static final class Timer { int left; LuaValue fn; }
    private static final class Bind { int key; LuaValue fn; boolean prev; }

    public static final List<Script> LIST = new ArrayList<>();
    public static final Map<String, Boolean> ENABLED = new HashMap<>();
    public static final Deque<String> LOG = new ArrayDeque<>();

    private static DrawContext cur;
    private static long tickCount;
    private static int scanCd;
    private static final Map<String, ItemStack> ITEMS = new HashMap<>();

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static Path dir() { return Config.dir().resolve("scripts"); }

    // ---------------- lifecycle ----------------
    public static void init() {
        boolean fresh = !Files.exists(dir());
        try { Files.createDirectories(dir()); } catch (Exception ignored) {}
        copyRes("API.md", true);
        if (fresh) copyRes("example.lua", true);
        scan();
    }

    private static void copyRes(String name, boolean overwrite) {
        Path dst = dir().resolve(name);
        if (!overwrite && Files.exists(dst)) return;
        try (InputStream in = Scripts.class.getResourceAsStream("/assets/ezvisuals/lua/" + name)) {
            if (in != null) Files.write(dst, in.readAllBytes());
        } catch (Exception ignored) {}
    }

    public static void log(String s) {
        LOG.addLast(s);
        while (LOG.size() > 40) LOG.removeFirst();
    }

    public static void scan() {
        Set<String> seen = new HashSet<>();
        try (var st = Files.list(dir())) {
            for (Path p : (Iterable<Path>) st::iterator) {
                String fn = p.getFileName().toString();
                if (!fn.endsWith(".lua")) continue;
                seen.add(fn);
                long mt = Files.getLastModifiedTime(p).toMillis();
                Script s = find(fn);
                if (s == null) {
                    s = new Script(fn, p);
                    s.enabled = ENABLED.getOrDefault(fn, true);
                    s.mtime = mt;
                    LIST.add(s);
                    if (s.enabled) load(s);
                } else if (s.mtime != mt) {
                    s.mtime = mt;
                    reload(s);
                }
            }
        } catch (Exception ignored) {}
        LIST.removeIf(s -> {
            if (!seen.contains(s.name)) { unload(s); return true; }
            return false;
        });
        LIST.sort(Comparator.comparing(s -> s.name));
    }

    private static Script find(String name) {
        for (Script s : LIST) if (s.name.equals(name)) return s;
        return null;
    }

    public static void reloadAll() {
        for (Script s : new ArrayList<>(LIST)) reload(s);
    }

    public static void reload(Script s) {
        unload(s);
        s.failed = false;
        s.error = "";
        if (s.enabled) load(s);
    }

    public static void setEnabled(Script s, boolean on) {
        s.enabled = on;
        ENABLED.put(s.name, on);
        if (on) reload(s);
        else { unload(s); s.failed = false; s.error = ""; }
    }

    public static void applyEnabled() {
        for (Script s : new ArrayList<>(LIST)) {
            boolean want = ENABLED.getOrDefault(s.name, true);
            if (want != s.enabled) setEnabled(s, want);
        }
    }

    public static void syncEnabledMap() {
        for (Script s : LIST) ENABLED.put(s.name, s.enabled);
    }

    private static void load(Script s) {
        try {
            s.timers.clear();
            s.binds.clear();
            s.g = newGlobals(s);
            String src = Files.readString(s.path, StandardCharsets.UTF_8);
            s.g.load(src, s.name).call();
            s.onTick = fn(s.g, "on_tick");
            s.onRender = fn(s.g, "on_render");
            s.onLoad = fn(s.g, "on_load");
            s.onUnload = fn(s.g, "on_unload");
            s.onChat = fn(s.g, "on_chat");
            call(s, s.onLoad);
            if (!s.failed) log("Loaded " + s.name);
        } catch (Throwable e) {
            fail(s, e);
        }
    }

    private static void unload(Script s) {
        if (s.g != null && s.onUnload != null) {
            try { s.onUnload.call(); } catch (Throwable ignored) {}
        }
        s.g = null;
        s.onTick = s.onRender = s.onLoad = s.onUnload = s.onChat = null;
        s.timers.clear();
        s.binds.clear();
    }

    private static LuaValue fn(Globals g, String n) {
        LuaValue v = g.get(n);
        return v.isfunction() ? v : null;
    }

    private static void fail(Script s, Throwable e) {
        s.failed = true;
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        int nl = m.indexOf('\n');
        if (nl > 0) m = m.substring(0, nl);
        if (m.length() > 160) m = m.substring(0, 160);
        s.error = m;
        log("[" + s.name + "] ERROR: " + m);
    }

    private static void call(Script s, LuaValue f, LuaValue... args) {
        if (f == null || s.failed) return;
        try {
            f.invoke(LuaValue.varargsOf(args));
        } catch (Throwable e) {
            fail(s, e);
        }
    }

    // ---------------- per-frame hooks ----------------
    public static void tick(MinecraftClient mc) {
        tickCount++;
        if (--scanCd <= 0) { scanCd = 20; scan(); }
        boolean free = mc.currentScreen == null;
        long win = mc.getWindow().getHandle();
        for (Script s : new ArrayList<>(LIST)) {
            if (!s.enabled || s.failed || s.g == null) continue;
            for (Bind b : new ArrayList<>(s.binds)) {
                boolean d = InputUtil.isKeyPressed(win, b.key);
                if (free && d && !b.prev) call(s, b.fn);
                b.prev = d;
            }
            for (Timer t : new ArrayList<>(s.timers)) {
                if (--t.left <= 0) { s.timers.remove(t); call(s, t.fn); }
            }
            call(s, s.onTick);
        }
    }

    public static void render(DrawContext c) {
        cur = c;
        try {
            for (Script s : new ArrayList<>(LIST)) {
                if (s.enabled && !s.failed && s.g != null) call(s, s.onRender);
            }
        } finally {
            cur = null;
        }
    }

    public static void chat(Text msg) {
        String str = msg.getString();
        for (Script s : new ArrayList<>(LIST)) {
            if (s.enabled && !s.failed && s.g != null) call(s, s.onChat, LuaValue.valueOf(str));
        }
    }

    // ---------------- API helpers ----------------
    private static void def(LuaTable t, String name, Function<Varargs, Varargs> f) {
        t.set(name, new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs a) { return f.apply(a); }
        });
    }

    private static LuaValue N(double d) { return LuaValue.valueOf(d); }
    private static LuaValue S(String s) { return LuaValue.valueOf(s); }
    private static LuaValue B(boolean b) { return LuaValue.valueOf(b); }
    private static Varargs M(LuaValue... v) { return LuaValue.varargsOf(v); }
    private static LuaValue color(int c) { return LuaValue.valueOf((double) (c & 0xFFFFFFFFL)); }
    private static int col(Varargs a, int i) { return (int) a.checklong(i); }

    private static DrawContext ctx() {
        if (cur == null) throw new LuaError("draw functions only work inside on_render()");
        return cur;
    }

    private static ClientPlayerEntity pl() {
        ClientPlayerEntity p = mc().player;
        if (p == null) throw new LuaError("not in a world");
        return p;
    }

    private static Module mod(String id) {
        for (Module m : Modules.ALL) if (m.id.equals(id)) return m;
        throw new LuaError("unknown module '" + id + "'");
    }

    private static int keyCode(Varargs a, int i) {
        LuaValue v = a.arg(i);
        if (v.isnumber()) return v.toint();
        String n = v.tojstring().toUpperCase(Locale.ROOT);
        if (n.length() == 1) return n.charAt(0);
        if (n.matches("F([1-9]|1[0-2])")) return 289 + Integer.parseInt(n.substring(1));
        switch (n) {
            case "SPACE": return 32;
            case "ESC": return 256;
            case "ENTER": return 257;
            case "TAB": return 258;
            case "BACKSPACE": return 259;
            case "RIGHT": return 262;
            case "LEFT": return 263;
            case "DOWN": return 264;
            case "UP": return 265;
            case "LSHIFT": return 340;
            case "LCTRL": return 341;
            case "LALT": return 342;
            case "RSHIFT": return 344;
            case "RCTRL": return 345;
            case "RALT": return 346;
            default: throw new LuaError("unknown key '" + n + "'");
        }
    }

    private static Varargs stackInfo(ItemStack st) {
        if (st.isEmpty()) return M(S("minecraft:air"), S(""), N(0));
        return M(S(Registries.ITEM.getId(st.getItem()).toString()), S(st.getName().getString()), N(st.getCount()));
    }

    private static LuaTable buildEz(Script s) {
        LuaTable ez = new LuaTable();

        // ---- info ----
        def(ez, "fps", a -> N(mc().getCurrentFps()));
        def(ez, "tps", a -> N(Tps.value()));
        def(ez, "ping", a -> {
            if (mc().getNetworkHandler() == null || mc().player == null) return N(-1);
            var e = mc().getNetworkHandler().getPlayerListEntry(mc().player.getUuid());
            return N(e == null ? -1 : e.getLatency());
        });
        def(ez, "pos", a -> { ClientPlayerEntity p = pl(); return M(N(p.getX()), N(p.getY()), N(p.getZ())); });
        def(ez, "rot", a -> { ClientPlayerEntity p = pl(); return M(N(p.getYaw()), N(p.getPitch())); });
        def(ez, "health", a -> N(pl().getHealth() + pl().getAbsorptionAmount()));
        def(ez, "armor", a -> N(U.armorPct(pl())));
        def(ez, "name", a -> S(pl().getName().getString()));
        def(ez, "on_ground", a -> B(pl().isOnGround()));
        def(ez, "sneaking", a -> B(pl().isSneaking()));
        def(ez, "sprinting", a -> B(pl().isSprinting()));
        def(ez, "in_combat", a -> B(Core.combatTicks > 0));
        def(ez, "combat_left", a -> N(Core.combatTicks));
        def(ez, "screen_w", a -> N(mc().getWindow().getScaledWidth()));
        def(ez, "screen_h", a -> N(mc().getWindow().getScaledHeight()));
        def(ez, "millis", a -> N(System.currentTimeMillis()));
        def(ez, "tick", a -> N(tickCount));
        def(ez, "held", a -> stackInfo(pl().getMainHandStack()));
        def(ez, "offhand", a -> stackInfo(pl().getOffHandStack()));
        def(ez, "hotbar", a -> {
            int i = a.checkint(1);
            if (i < 1 || i > 9) throw new LuaError("hotbar slot must be 1..9");
            return stackInfo(pl().getInventory().getStack(i - 1));
        });
        def(ez, "selected_slot", a -> N(pl().getInventory().selectedSlot + 1));
        def(ez, "totems", a -> N(Core.totemCount(pl())));
        def(ez, "players", a -> {
            LuaTable t = new LuaTable();
            ClientPlayerEntity me = pl();
            int n = 1;
            for (PlayerEntity o : mc().world.getPlayers()) {
                if (o == me) continue;
                LuaTable e = new LuaTable();
                e.set("name", S(o.getName().getString()));
                e.set("x", N(o.getX()));
                e.set("y", N(o.getY()));
                e.set("z", N(o.getZ()));
                e.set("hp", N(o.getHealth() + o.getAbsorptionAmount()));
                e.set("armor", N(U.armorPct(o)));
                e.set("dist", N(o.distanceTo(me)));
                t.set(n++, e);
            }
            return t;
        });
        def(ez, "effects", a -> {
            LuaTable t = new LuaTable();
            int n = 1;
            for (StatusEffectInstance e : pl().getStatusEffects()) {
                LuaTable x = new LuaTable();
                x.set("name", S(e.getEffectType().value().getName().getString()));
                x.set("amp", N(e.getAmplifier() + 1));
                x.set("ticks", N(e.isInfinite() ? -1 : e.getDuration()));
                t.set(n++, x);
            }
            return t;
        });

        // ---- actions ----
        def(ez, "chat", a -> { pl().sendMessage(Text.literal(a.checkjstring(1)), false); return LuaValue.NIL; });
        def(ez, "actionbar", a -> { pl().sendMessage(Text.literal(a.checkjstring(1)), true); return LuaValue.NIL; });
        def(ez, "command", a -> { Core.sendCommand(mc(), a.checkjstring(1)); return LuaValue.NIL; });
        def(ez, "select_slot", a -> {
            int i = a.checkint(1);
            if (i < 1 || i > 9) throw new LuaError("slot must be 1..9");
            pl().getInventory().selectedSlot = i - 1;
            return LuaValue.NIL;
        });
        def(ez, "use", a -> {
            if (mc().interactionManager != null) mc().interactionManager.interactItem(pl(), Hand.MAIN_HAND);
            return LuaValue.NIL;
        });
        def(ez, "log", a -> { log("[" + s.name + "] " + a.arg(1).tojstring()); return LuaValue.NIL; });

        // ---- module control ----
        def(ez, "module", a -> B(mod(a.checkjstring(1)).enabled));
        def(ez, "set_module", a -> { mod(a.checkjstring(1)).enabled = a.toboolean(2); return LuaValue.NIL; });
        def(ez, "setting", a -> {
            Setting st = mod(a.checkjstring(1)).get(a.checkjstring(2));
            return st.type == Setting.BOOL ? B(st.b()) : N(st.value);
        });
        def(ez, "set_setting", a -> {
            Setting st = mod(a.checkjstring(1)).get(a.checkjstring(2));
            LuaValue v = a.arg(3);
            st.set(v.isboolean() ? (v.toboolean() ? 1 : 0) : v.checkdouble());
            return LuaValue.NIL;
        });
        def(ez, "accent", a -> color(Theme.cur().accent));
        def(ez, "accent2", a -> color(Theme.cur().accent2));
        def(ez, "rgba", a -> {
            int r = a.checkint(1), g = a.checkint(2), b = a.checkint(3), al = a.optint(4, 255);
            return color(((al & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255));
        });

        // ---- world ----
        def(ez, "set_sky", a -> {
            Module m = Modules.CUSTOMWORLD;
            m.enabled = true;
            m.get("preset").set(0);
            m.get("r").set(a.checkdouble(1) * 100);
            m.get("g").set(a.checkdouble(2) * 100);
            m.get("b").set(a.checkdouble(3) * 100);
            return LuaValue.NIL;
        });
        def(ez, "set_time", a -> {
            Module m = Modules.CUSTOMWORLD;
            m.enabled = true;
            m.get("time").set(1);
            m.get("t").set(a.checkdouble(1));
            return LuaValue.NIL;
        });
        def(ez, "reset_sky", a -> { Modules.CUSTOMWORLD.enabled = false; return LuaValue.NIL; });

        // ---- input / timers ----
        def(ez, "key", a -> N(keyCode(a, 1)));
        def(ez, "key_down", a -> B(InputUtil.isKeyPressed(mc().getWindow().getHandle(), keyCode(a, 1))));
        def(ez, "bind", a -> {
            Bind b = new Bind();
            b.key = keyCode(a, 1);
            b.fn = a.checkfunction(2);
            s.binds.add(b);
            return LuaValue.NIL;
        });
        def(ez, "after", a -> {
            Timer t = new Timer();
            t.left = Math.max(1, a.checkint(1));
            t.fn = a.checkfunction(2);
            s.timers.add(t);
            return LuaValue.NIL;
        });

        // ---- drawing (inside on_render) ----
        def(ez, "rect", a -> {
            int x = a.checkint(1), y = a.checkint(2);
            ctx().fill(x, y, x + a.checkint(3), y + a.checkint(4), col(a, 5));
            return LuaValue.NIL;
        });
        def(ez, "rrect", a -> {
            Draw.rrect(ctx(), a.checkint(1), a.checkint(2), a.checkint(3), a.checkint(4), a.checkint(5), col(a, 6));
            return LuaValue.NIL;
        });
        def(ez, "gradient", a -> {
            Draw.hgrad(ctx(), a.checkint(1), a.checkint(2), a.checkint(3), a.checkint(4), col(a, 5), col(a, 6));
            return LuaValue.NIL;
        });
        def(ez, "text", a -> {
            ctx().drawText(mc().textRenderer, a.checkjstring(1), a.checkint(2), a.checkint(3),
                    a.isnil(4) ? 0xFFFFFFFF : col(a, 4), a.toboolean(5));
            return LuaValue.NIL;
        });
        def(ez, "text_width", a -> N(Draw.w(a.checkjstring(1))));
        def(ez, "line", a -> {
            Proj.line(ctx(), a.checkint(1), a.checkint(2), a.checkint(3), a.checkint(4), col(a, 5), a.optint(6, 1));
            return LuaValue.NIL;
        });
        def(ez, "item", a -> {
            String id = a.checkjstring(1);
            ItemStack st = ITEMS.computeIfAbsent(id, k -> {
                Identifier idf = Identifier.tryParse(k);
                Item it = idf == null ? Items.AIR : Registries.ITEM.get(idf);
                return new ItemStack(it);
            });
            ctx().drawItem(st, a.checkint(2), a.checkint(3));
            return LuaValue.NIL;
        });
        def(ez, "world_to_screen", a -> {
            int[] p = Proj.project(mc(), new Vec3d(a.checkdouble(1), a.checkdouble(2), a.checkdouble(3)));
            return p == null ? LuaValue.NIL : M(N(p[0]), N(p[1]));
        });
        def(ez, "circle", a -> {
            Proj.circle(ctx(), mc(), new Vec3d(a.checkdouble(1), a.checkdouble(2), a.checkdouble(3)),
                    a.checkdouble(4), col(a, 5), 64);
            return LuaValue.NIL;
        });
        return ez;
    }

    private static Globals newGlobals(Script s) {
        Globals g = JsePlatform.standardGlobals();
        for (String n : new String[]{"os", "io", "luajava", "debug", "package", "require", "dofile",
                "loadfile", "load", "loadstring", "collectgarbage"}) {
            g.set(n, LuaValue.NIL);
        }
        g.set("ez", buildEz(s));
        LuaTable dummy = new LuaTable();
        def(dummy, "print", a -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= a.narg(); i++) { if (i > 1) sb.append('\t'); sb.append(a.arg(i).tojstring()); }
            log("[" + s.name + "] " + sb);
            return LuaValue.NIL;
        });
        g.set("print", dummy.get("print"));
        return g;
    }
}
