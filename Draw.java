package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class Draw {
    private Draw() {}
    public static final int TEXT = 0xFFEDEDF5, SUB = 0xFF8A8A9C;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static int alpha(int col, int a) {
        return (col & 0x00FFFFFF) | (Math.max(0, Math.min(255, a)) << 24);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (int) (((a >>> 24) & 255) + (((b >>> 24) & 255) - ((a >>> 24) & 255)) * t);
        int ar = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int ag = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int ab = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x + r, y, x + w - r, y + r, col);
        c.fill(x, y + r, x + w, y + h - r, col);
        c.fill(x + r, y + h - r, x + w - r, y + h, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(Math.sqrt(r * r - dy * dy));
            int off = r - dx;
            c.fill(x + off, y + i, x + r, y + i + 1, col);
            c.fill(x + w - r, y + i, x + w - off, y + i + 1, col);
            c.fill(x + off, y + h - 1 - i, x + r, y + h - i, col);
            c.fill(x + w - r, y + h - 1 - i, x + w - off, y + h - i, col);
        }
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y + 1, x + 1, y + h - 1, col);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    public static void hgrad(DrawContext c, int x, int y, int w, int h, int c1, int c2) {
        int seg = Math.max(1, Math.min(48, w));
        for (int i = 0; i < seg; i++) {
            int sx = x + w * i / seg, ex = x + w * (i + 1) / seg;
            c.fill(sx, y, ex, y + h, lerp(c1, c2, i / (float) seg));
        }
    }

    public static int w(String s) { return mc().textRenderer.getWidth(s); }

    public static void text(DrawContext c, String s, int x, int y, int col) {
        c.drawText(mc().textRenderer, s, x, y, col, false);
    }

    public static void shadow(DrawContext c, String s, int x, int y, int col) {
        c.drawText(mc().textRenderer, s, x, y, col, true);
    }

    public static void textR(DrawContext c, String s, int xRight, int y, int col) {
        text(c, s, xRight - w(s), y, col);
    }

    public static void textC(DrawContext c, String s, int xc, int y, int col) {
        text(c, s, xc - w(s) / 2, y, col);
    }

    public static void scaled(DrawContext c, String s, float x, float y, float sc, int col, boolean sh) {
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(sc, sc, 1f);
        c.drawText(mc().textRenderer, s, 0, 0, col, sh);
        m.pop();
    }

    public static void sw(DrawContext c, int x, int y, float anim, int accent) {
        rrect(c, x, y, 22, 10, 5, lerp(0xFF2A2A36, accent, anim));
        rrect(c, x + 2 + (int) (anim * 12), y + 2, 6, 6, 3, 0xFFFFFFFF);
    }
}
