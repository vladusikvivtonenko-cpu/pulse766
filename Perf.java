package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/** Applies / restores video options for the Performance Mode module. */
public final class Perf {
    private Perf() {}
    private static boolean applied;
    private static ParticlesMode pm;
    private static GraphicsMode gm;
    private static CloudRenderMode cm;
    private static boolean shadows;
    private static double entDist;
    private static int blend;

    public static void tick(MinecraftClient mc) {
        boolean on = Modules.PERF.enabled;
        if (on && !applied) apply(mc);
        else if (!on && applied) restore(mc);
    }

    public static void shutdown(MinecraftClient mc) {
        if (applied) restore(mc);
    }

    private static void apply(MinecraftClient mc) {
        Module m = Modules.PERF;
        pm = mc.options.getParticles().getValue();
        gm = mc.options.getGraphicsMode().getValue();
        cm = mc.options.getCloudRenderMode().getValue();
        shadows = mc.options.getEntityShadows().getValue();
        entDist = mc.options.getEntityDistanceScaling().getValue();
        blend = mc.options.getBiomeBlendRadius().getValue();
        applied = true;

        ParticlesMode[] pms = ParticlesMode.values();
        mc.options.getParticles().setValue(pms[Math.max(0, Math.min(pms.length - 1, m.get("particles").i()))]);
        if (m.get("fast").b()) mc.options.getGraphicsMode().setValue(GraphicsMode.FAST);
        if (m.get("clouds").b()) mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        if (m.get("shadows").b()) mc.options.getEntityShadows().setValue(false);
        mc.options.getEntityDistanceScaling().setValue(m.get("dist").d());
        if (m.get("blend").b()) mc.options.getBiomeBlendRadius().setValue(0);
    }

    private static void restore(MinecraftClient mc) {
        mc.options.getParticles().setValue(pm);
        mc.options.getGraphicsMode().setValue(gm);
        mc.options.getCloudRenderMode().setValue(cm);
        mc.options.getEntityShadows().setValue(shadows);
        mc.options.getEntityDistanceScaling().setValue(entDist);
        mc.options.getBiomeBlendRadius().setValue(blend);
        applied = false;
    }
}
