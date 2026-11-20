package com.ezvisuals;

import java.util.Locale;

public class Setting {
    public static final int BOOL = 0, NUM = 1, MODE = 2;
    public final String id, name;
    public final int type;
    public final double def, min, max, step;
    public double value;
    public float anim;
    public String[] options = new String[0];

    private Setting(String id, String name, int type, double def, double min, double max, double step) {
        this.id = id; this.name = name; this.type = type;
        this.def = def; this.value = def;
        this.min = min; this.max = max; this.step = step;
        this.anim = (type == BOOL && def > 0.5) ? 1f : 0f;
    }

    public static Setting bool(String id, String name, boolean def) {
        return new Setting(id, name, BOOL, def ? 1 : 0, 0, 1, 1);
    }

    public static Setting num(String id, String name, double def, double min, double max, double step) {
        return new Setting(id, name, NUM, def, min, max, step);
    }

    public static Setting mode(String id, String name, int def, String... opts) {
        Setting s = new Setting(id, name, MODE, def, 0, opts.length - 1, 1);
        s.options = opts;
        return s;
    }

    public boolean b() { return value > 0.5; }
    public int i() { return (int) Math.round(value); }
    public float f() { return (float) value; }
    public double d() { return value; }

    public void set(double v) {
        if (type == NUM && step > 0) v = min + Math.round((v - min) / step) * step;
        value = Math.max(min, Math.min(max, v));
    }

    public void setFraction(double f) {
        set(min + Math.max(0, Math.min(1, f)) * (max - min));
    }

    public void cycle(int d) {
        int n = options.length;
        if (n == 0) return;
        value = ((i() + d) % n + n) % n;
    }

    public void reset() { value = def; }

    public String display() {
        if (type == MODE) return options[Math.max(0, Math.min(options.length - 1, i()))];
        if (type == BOOL) return b() ? "On" : "Off";
        if (step >= 1) return String.valueOf(i());
        return String.format(Locale.ROOT, step < 0.1 ? "%.2f" : "%.1f", value);
    }
}
