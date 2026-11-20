package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public final class Hud {
    private Hud() {}
    private static ItemStack TOTEM;

    private static ItemStack[] SAMPLE_ARMOR, SAMPLE_CD;

    private static void bg(DrawContext c, int x, int y, int w, int h) {
        if (!Modules.PANELS.enabled) return;
        int a = (int) (Modules.PANELS.get("alpha").d() * 2.55);
        Draw.rrect(c, x - 4, y - 3, w + 8, h + 6, 4, Draw.alpha(0x0E0E14, a));
    }

    public static void render(DrawContext c, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        boolean lite = Modules.AUTO_Q.enabled && mc.getCurrentFps() < Modules.AUTO_Q.get("fps").i();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        if (Modules.WEATHER.enabled && !lite) Fx.render(c, mc);
        if (Modules.ITEM_HL.enabled) itemHl(c, mc, sw, sh);
        Pred.render(c, mc, td);
        Vfx.render(c, mc, td);
        if (Modules.NAMETAGS.enabled && !lite) tags(c, mc, td);
        if (Modules.CROSSHAIR.enabled) Vfx.crosshair(c, mc, sw, sh);

        if (Modules.WATERMARK.enabled) drawWidget(HudPos.WATERMARK, c, mc, td, false);
        if (Modules.ARMOR_HUD.enabled) drawWidget(HudPos.ARMOR, c, mc, td, false);
        if (Modules.POTION_HUD.enabled) drawWidget(HudPos.POTIONS, c, mc, td, false);
        if (Modules.COOLDOWNS.enabled) drawWidget(HudPos.COOLDOWNS, c, mc, td, false);
        if (Modules.CD_DISORIENT.enabled) drawWidget(HudPos.DISORIENT, c, mc, td, false);
        if (Modules.CD_TRAP.enabled) drawWidget(HudPos.TRAP, c, mc, td, false);
        if (Modules.PVP.enabled && Modules.PVP.get("hud").b()) pvp(c, sw, mc);
        if (Modules.TOTEM.enabled && Modules.TOTEM.get("hud").b()) totem(c, mc, sw, sh);
        if (Modules.LOOK.enabled) look(c, mc, sw, sh);
        Scripts.render(c);
    }

    /** Draws one movable widget at its saved position. preview = show sample content when it would be empty (HUD editor). */
    public static void drawWidget(HudPos.Widget wd, DrawContext c, MinecraftClient mc, float td, boolean preview) {
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int x = HudPos.x(wd, sw), y = HudPos.y(wd, sh);
        wd.px = x; wd.py = y;
        int[] s;
        if (wd == HudPos.WATERMARK) s = watermark(c, mc, x, y);
        else if (wd == HudPos.ARMOR) s = armor(c, mc, x, y, preview);
        else if (wd == HudPos.POTIONS) s = potions(c, mc, x, y, preview);
        else if (wd == HudPos.DISORIENT) s = tracker(c, mc, Modules.CD_DISORIENT, "Disorient", x, y, td, preview);
        else if (wd == HudPos.TRAP) s = tracker(c, mc, Modules.CD_TRAP, "Trap", x, y, td, preview);
        else s = cooldowns(c, mc, x, y, td, preview);
        wd.w = s[0];
        wd.h = s[1];
    }

    // ---------------- watermark (pill) ----------------
    private static int[] watermark(DrawContext c, MinecraftClient mc, int x, int y) {
        Theme t = Theme.cur();
        Module m = Modules.WATERMARK;
        List<String> parts = new ArrayList<>();
        if (m.get("ping").b() && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            parts.add((e != null ? e.getLatency() : 0) + " ms");
        }
        if (m.get("fps").b()) parts.add(mc.getCurrentFps() + " FPS");
        if (m.get("tps").b()) parts.add("TPS " + Tps.str());
        if (m.get("xyz").b()) {
            parts.add((int) Math.floor(mc.player.getX()) + " " + (int) Math.floor(mc.player.getY()) + " " + (int) Math.floor(mc.player.getZ()));
        }
        if (m.get("name").b()) parts.add(mc.player.getName().getString());

        String brand = "Ezvisuals";
        int bw = Draw.w(brand), h = 18;
        int w = 7 + 10 + 5 + bw + 8;
        for (String p : parts) w += 6 + 1 + 6 + Draw.w(p);
        w += parts.isEmpty() ? 0 : -2;

        Draw.rrect(c, x - 1, y - 1, w + 2, h + 2, 7, Draw.alpha(t.accent, 0x30));
        Draw.rrect(c, x, y, w, h, 6, 0xD00C0C12);
        // logo
        Draw.rrect(c, x + 7, y + 4, 10, 10, 3, t.accent);
        Draw.hgrad(c, x + 9, y + 4, 6, 1, 0x80FFFFFF, 0x20FFFFFF);
        Draw.scaled(c, "E", x + 9.5f, y + 5.6f, 0.85f, 0xFFFFFFFF, false);
        int tx = x + 7 + 10 + 5;
        Draw.text(c, brand, tx, y + 5, 0xFFFFFFFF);
        tx += bw;
        for (String p : parts) {
            tx += 6;
            c.fill(tx, y + 4, tx + 1, y + h - 4, 0x40FFFFFF);
            tx += 1 + 6;
            Draw.text(c, p, tx, y + 5, 0xFFE6E6F0);
            tx += Draw.w(p);
        }
        return new int[]{w, h};
    }

    // ---------------- armor ----------------
    private static int[] armor(DrawContext c, MinecraftClient mc, int x, int y, boolean preview) {
        boolean horiz = Modules.ARMOR_HUD.get("layout").i() == 1;
        ItemStack[] st = new ItemStack[4];
        int[] pc = new int[4];
        boolean any = false;
        for (int i = 0; i < 4; i++) {
            st[i] = mc.player.getEquippedStack(U.ARMOR[i]);
            if (!st[i].isEmpty()) any = true;
            pc[i] = U.itemPct(st[i]);
        }
        int total = U.armorPct(mc.player);
        if (!any) {
            if (!preview) return new int[]{0, 0};
            if (SAMPLE_ARMOR == null) SAMPLE_ARMOR = new ItemStack[]{new ItemStack(Items.DIAMOND_HELMET),
                    new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)};
            st = SAMPLE_ARMOR;
            pc = new int[]{100, 92, 64, 28};
            total = 71;
        }
        String totTxt = total < 0 ? "--" : total + "%";
        int hdr = 12;
        if (horiz) {
            int w = 4 * 26, h = hdr + 16 + 2 + 9;
            bg(c, x, y, w, h);
            Draw.shadow(c, "Armor", x, y, 0xFFFFFFFF);
            Draw.textR(c, totTxt, x + w, y, total < 0 ? Draw.SUB : U.pctColor(total));
            for (int i = 0; i < 4; i++) {
                int ix = x + i * 26 + 5, iy = y + hdr;
                if (st[i].isEmpty()) { Draw.shadow(c, "-", ix + 6, iy + 4, Draw.SUB); continue; }
                c.drawItem(st[i], ix, iy);
                String p = pc[i] < 0 ? "n/a" : pc[i] + "%";
                Draw.textC(c, p, ix + 8, iy + 18, pc[i] < 0 ? Draw.SUB : U.pctColor(pc[i]));
            }
            return new int[]{w, h};
        }
        int rowH = 17, w = 62, h = hdr + 4 * rowH;
        bg(c, x, y, w, h);
        Draw.shadow(c, "Armor", x, y, 0xFFFFFFFF);
        Draw.textR(c, totTxt, x + w, y, total < 0 ? Draw.SUB : U.pctColor(total));
        for (int i = 0; i < 4; i++) {
            int ry = y + hdr + i * rowH;
            if (st[i].isEmpty()) { Draw.shadow(c, "-", x + 22, ry + 4, Draw.SUB); continue; }
            c.drawItem(st[i], x, ry);
            int col = pc[i] < 0 ? Draw.SUB : U.pctColor(pc[i]);
            Draw.shadow(c, pc[i] < 0 ? "n/a" : pc[i] + "%", x + 21, ry + 1, col);
            c.fill(x + 21, ry + 11, x + w, ry + 13, 0x90222230);
            if (pc[i] > 0) c.fill(x + 21, ry + 11, x + 21 + (w - 21) * pc[i] / 100, ry + 13, col);
        }
        return new int[]{w, h};
    }

    // ---------------- potions ----------------
    private static int[] potions(DrawContext c, MinecraftClient mc, int x, int y, boolean preview) {
        List<String> names = new ArrayList<>(), times = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        List<Boolean> lows = new ArrayList<>();
        List<StatusEffectInstance> list = new ArrayList<>(mc.player.getStatusEffects());
        list.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());
        if (list.size() > 10) list = list.subList(0, 10);
        for (StatusEffectInstance e : list) {
            names.add(e.getEffectType().value().getName().getString() + " " + U.roman(e.getAmplifier() + 1));
            times.add(e.isInfinite() ? "inf" : U.time(e.getDuration()));
            cols.add(0xFF000000 | e.getEffectType().value().getColor());
            lows.add(!e.isInfinite() && e.getDuration() < 200);
        }
        if (names.isEmpty()) {
            if (!preview) return new int[]{0, 0};
            names.add("Speed II"); times.add("1:30"); cols.add(0xFF7CAFC6); lows.add(false);
            names.add("Strength I"); times.add("0:45"); cols.add(0xFF932423); lows.add(false);
            names.add("Fire Resistance"); times.add("0:08"); cols.add(0xFFE49A3A); lows.add(true);
        }
        int maxW = 64;
        for (int i = 0; i < names.size(); i++) maxW = Math.max(maxW, Draw.w(names.get(i)) + Draw.w(times.get(i)) + 22);
        int w = maxW, h = 12 + names.size() * 11;
        bg(c, x, y, w, h);
        Draw.shadow(c, "Effects", x, y, 0xFFFFFFFF);
        for (int i = 0; i < names.size(); i++) {
            int ry = y + 12 + i * 11;
            c.fill(x, ry + 1, x + 2, ry + 9, cols.get(i));
            Draw.shadow(c, names.get(i), x + 6, ry, 0xFFE6E6F0);
            Draw.textR(c, times.get(i), x + w, ry, lows.get(i) ? 0xFFFF5555 : Draw.SUB);
        }
        return new int[]{w, h};
    }

    // ---------------- cooldowns ----------------
    private static final class Tr { double t0, last; float p0; boolean live; }
    private static final Map<Item, Tr> TR = new HashMap<>();
    /** Full cooldown length in ticks, learned the first time an item is seen cooling down from the start. */
    private static final Map<Item, Float> TOTAL = new HashMap<>();
    private static final Map<Item, ItemStack> FALLBACK = new HashMap<>();

    /** Seconds left, or -1 while the full length is not known yet. */
    private static float secLeft(MinecraftClient mc, Item item, boolean cd, float p, float td) {
        Tr t = TR.computeIfAbsent(item, k -> new Tr());
        if (!cd || mc.world == null) { t.live = false; return 0f; }
        double now = mc.world.getTime() + td;
        if (!t.live || now - t.last > 3) { t.live = true; t.t0 = now; t.p0 = p; }
        t.last = now;
        Float tot = TOTAL.get(item);
        if (tot == null && t.p0 - p >= 0.04f) {
            tot = (float) ((now - t.t0) / (t.p0 - p));
            if (t.p0 > 0.97f) TOTAL.put(item, tot);
        }
        return tot == null ? -1f : p * tot / 20f;
    }

    private static ItemStack findStack(MinecraftClient mc, Item item) {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) return s;
        }
        ItemStack o = mc.player.getOffHandStack();
        if (o.isOf(item)) return o;
        return FALLBACK.computeIfAbsent(item, k -> new ItemStack(k));
    }

    private static String fmtTime(float sec, float p) {
        return sec >= 0 ? String.format(Locale.ROOT, "%.1fs", sec) : Math.round(p * 100) + "%";
    }

    private static int[] cooldowns(DrawContext c, MinecraftClient mc, int x, int y, float td, boolean preview) {
        ItemCooldownManager cm = mc.player.getItemCooldownManager();
        PlayerInventory inv = mc.player.getInventory();
        Set<Item> seen = new HashSet<>();
        // items that have their own movable timer are not repeated here
        if (Modules.CD_DISORIENT.enabled) seen.add(CdItems.item(Modules.CD_DISORIENT.get("item").i()));
        if (Modules.CD_TRAP.enabled) seen.add(CdItems.item(Modules.CD_TRAP.get("item").i()));
        List<ItemStack> st = new ArrayList<>();
        List<Float> pr = new ArrayList<>();
        List<Float> sc = new ArrayList<>();
        for (int i = 0; i < 37 && st.size() < 8; i++) {
            ItemStack s = i < 36 ? inv.getStack(i) : mc.player.getOffHandStack();
            if (s.isEmpty() || seen.contains(s.getItem())) continue;
            if (cm.isCoolingDown(s)) {
                seen.add(s.getItem());
                float p = cm.getCooldownProgress(s, td);
                st.add(s);
                pr.add(p);
                sc.add(secLeft(mc, s.getItem(), true, p, td));
            }
        }
        if (st.isEmpty()) {
            if (!preview) return new int[]{0, 0};
            if (SAMPLE_CD == null) SAMPLE_CD = new ItemStack[]{new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.TOTEM_OF_UNDYING)};
            st.add(SAMPLE_CD[0]); pr.add(0.6f); sc.add(9f);
            st.add(SAMPLE_CD[1]); pr.add(0.25f); sc.add(22f);
        }
        Theme t = Theme.cur();
        int rowH = 17, w = 78, h = 12 + st.size() * rowH;
        bg(c, x, y, w, h);
        Draw.shadow(c, "Cooldowns", x, y, 0xFFFFFFFF);
        for (int i = 0; i < st.size(); i++) {
            int ry = y + 12 + i * rowH;
            c.drawItem(st.get(i), x, ry);
            float p = pr.get(i);
            Draw.shadow(c, fmtTime(sc.get(i), p), x + 21, ry + 1, 0xFFE6E6F0);
            c.fill(x + 21, ry + 11, x + w, ry + 13, 0x90222230);
            c.fill(x + 21, ry + 11, x + 21 + (int) ((w - 21) * (1f - p)), ry + 13, t.accent);
        }
        return new int[]{w, h};
    }

    /** One movable timer for a single chosen item (Disorient / Trap). */
    private static int[] tracker(DrawContext c, MinecraftClient mc, Module m, String preset, int x, int y, float td, boolean preview) {
        Theme t = Theme.cur();
        Item item = CdItems.item(m.get("item").i());
        ItemStack st = findStack(mc, item);
        ItemCooldownManager cm = mc.player.getItemCooldownManager();
        boolean cd = cm.isCoolingDown(st);
        float p = cd ? cm.getCooldownProgress(st, td) : 0f;
        float sec = secLeft(mc, item, cd, p, td);
        boolean sample = false;
        if (!cd) {
            if (preview) { sample = true; p = 0.6f; sec = 12f; }
            else if (!m.get("ready").b()) return new int[]{0, 0};
        }
        String label = m.get("label").i() == 1 ? st.getName().getString() : preset;
        boolean active = cd || sample;
        String time = active ? fmtTime(sec, p) : "READY";
        int w = Math.max(84, 21 + Draw.w(label) + 8 + Draw.w(time)), h = 17;
        bg(c, x, y, w, h);
        c.drawItem(st, x, y);
        Draw.shadow(c, label, x + 21, y, 0xFFFFFFFF);
        Draw.textR(c, time, x + w, y, active ? 0xFFE6E6F0 : 0xFF5CFF7A);
        c.fill(x + 21, y + 11, x + w, y + 13, 0x90222230);
        if (active) c.fill(x + 21, y + 11, x + 21 + (int) ((w - 21) * (1f - p)), y + 13, t.accent);
        else c.fill(x + 21, y + 11, x + w, y + 13, 0xFF5CFF7A);
        return new int[]{w, h};
    }

    // ---------------- PvP status ----------------
    private static void pvp(DrawContext c, int sw, MinecraftClient mc) {
        boolean combat = Core.combatTicks > 0;
        String a = combat ? "IN COMBAT " + String.format(Locale.ROOT, "%.1f", Core.combatTicks / 20f) + "s" : "PvP SAFE";
        String b = combat ? "Do not log out" : "Safe to disconnect";
        int col = combat ? 0xFFFF5555 : 0xFF5CFF7A;
        int y = 8;
        if (Modules.WATERMARK.enabled) y = HudPos.WATERMARK.py + HudPos.WATERMARK.h + 8;
        bg(c, sw / 2 - Math.max(Draw.w(a), Draw.w(b)) / 2, y, Math.max(Draw.w(a), Draw.w(b)), 20);
        Draw.shadow(c, a, sw / 2 - Draw.w(a) / 2, y, col);
        Draw.shadow(c, b, sw / 2 - Draw.w(b) / 2, y + 10, Draw.SUB);
    }

    // ---------------- totem ----------------
    private static void totem(DrawContext c, MinecraftClient mc, int sw, int sh) {
        if (TOTEM == null) TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);
        int cnt = Core.totemCount(mc.player);
        boolean off = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        int w = 76, h = 20, x = sw / 2 + 100, y = sh - 24;
        bg(c, x, y, w, h);
        c.drawItem(TOTEM, x, y + 2);
        Draw.shadow(c, "x" + cnt, x + 20, y + 1, cnt > 0 ? 0xFFFFFFFF : 0xFFFF5555);
        Draw.shadow(c, off ? "offhand OK" : "NO OFFHAND", x + 20, y + 11, off ? 0xFF5CFF7A : 0xFFFF5555);
    }

    // ---------------- look slots ----------------
    private static void look(DrawContext c, MinecraftClient mc, int sw, int sh) {
        Entity e = mc.targetedEntity;
        if (!(e instanceof PlayerEntity pl)) return;
        ItemStack[] st = {pl.getMainHandStack(), pl.getOffHandStack(),
                pl.getEquippedStack(U.ARMOR[0]), pl.getEquippedStack(U.ARMOR[1]),
                pl.getEquippedStack(U.ARMOR[2]), pl.getEquippedStack(U.ARMOR[3])};
        int w = 6 * 22 + 8, h = 46, x = sw / 2 + 16, y = sh / 2 - 22;
        bg(c, x, y, w, h);
        String hp = String.format(Locale.ROOT, "%.1f", pl.getHealth() + pl.getAbsorptionAmount());
        Draw.shadow(c, pl.getName().getString() + "  " + hp + " HP", x + 6, y + 4, 0xFFFFFFFF);
        for (int i = 0; i < 6; i++) {
            int ix = x + 6 + i * 22, iy = y + 16;
            Draw.rrect(c, ix - 1, iy - 1, 18, 18, 3, 0xFF1B1B25);
            if (st[i].isEmpty()) continue;
            c.drawItem(st[i], ix, iy);
            int p = U.itemPct(st[i]);
            if (p >= 0) Draw.scaled(c, p + "%", ix, iy + 18, 0.7f, U.pctColor(p), false);
        }
    }

    // ---------------- item highlight ----------------
    private static void itemHl(DrawContext c, MinecraftClient mc, int sw, int sh) {
        Theme t = Theme.cur();
        Module m = Modules.ITEM_HL;
        PlayerInventory inv = mc.player.getInventory();
        float pl = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 250.0));
        int baseX = sw / 2 - 90, y = sh - 19;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            int col = U.hl(s);
            if (col == 0) continue;
            int x = baseX + i * 20 + 2;
            int a = m.get("pulse").b() ? (int) (90 + 110 * pl) : 190;
            if (m.get("fill").b()) c.fill(x - 1, y - 1, x + 17, y + 17, Draw.alpha(col, 0x2C));
            Draw.outline(c, x - 2, y - 2, 20, 20, Draw.alpha(col, a));
        }
        int sx = baseX + inv.selectedSlot * 20 + 2;
        Draw.outline(c, sx - 3, y - 3, 22, 22, Draw.alpha(t.accent, (int) (120 + 120 * pl)));
    }

    // ---------------- name tags ----------------
    private static void tags(DrawContext c, MinecraftClient mc, float td) {
        Module m = Modules.NAMETAGS;
        double maxD = m.get("dist").d();
        boolean items = m.get("items").b(), showHp = m.get("hp").b();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Theme t = Theme.cur();
        int drawn = 0;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator()) continue;
            Vec3d pos = p.getLerpedPos(td);
            double dist = pos.distanceTo(cam);
            if (dist > maxD) continue;
            int[] s = Proj.project(mc, new Vec3d(pos.x, pos.y + p.getHeight() + 0.5, pos.z));
            if (s == null) continue;
            if (++drawn > 24) break;

            String name = p.getName().getString();
            String hp = showHp ? String.format(Locale.ROOT, "%.1f", p.getHealth() + p.getAbsorptionAmount()) : "";
            int ap = U.armorPct(p);
            String arm = ap < 0 ? "" : ap + "%";
            int w1 = Draw.w(name) + (showHp ? Draw.w(hp) + 8 : 0) + (arm.isEmpty() ? 0 : Draw.w(arm) + 8);
            int w = Math.max(w1 + 10, items ? 6 * 14 + 8 : 0);
            int h = 14 + (items ? 16 : 0);
            float sc = (float) Math.max(0.6, Math.min(1.0, 1.25 - dist / 90.0));

            MatrixStack ms = c.getMatrices();
            ms.push();
            ms.translate((float) s[0], (float) s[1], 0f);
            ms.scale(sc, sc, 1f);
            int x = -w / 2, y = -h;
            Draw.rrect(c, x, y, w, h, 3, 0xB0101018);
            c.fill(x + 2, y, x + w - 2, y + 1, t.accent);
            int tx = x + 5;
            Draw.text(c, name, tx, y + 3, 0xFFFFFFFF);
            tx += Draw.w(name) + 4;
            if (showHp) {
                float hv = p.getHealth() + p.getAbsorptionAmount();
                Draw.text(c, hp, tx, y + 3, hv > 14 ? 0xFF5CFF7A : hv > 7 ? 0xFFFFD34D : 0xFFFF5555);
                tx += Draw.w(hp) + 4;
            }
            if (!arm.isEmpty()) Draw.text(c, arm, tx, y + 3, U.pctColor(ap));
            if (items) {
                ItemStack[] st = {p.getMainHandStack(), p.getOffHandStack(),
                        p.getEquippedStack(U.ARMOR[0]), p.getEquippedStack(U.ARMOR[1]),
                        p.getEquippedStack(U.ARMOR[2]), p.getEquippedStack(U.ARMOR[3])};
                MatrixStack m2 = c.getMatrices();
                for (int i = 0; i < 6; i++) {
                    if (st[i].isEmpty()) continue;
                    m2.push();
                    m2.translate((float) (x + 4 + i * 14), (float) (y + 15), 0f);
                    m2.scale(0.75f, 0.75f, 1f);
                    c.drawItem(st[i], 0, 0);
                    m2.pop();
                }
            }
            ms.pop();
        }
    }
}
