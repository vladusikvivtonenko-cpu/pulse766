package com.ezvisuals;

/** User-editable look of the menu: the "Custom" theme colors and the fog effect inside the window. */
public final class GuiStyle {
    private GuiStyle() {}

    public static final Setting HUE = Setting.num("hue", "Hue", 190, 0, 360, 1);
    public static final Setting SAT = Setting.num("sat", "Saturation %", 100, 0, 100, 1);
    public static final Setting BRI = Setting.num("bri", "Brightness %", 100, 30, 100, 1);
    public static final Setting TINT = Setting.num("tint", "Background tint %", 35, 0, 100, 5);
    public static final Setting BGB = Setting.num("bgb", "Background brightness %", 7, 3, 25, 1);

    public static final Setting FOG = Setting.bool("fog", "Fog in menu", true);
    public static final Setting FOG_INT = Setting.num("fog_int", "Fog intensity %", 30, 5, 100, 5);
    public static final Setting FOG_N = Setting.num("fog_n", "Fog clouds", 4, 1, 8, 1);
    public static final Setting FOG_SPEED = Setting.num("fog_speed", "Fog speed", 3, 1, 10, 1);
    public static final Setting FOG_COL = Setting.mode("fog_col", "Fog color", 0, "Accent", "Accent + Accent 2", "White");
    public static final Setting FOG_OVER = Setting.bool("fog_over", "Fog over the content", true);

    public static final Setting[] COLOR = {HUE, SAT, BRI, TINT, BGB};
    public static final Setting[] FOGS = {FOG, FOG_INT, FOG_N, FOG_SPEED, FOG_COL, FOG_OVER};
    public static final Setting[] ALL = {HUE, SAT, BRI, TINT, BGB, FOG, FOG_INT, FOG_N, FOG_SPEED, FOG_COL, FOG_OVER};

    public static void reset() {
        for (Setting s : ALL) { s.reset(); s.anim = s.b() ? 1f : 0f; }
    }

    /** HSB (all 0..1) to 0xFFRRGGBB. */
    public static int hsb(float h, float s, float b) {
        h = ((h % 1f) + 1f) % 1f;
        s = Math.max(0f, Math.min(1f, s));
        b = Math.max(0f, Math.min(1f, b));
        float hh = h * 6f;
        int i = (int) hh;
        float f = hh - i, p = b * (1 - s), q = b * (1 - s * f), t = b * (1 - s * (1 - f));
        float r, g, bl;
        switch (i % 6) {
            case 0 -> { r = b; g = t; bl = p; }
            case 1 -> { r = q; g = b; bl = p; }
            case 2 -> { r = p; g = b; bl = t; }
            case 3 -> { r = p; g = q; bl = b; }
            case 4 -> { r = t; g = p; bl = b; }
            default -> { r = b; g = p; bl = q; }
        }
        return 0xFF000000 | ((int) (r * 255 + 0.5f) << 16) | ((int) (g * 255 + 0.5f) << 8) | (int) (bl * 255 + 0.5f);
    }
}
