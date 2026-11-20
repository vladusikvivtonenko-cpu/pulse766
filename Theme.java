package com.ezvisuals;

public final class Theme {
    public final String name;
    public int accent, accent2, bg, panel, card;

    private Theme(String name, int accent, int accent2, int bg, int panel, int card) {
        this.name = name; this.accent = accent; this.accent2 = accent2;
        this.bg = bg; this.panel = panel; this.card = card;
    }

    /** Last entry: colors come from the sliders in GuiStyle. */
    public static final Theme CUSTOM = new Theme("Custom", 0xFF00E5FF, 0xFF2979FF, 0xF00C0E14, 0xFF11131B, 0xFF181B26);

    public static final Theme[] ALL = {
            new Theme("Cyan", 0xFF00E5FF, 0xFF2979FF, 0xF00C0E14, 0xFF11131B, 0xFF181B26),
            new Theme("Violet", 0xFFB36BFF, 0xFF6C4DFF, 0xF00F0C16, 0xFF15111D, 0xFF1D1829),
            new Theme("Crimson", 0xFFFF4D6D, 0xFFFF1744, 0xF0120C0E, 0xFF181012, 0xFF22161A),
            new Theme("Emerald", 0xFF2DFF9C, 0xFF00C853, 0xF00B1210, 0xFF101A17, 0xFF16241F),
            new Theme("Sunset", 0xFFFF9E40, 0xFFFF4081, 0xF0130E0C, 0xFF1A1310, 0xFF251B16),
            new Theme("Sakura", 0xFFFF8FD0, 0xFFC77DFF, 0xF0130C12, 0xFF1A1119, 0xFF251A24),
            new Theme("Mono", 0xFFE0E0E0, 0xFF9E9E9E, 0xF00E0E0E, 0xFF151515, 0xFF1E1E1E),
            CUSTOM
    };

    public static int index = 0;

    public static Theme cur() {
        Theme t = ALL[Math.max(0, Math.min(ALL.length - 1, index))];
        if (t == CUSTOM) refreshCustom();
        return t;
    }

    /** Recomputes the Custom theme from the GuiStyle sliders. */
    public static void refreshCustom() {
        float h = GuiStyle.HUE.f() / 360f, s = GuiStyle.SAT.f() / 100f, v = GuiStyle.BRI.f() / 100f;
        float tint = s * GuiStyle.TINT.f() / 100f, bb = GuiStyle.BGB.f() / 100f;
        CUSTOM.accent = GuiStyle.hsb(h, s, v);
        CUSTOM.accent2 = GuiStyle.hsb(h + 0.08f, s, v * 0.85f);
        CUSTOM.bg = (GuiStyle.hsb(h, tint, bb) & 0x00FFFFFF) | 0xF0000000;
        CUSTOM.panel = GuiStyle.hsb(h, tint, bb * 1.45f);
        CUSTOM.card = GuiStyle.hsb(h, tint, bb * 2.0f);
    }
}
