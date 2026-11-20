package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Lightweight rain / snow / space overlay drawn with plain rectangles (cheap for weak PCs). */
public final class Fx {
    private Fx() {}
    private static final int MAX = 300;
    private static final float[] X = new float[MAX], Y = new float[MAX], VX = new float[MAX],
            VY = new float[MAX], SZ = new float[MAX], PH = new float[MAX];
    private static int count = 0, lastMode = -1;
    private static long lastNs = 0;
    private static final Random R = new Random();
    private static float ssX, ssY, ssLife;

    private static void init(int mode, int n, int sw, int sh) {
        count = n; lastMode = mode;
        for (int i = 0; i < n; i++) spawn(i, mode, sw, sh, true);
    }

    private static void spawn(int i, int mode, int sw, int sh, boolean anywhere) {
        X[i] = R.nextFloat() * sw;
        Y[i] = anywhere ? R.nextFloat() * sh : -10;
        PH[i] = R.nextFloat() * 6.28f;
        if (mode == 0) { VX[i] = -70; VY[i] = 420 + R.nextFloat() * 220; SZ[i] = 5 + R.nextInt(6); }
        else if (mode == 1) { VX[i] = 0; VY[i] = 30 + R.nextFloat() * 45; SZ[i] = 1 + R.nextInt(3); }
        else { VX[i] = -4 - R.nextFloat() * 6; VY[i] = 0; SZ[i] = R.nextFloat() < 0.2f ? 2 : 1; }
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        Module m = Modules.WEATHER;
        int mode = m.get("type").i();
        int dens = Math.min(MAX, m.get("dens").i());
        if (Modules.PERF.enabled) dens = Math.max(10, dens / 2);
        float spd = m.get("speed").f();
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        if (mode != lastMode || dens != count) init(mode, dens, sw, sh);
        float tt = (now / 1_000_000L % 100000L) / 1000f;

        if (m.get("tint").b()) {
            int col = mode == 0 ? 0x16102A50 : mode == 1 ? 0x0CFFFFFF : 0x30140030;
            c.fill(0, 0, sw, sh, col);
        }

        for (int i = 0; i < count; i++) {
            X[i] += VX[i] * dt * spd;
            Y[i] += VY[i] * dt * spd;
            if (mode == 1) X[i] += (float) Math.sin(tt * 1.5f + PH[i]) * 12f * dt;
            if (Y[i] > sh) spawn(i, mode, sw, sh, false);
            if (X[i] < -12) X[i] = sw + 6;
            if (X[i] > sw + 12) X[i] = -6;
            int x = (int) X[i], y = (int) Y[i], s = (int) SZ[i];
            if (mode == 0) {
                c.fill(x, y, x + 1, y + s / 2, 0x90B8D8FF);
                c.fill(x - 1, y + s / 2, x, y + s, 0x90B8D8FF);
            } else if (mode == 1) {
                c.fill(x, y, x + s, y + s, 0xC8FFFFFF);
            } else {
                int a = (int) (110 + 100 * Math.sin(tt * 3f + PH[i]));
                c.fill(x, y, x + s, y + s, (Math.max(40, Math.min(255, a)) << 24) | 0xE8E8FF);
            }
        }

        if (mode == 2) {
            if (ssLife <= 0 && R.nextFloat() < 0.006f) {
                ssX = R.nextFloat() * sw * 0.7f; ssY = R.nextFloat() * sh * 0.4f; ssLife = 0.6f;
            }
            if (ssLife > 0) {
                ssLife -= dt;
                ssX += 520 * dt; ssY += 260 * dt;
                for (int k = 0; k < 14; k++) {
                    int a = (int) (200 * (1 - k / 14f) * Math.min(1f, ssLife * 3));
                    int px = (int) (ssX - k * 4), py = (int) (ssY - k * 2);
                    c.fill(px, py, px + 2, py + 1, (Math.max(0, a) << 24) | 0xFFFFFF);
                }
            }
        }
    }
}
