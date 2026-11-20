package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** World-space visuals drawn from the HUD with the manual projection: jump circles, hit flash, hats, cosmetics, crosshair. */
public final class Vfx {
    private Vfx() {}

    private interface Col { int at(float f); }

    private static final List<double[]> JUMPS = new ArrayList<>();
    private static Entity hitEnt;
    private static long hitAt;

    // ---------------- events ----------------
    public static void addJump(Vec3d p) {
        if (!Modules.JUMPFX.enabled) return;
        JUMPS.add(new double[]{p.x, p.y + 0.02, p.z, System.currentTimeMillis()});
        while (JUMPS.size() > 8) JUMPS.remove(0);
    }

    public static void onHit(Entity e) {
        if (!Modules.HITFX.enabled) return;
        hitEnt = e;
        hitAt = System.currentTimeMillis();
    }

    // ---------------- helpers ----------------
    /** mode: 0 theme, 1 white, 2 red, 3 rainbow. f = position along the shape (0..1) for gradients. */
    public static int col(int mode, float f) {
        Theme t = Theme.cur();
        switch (mode) {
            case 1: return 0xFFFFFFFF;
            case 2: return 0xFFFF4D4D;
            case 3: {
                float h = ((System.currentTimeMillis() % 4000L) / 4000f + f) % 1f;
                return 0xFF000000 | MathHelper.hsvToRgb(h, 0.75f, 1f);
            }
            default: return Draw.lerp(t.accent, t.accent2, Math.max(0f, Math.min(1f, f)));
        }
    }

    private static void seg(DrawContext c, MinecraftClient mc, Vec3d a, Vec3d b, int col, int size) {
        int[] p = Proj.project(mc, a), q = Proj.project(mc, b);
        if (p == null || q == null) return;
        Proj.line(c, p[0], p[1], q[0], q[1], col, size);
    }

    private static void ring(DrawContext c, MinecraftClient mc, Vec3d ctr, double r, Col col, int segs, int size) {
        Vec3d prev = null;
        for (int i = 0; i <= segs; i++) {
            double a = 2 * Math.PI * i / segs;
            Vec3d v = new Vec3d(ctr.x + Math.cos(a) * r, ctr.y, ctr.z + Math.sin(a) * r);
            if (prev != null) seg(c, mc, prev, v, col.at(i / (float) segs), size);
            prev = v;
        }
    }

    /** Local point relative to a head: lx = right, lz = forward, y = absolute world height. */
    private static Vec3d pt(Vec3d pos, double yaw, double lx, double y, double lz) {
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw), fx = -Math.sin(yaw), fz = Math.cos(yaw);
        return new Vec3d(pos.x + lx * rx + lz * fx, y, pos.z + lx * rz + lz * fz);
    }

    private static void box(DrawContext c, MinecraftClient mc, Vec3d pos, double yaw,
                            double x0, double x1, double y0, double y1, double z0, double z1, int col) {
        Vec3d[] v = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            v[i] = pt(pos, yaw, (i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1);
        }
        for (int i = 0; i < 8; i++) {
            for (int j = i + 1; j < 8; j++) {
                if (Integer.bitCount(i ^ j) == 1) seg(c, mc, v[i], v[j], col, 1);
            }
        }
    }

    // ---------------- main render ----------------
    public static void render(DrawContext c, MinecraftClient mc, float td) {
        long now = System.currentTimeMillis();
        if (Modules.JUMPFX.enabled) jumps(c, mc, now);
        if (Modules.HITFX.enabled) hit(c, mc, td, now);
        boolean hat = Modules.CHINAHAT.enabled, cos = Modules.COSMETICS.enabled;
        if (!hat && !cos) return;
        boolean fp = mc.options.getPerspective().isFirstPerson();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        int n = 0;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p.isSpectator()) continue;
            boolean self = p == mc.player;
            if (self && fp) continue;
            if (!self && p.squaredDistanceTo(cam) > 32 * 32) continue;
            if (++n > 10) break;
            if (hat && (self || Modules.CHINAHAT.get("others").b())) chinaHat(c, mc, p, td);
            if (cos && (self || Modules.COSMETICS.get("others").b())) cosmetics(c, mc, p, td);
        }
    }

    // ---------------- jump circles ----------------
    private static void jumps(DrawContext c, MinecraftClient mc, long now) {
        if (JUMPS.isEmpty()) return;
        Module m = Modules.JUMPFX;
        double max = m.get("size").d();
        double dur = m.get("time").d();
        int mode = m.get("color").i();
        for (int i = JUMPS.size() - 1; i >= 0; i--) {
            double[] j = JUMPS.get(i);
            double t = (now - j[3]) / dur;
            if (t >= 1) { JUMPS.remove(i); continue; }
            double e = 1 - (1 - t) * (1 - t);
            double r = 0.2 + max * e;
            int a = (int) (230 * (1 - t));
            Vec3d ctr = new Vec3d(j[0], j[1], j[2]);
            ring(c, mc, ctr, r, f -> Draw.alpha(col(mode, f), a), 44, 2);
            ring(c, mc, ctr, r * 0.82, f -> Draw.alpha(col(mode, f), a / 2), 36, 1);
        }
    }

    // ---------------- hit flash ----------------
    private static void hit(DrawContext c, MinecraftClient mc, float td, long now) {
        Entity e = hitEnt;
        if (e == null) return;
        double dur = Modules.HITFX.get("time").d();
        double age = now - hitAt;
        if (age > dur || e.isRemoved()) { hitEnt = null; return; }
        float f = (float) (1.0 - age / dur);
        Vec3d pos = e.getLerpedPos(td);
        double hw = e.getWidth() / 2.0 + 0.05, h = e.getHeight() + 0.05;
        Vec3d[] v = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            v[i] = pos.add((i & 1) == 0 ? -hw : hw, (i & 2) == 0 ? 0 : h, (i & 4) == 0 ? -hw : hw);
        }
        int mode = Modules.HITFX.get("color").i();
        for (int i = 0; i < 8; i++) {
            for (int j = i + 1; j < 8; j++) {
                if (Integer.bitCount(i ^ j) != 1) continue;
                seg(c, mc, v[i], v[j], Draw.alpha(col(mode, (i + j) / 14f), (int) (255 * f)), 2);
            }
        }
    }

    // ---------------- china hat ----------------
    private static void chinaHat(DrawContext c, MinecraftClient mc, PlayerEntity p, float td) {
        Module m = Modules.CHINAHAT;
        int mode = m.get("color").i();
        double r = m.get("radius").d(), h = m.get("height").d();
        Vec3d pos = p.getLerpedPos(td);
        double y = pos.y + p.getHeight() - 0.02;
        Vec3d base = new Vec3d(pos.x, y, pos.z);
        Vec3d apex = new Vec3d(pos.x, y + h, pos.z);
        ring(c, mc, base, r, f -> Draw.alpha(col(mode, f), 0xF0), 40, 2);
        ring(c, mc, base.add(0, h * 0.33, 0), r * 0.66, f -> Draw.alpha(col(mode, f), 0x90), 28, 1);
        ring(c, mc, base.add(0, h * 0.66, 0), r * 0.33, f -> Draw.alpha(col(mode, f), 0x70), 20, 1);
        for (int i = 0; i < 16; i++) {
            double a = 2 * Math.PI * i / 16;
            Vec3d v = base.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            seg(c, mc, apex, v, Draw.alpha(col(mode, i / 16f), 0xB0), 1);
        }
    }

    // ---------------- cosmetics ----------------
    private static void cosmetics(DrawContext c, MinecraftClient mc, PlayerEntity p, float td) {
        Module m = Modules.COSMETICS;
        double s = m.get("scale").d();
        Vec3d pos = p.getLerpedPos(td);
        double top = pos.y + p.getHeight();
        double yaw = Math.toRadians(p.getHeadYaw());
        if (m.get("straw").b()) strawHat(c, mc, pos, top, yaw, s);
        if (m.get("bacon").b()) baconHair(c, mc, pos, top, yaw, s);
        if (m.get("halo").b()) {
            Vec3d ctr = new Vec3d(pos.x, top + 0.45 * s, pos.z);
            ring(c, mc, ctr, 0.30 * s, f -> 0xFFFFD34D, 36, 2);
            ring(c, mc, ctr, 0.26 * s, f -> 0x90FFD34D, 32, 1);
        }
    }

    private static void strawHat(DrawContext c, MinecraftClient mc, Vec3d pos, double top, double yaw, double s) {
        final int STRAW = 0xFFF5D060, STRAW2 = 0x90F5D060, RED = 0xFFD62839;
        double by = top - 0.06 * s;
        Vec3d b = new Vec3d(pos.x, by, pos.z);
        // brim
        ring(c, mc, b, 0.56 * s, f -> STRAW, 44, 2);
        ring(c, mc, b, 0.49 * s, f -> STRAW2, 36, 1);
        for (int i = 0; i < 16; i++) {
            double a = 2 * Math.PI * i / 16;
            seg(c, mc, b.add(Math.cos(a) * 0.30 * s, 0, Math.sin(a) * 0.30 * s),
                    b.add(Math.cos(a) * 0.56 * s, 0, Math.sin(a) * 0.56 * s), STRAW2, 1);
        }
        // crown
        double[] ry = {0.0, 0.08, 0.15, 0.21};
        double[] rr = {0.30, 0.29, 0.25, 0.15};
        for (int k = 1; k < ry.length; k++) {
            ring(c, mc, b.add(0, ry[k] * s, 0), rr[k] * s, f -> STRAW, 28, 1);
        }
        for (int i = 0; i < 8; i++) {
            double a = 2 * Math.PI * i / 8;
            for (int k = 0; k < ry.length - 1; k++) {
                seg(c, mc, b.add(Math.cos(a) * rr[k] * s, ry[k] * s, Math.sin(a) * rr[k] * s),
                        b.add(Math.cos(a) * rr[k + 1] * s, ry[k + 1] * s, Math.sin(a) * rr[k + 1] * s), STRAW2, 1);
            }
            seg(c, mc, b.add(Math.cos(a) * rr[3] * s, ry[3] * s, Math.sin(a) * rr[3] * s),
                    b.add(0, 0.23 * s, 0), STRAW2, 1);
        }
        // red band
        ring(c, mc, b.add(0, 0.03 * s, 0), 0.298 * s, f -> RED, 32, 2);
        ring(c, mc, b.add(0, 0.06 * s, 0), 0.292 * s, f -> RED, 32, 1);
    }

    private static void baconHair(DrawContext c, MinecraftClient mc, Vec3d pos, double top, double yaw, double s) {
        final int BR = 0xFF8A4F2B, TAN = 0xFFD9A066;
        double hi = top + 0.10 * s;
        box(c, mc, pos, yaw, -0.30 * s, 0.30 * s, top - 0.05 * s, hi, -0.30 * s, 0.30 * s, BR);          // cap
        box(c, mc, pos, yaw, -0.30 * s, 0.30 * s, top - 0.12 * s, hi, 0.28 * s, 0.33 * s, BR);           // fringe
        box(c, mc, pos, yaw, -0.30 * s, 0.30 * s, top - 0.40 * s, hi, -0.33 * s, -0.28 * s, BR);         // back
        box(c, mc, pos, yaw, -0.33 * s, -0.28 * s, top - 0.22 * s, hi, -0.30 * s, 0.20 * s, BR);         // left
        box(c, mc, pos, yaw, 0.28 * s, 0.33 * s, top - 0.22 * s, hi, -0.30 * s, 0.20 * s, BR);           // right
        for (int k = -2; k <= 2; k++) {                                                                  // bacon strips
            seg(c, mc, pt(pos, yaw, k * 0.12 * s, hi, -0.30 * s), pt(pos, yaw, k * 0.12 * s, hi, 0.30 * s), TAN, 1);
        }
    }

    // ---------------- crosshair ----------------
    private static void bar(DrawContext c, int x1, int y1, int x2, int y2, int col, boolean outline) {
        if (outline) c.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xB0000000);
        c.fill(x1, y1, x2, y2, col);
    }

    public static void crosshair(DrawContext c, MinecraftClient mc, int sw, int sh) {
        Module m = Modules.CROSSHAIR;
        if (!mc.options.getPerspective().isFirstPerson()) return;
        int cx = sw / 2, cy = sh / 2;
        int style = m.get("style").i(), size = m.get("size").i(), th = m.get("thick").i();
        int gap = m.get("gap").i();
        if (m.get("dyn").b() && mc.player != null) {
            Vec3d v = mc.player.getVelocity();
            gap += (int) Math.min(5, Math.sqrt(v.x * v.x + v.z * v.z) * 25);
        }
        int col = col(m.get("color").i(), 0f);
        boolean ol = m.get("outline").b();
        int half = th / 2;
        if (style == 0 || style == 3) {
            bar(c, cx - gap - size, cy - half, cx - gap, cy - half + th, col, ol);
            bar(c, cx + gap, cy - half, cx + gap + size, cy - half + th, col, ol);
            bar(c, cx - half, cy - gap - size, cx - half + th, cy - gap, col, ol);
            bar(c, cx - half, cy + gap, cx - half + th, cy + gap + size, col, ol);
        }
        if (style == 1 || style == 3) {
            int d = th + 1;
            bar(c, cx - d / 2, cy - d / 2, cx - d / 2 + d, cy - d / 2 + d, col, ol);
        }
        if (style == 2) {
            double r = gap + size / 2.0 + 1;
            int pts = 48;
            if (ol) {
                for (int i = 0; i < pts; i++) {
                    double a = 2 * Math.PI * i / pts;
                    int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
                    c.fill(x - 1, y - 1, x + th + 1, y + th + 1, 0x90000000);
                }
            }
            for (int i = 0; i < pts; i++) {
                double a = 2 * Math.PI * i / pts;
                int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
                c.fill(x, y, x + th, y + th, col);
            }
        }
    }
}
