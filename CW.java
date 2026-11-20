package com.ezvisuals;

/** Custom Worlds values used by the sky / time / fog mixins. */
public final class CW {
    private CW() {}
    private static final float[][] PRE = {{0.65f, 0.02f, 0.02f}, {0.02f, 0.02f, 0.12f}, {1.0f, 0.45f, 0.2f}, {0.3f, 0.0f, 0.5f}};
    private static final long[] PT = {13200, 18000, 12800, 18000};

    public static boolean on() { return Modules.CUSTOMWORLD.enabled; }

    public static float[] color() {
        Module m = Modules.CUSTOMWORLD;
        int p = m.get("preset").i();
        if (p > 0) return PRE[p - 1];
        return new float[]{m.get("r").f() / 100f, m.get("g").f() / 100f, m.get("b").f() / 100f};
    }

    public static boolean timeActive() {
        Module m = Modules.CUSTOMWORLD;
        return m.enabled && (m.get("preset").i() > 0 || m.get("time").b());
    }

    public static long time() {
        Module m = Modules.CUSTOMWORLD;
        int p = m.get("preset").i();
        return p > 0 ? PT[p - 1] : (long) m.get("t").d();
    }

    public static boolean fog() { return Modules.CUSTOMWORLD.enabled && Modules.CUSTOMWORLD.get("fog").b(); }
}
