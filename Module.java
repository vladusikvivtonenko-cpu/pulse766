package com.ezvisuals;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String id, name, category, desc;
    public final boolean def;
    public boolean enabled, hold, expanded, prevDown;
    public int bind = -1;
    private int defBind = -1;
    private boolean defHold = false;
    public float anim;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String id, String name, String category, String desc, boolean on, Setting... s) {
        this.id = id; this.name = name; this.category = category; this.desc = desc;
        this.def = on; this.enabled = on; this.anim = on ? 1f : 0f;
        for (Setting x : s) settings.add(x);
    }

    public Module bind(int key, boolean hold) {
        this.bind = key; this.hold = hold;
        this.defBind = key; this.defHold = hold;
        return this;
    }

    public Setting get(String sid) {
        for (Setting s : settings) if (s.id.equals(sid)) return s;
        throw new IllegalArgumentException("No setting " + sid + " in " + id);
    }

    public void reset() {
        enabled = def; bind = defBind; hold = defHold;
        for (Setting s : settings) s.reset();
    }
}
