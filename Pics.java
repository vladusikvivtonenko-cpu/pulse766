package com.ezvisuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Custom pictures 1-4 and the menu mascot are loaded from the config folder, no rebuild needed. */
public final class Pics {
    private Pics() {}
    public static final int N = 4;
    private static final Identifier[] IDS = new Identifier[N];
    private static final Identifier MASCOT = Identifier.of("ezvisuals", "custom/mascot");
    public static final int[] W = new int[N], H = new int[N];
    private static final boolean[] OK = new boolean[N];
    private static int mw, mh;
    private static boolean mok, loaded;

    static {
        for (int i = 0; i < N; i++) IDS[i] = Identifier.of("ezvisuals", "custom/pic" + (i + 1));
    }

    public static Path picsDir() {
        Path d = Config.dir().resolve("pictures");
        try { Files.createDirectories(d); } catch (Exception ignored) {}
        return d;
    }

    public static Path mascotFile() { return Config.dir().resolve("mascot.png"); }

    public static void ensureLoaded() {
        if (!loaded) reload();
    }

    public static boolean mascotLoaded() { return mok; }

    public static void reload() {
        loaded = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < N; i++) {
            OK[i] = false;
            int[] wh = load(mc, IDS[i], picsDir().resolve((i + 1) + ".png"));
            if (wh != null) { OK[i] = true; W[i] = wh[0]; H[i] = wh[1]; }
        }
        int[] m = load(mc, MASCOT, mascotFile());
        mok = m != null;
        if (mok) { mw = m[0]; mh = m[1]; }
    }

    private static int[] load(MinecraftClient mc, Identifier id, Path f) {
        try {
            mc.getTextureManager().destroyTexture(id);
        } catch (Exception ignored) {}
        if (!Files.exists(f)) return null;
        try (InputStream in = Files.newInputStream(f)) {
            NativeImage img = NativeImage.read(in);
            int w = img.getWidth(), h = img.getHeight();
            mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
            return new int[]{w, h};
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean has(int i) { return OK[i]; }

    private static void fit(DrawContext c, Identifier id, int iw, int ih, int x, int y, int maxW, int maxH, boolean bottom) {
        double s = Math.min((double) maxW / iw, (double) maxH / ih);
        int dw = Math.max(1, (int) (iw * s)), dh = Math.max(1, (int) (ih * s));
        int dx = x + (maxW - dw) / 2, dy = bottom ? y + maxH - dh : y + (maxH - dh) / 2;
        c.drawTexture(RenderLayer::getGuiTextured, id, dx, dy, 0f, 0f, dw, dh, iw, ih, iw, ih);
    }

    public static void draw(DrawContext c, int i, int x, int y, int maxW, int maxH) {
        if (OK[i]) fit(c, IDS[i], W[i], H[i], x, y, maxW, maxH, false);
    }

    public static void drawMascot(DrawContext c, int x, int y, int maxW, int maxH) {
        if (mok) fit(c, MASCOT, mw, mh, x, y, maxW, maxH, true);
    }
}
