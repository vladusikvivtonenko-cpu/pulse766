package com.ezvisuals;

import net.minecraft.client.MinecraftClient;

/** Aspect Ratio module values (used by the projection mixin and by the world-to-screen projection). */
public final class Aspect {
    private Aspect() {}

    public static boolean on() { return Modules.ASPECT.enabled; }

    public static float target() { return Modules.ASPECT.get("ratio").f(); }

    public static double current(MinecraftClient mc) {
        if (on()) return target();
        return (double) mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());
    }
}
