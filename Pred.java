package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Pred {
    private Pred() {}

    private static final class Kind {
        final double speed, grav, drag, wdrag, roll;
        Kind(double speed, double grav, double drag, double wdrag, double roll) {
            this.speed = speed; this.grav = grav; this.drag = drag; this.wdrag = wdrag; this.roll = roll;
        }
    }

    private static Kind kind(ClientPlayerEntity p, ItemStack s) {
        Module pr = Modules.PREDICT;
        if (s.isEmpty()) return null;
        Item it = s.getItem();
        if (it == Items.BOW) {
            if (!pr.get("bow").b() || !p.isUsingItem() || !p.getActiveItem().isOf(Items.BOW)) return null;
            float pull = BowItem.getPullProgress(p.getItemUseTime());
            if (pull < 0.1f) return null;
            return new Kind(pull * 3.0, 0.05, 0.99, 0.6, 0);
        }
        if (it == Items.CROSSBOW) {
            if (!pr.get("cross").b() || !CrossbowItem.isCharged(s)) return null;
            ChargedProjectilesComponent cp = s.get(DataComponentTypes.CHARGED_PROJECTILES);
            boolean fw = cp != null && cp.contains(Items.FIREWORK_ROCKET);
            return fw ? new Kind(1.6, 0.0, 1.0, 1.0, 0) : new Kind(3.15, 0.05, 0.99, 0.6, 0);
        }
        if (it == Items.TRIDENT) {
            if (!pr.get("trident").b() || !p.isUsingItem() || !p.getActiveItem().isOf(Items.TRIDENT)) return null;
            if (p.getItemUseTime() < 10) return null;
            return new Kind(2.5, 0.05, 0.99, 0.99, 0);
        }
        if (!pr.get("thr").b()) return null;
        if (it == Items.ENDER_PEARL || it == Items.SNOWBALL || it == Items.EGG)
            return new Kind(1.5, 0.03, 0.99, 0.8, 0);
        if (it == Items.SPLASH_POTION || it == Items.LINGERING_POTION)
            return new Kind(0.5, 0.05, 0.99, 0.8, -20);
        return null;
    }

    private static String name(ItemStack s) {
        return s.getName().getString().toLowerCase(Locale.ROOT);
    }

    private static boolean has(String n, String... keys) {
        for (String k : keys) if (n.contains(k)) return true;
        return false;
    }

    private static final class Ft {
        final double r;
        final int col;
        Ft(double r, int col) { this.r = r; this.col = col; }
    }

    /** FunTime items: plast (dried kelp), disorient (ender eye), freeze (snowball), crossbow. */
    private static Ft ftInfo(ItemStack s) {
        if (s.isEmpty()) return null;
        String n = name(s);
        Module ft = Modules.FT;
        Item it = s.getItem();
        if (has(n, "пласт", "plast") || it == Items.DRIED_KELP) return new Ft(ft.get("plast").d(), 0xFF33E64D);
        if (has(n, "дезор", "disor") || it == Items.ENDER_EYE) return new Ft(ft.get("disor").d(), 0xFFCC33E6);
        if (has(n, "заморо", "freez") || it == Items.SNOWBALL) return new Ft(ft.get("freeze").d(), 0xFF33CCFF);
        if (it == Items.CROSSBOW) return new Ft(ft.get("cross").d(), 0xFFFF4D33);
        return null;
    }

    private static BlockHitResult simulate(MinecraftClient mc, ClientPlayerEntity p, Kind k, float td, List<Vec3d> out) {
        Vec3d eye = p.getCameraPosVec(td);
        double yr = Math.toRadians(p.getYaw(td));
        double pr = Math.toRadians(p.getPitch(td));
        double fx = -Math.sin(yr) * Math.cos(pr);
        double fy = -Math.sin(Math.toRadians(p.getPitch(td) + k.roll));
        double fz = Math.cos(yr) * Math.cos(pr);
        Vec3d v = new Vec3d(fx, fy, fz).normalize().multiply(k.speed);
        Vec3d pv = p.getVelocity();
        v = v.add(pv.x, p.isOnGround() ? 0 : pv.y, pv.z);
        Vec3d pos = new Vec3d(eye.x, eye.y - 0.1, eye.z);
        for (int i = 0; i < 160; i++) {
            Vec3d next = pos.add(v);
            BlockHitResult h = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (h.getType() != HitResult.Type.MISS) {
                out.add(h.getPos());
                return h;
            }
            out.add(next);
            pos = next;
            boolean water = !mc.world.getFluidState(BlockPos.ofFloored(pos)).isEmpty();
            v = v.multiply(water ? k.wdrag : k.drag).add(0, -k.grav, 0);
            if (pos.y < mc.world.getBottomY() - 10) break;
        }
        return null;
    }

    private static void radiusCircle(DrawContext c, MinecraftClient mc, Vec3d center, double r, int col) {
        Proj.circle(c, mc, center, r, Draw.alpha(col, 0xE6), 72);
        Proj.circle(c, mc, center, r * 0.97, Draw.alpha(col, 0x80), 72);
        if (Modules.FT.get("text").b()) {
            int[] s = Proj.project(mc, center.add(0, 0.6, 0));
            if (s != null) {
                String txt = "R " + String.format(Locale.ROOT, "%.1f", r);
                Draw.rrect(c, s[0] - Draw.w(txt) / 2 - 3, s[1] - 2, Draw.w(txt) + 6, 12, 3, 0xC0000000);
                Draw.textC(c, txt, s[0], s[1], col);
            }
        }
    }

    public static void render(DrawContext c, MinecraftClient mc, float td) {
        boolean pOn = Modules.PREDICT.enabled, fOn = Modules.FT.enabled;
        if (!pOn && !fOn) return;
        ClientPlayerEntity p = mc.player;
        Theme t = Theme.cur();

        // FT radius circle around the player (plast / disorient / freeze / crossbow)
        if (fOn) {
            Ft f = ftInfo(p.getMainHandStack());
            if (f == null) f = ftInfo(p.getOffHandStack());
            if (f != null) radiusCircle(c, mc, p.getLerpedPos(td).add(0, 0.05, 0), f.r, f.col);
        }

        ItemStack s = p.getMainHandStack();
        Kind k = kind(p, s);
        if (k == null) { s = p.getOffHandStack(); k = kind(p, s); }
        if (k == null) return;

        List<Vec3d> pts = new ArrayList<>();
        BlockHitResult hit = simulate(mc, p, k, td, pts);
        if (pOn) {
            int mode = Modules.PREDICT.get("color").i();
            int col = mode == 1 ? 0xFFFFFFFF : mode == 2 ? 0xFFFF4D4D : t.accent;
            int[] prev = null;
            for (Vec3d v : pts) {
                int[] sc = Proj.project(mc, v);
                if (sc != null && prev != null) Proj.line(c, prev[0], prev[1], sc[0], sc[1], Draw.alpha(col, 0xC0), 1);
                prev = sc;
            }
            if (hit != null && Modules.PREDICT.get("marker").b()) {
                int[] e = Proj.project(mc, hit.getPos());
                if (e != null) {
                    Draw.rrect(c, e[0] - 3, e[1] - 3, 6, 6, 2, col);
                    double dist = hit.getPos().distanceTo(p.getEyePos());
                    Draw.shadow(c, String.format(Locale.ROOT, "%.1fm", dist), e[0] + 7, e[1] - 4, 0xFFFFFFFF);
                }
            }
        }
        // potions: radius where they land
        if (fOn && hit != null && (s.isOf(Items.SPLASH_POTION) || s.isOf(Items.LINGERING_POTION))) {
            radiusCircle(c, mc, hit.getPos().add(0, 0.05, 0), Modules.FT.get("potion").d(), 0xFFFF6BD6);
        }
    }
}
