package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** Manual 3D -> screen projection so we can draw world markers from the HUD. */
public final class Proj {
    private Proj() {}

    public static int[] project(MinecraftClient mc, Vec3d p) {
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
        Vec3d fwd = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d up = right.crossProduct(fwd);
        Vec3d d = p.subtract(cp);
        double zc = d.dotProduct(fwd);
        if (zc < 0.05) return null;
        double xc = d.dotProduct(right), yc = d.dotProduct(up);
        double th = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        double aspect = Aspect.current(mc);
        int sx = (int) (sw / 2.0 + (xc / zc) / (th * aspect) * sw / 2.0);
        int sy = (int) (sh / 2.0 - (yc / zc) / th * sh / 2.0);
        return new int[]{sx, sy};
    }

    public static void line(DrawContext c, int x0, int y0, int x1, int y1, int col, int size) {
        int dx = x1 - x0, dy = y1 - y0;
        int n = Math.max(Math.abs(dx), Math.abs(dy));
        if (n > 500) return;
        if (n == 0) { c.fill(x0, y0, x0 + size, y0 + size, col); return; }
        for (int i = 0; i <= n; i++) {
            int x = x0 + dx * i / n, y = y0 + dy * i / n;
            c.fill(x, y, x + size, y + size, col);
        }
    }

    public static void circle(DrawContext c, MinecraftClient mc, Vec3d center, double r, int col, int segs) {
        int[] prev = null;
        for (int i = 0; i <= segs; i++) {
            double a = 2 * Math.PI * i / segs;
            int[] s = project(mc, new Vec3d(center.x + Math.cos(a) * r, center.y, center.z + Math.sin(a) * r));
            if (s != null && prev != null) line(c, prev[0], prev[1], s[0], s[1], col, 2);
            prev = s;
        }
    }
}
