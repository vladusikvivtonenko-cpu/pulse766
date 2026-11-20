package com.ezvisuals;

import java.util.Locale;

public final class Tps {
    private Tps() {}
    private static long last = 0;
    private static double tps = -1;

    public static void onUpdate() {
        long now = System.nanoTime();
        if (last != 0) {
            double dt = (now - last) / 1e9;
            if (dt > 0.05 && dt < 10) {
                double inst = Math.max(0, Math.min(20, 20.0 / dt));
                tps = tps < 0 ? inst : tps * 0.6 + inst * 0.4;
            }
        }
        last = now;
    }

    public static void reset() { last = 0; tps = -1; }

    public static double value() { return tps; }

    public static String str() {
        return tps < 0 ? "--" : String.format(Locale.ROOT, "%.1f", tps);
    }
}
