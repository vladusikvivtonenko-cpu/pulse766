package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public final class Core {
    private Core() {}

    public static int combatTicks = 0;
    private static boolean zoomActive, espWas, jumpWasGround;
    private static int savedFov;
    private static int bridgeCd, totemCd;
    private static int fsStage, fsTimer, fsPrev;

    public static void startCombat() {
        if (Modules.PVP.enabled) combatTicks = Modules.PVP.get("time").i() * 20;
    }

    public static int totemCount(ClientPlayerEntity p) {
        int n = 0;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack s = inv.main.get(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) n += s.getCount();
        }
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) n += p.getOffHandStack().getCount();
        return n;
    }

    public static void tick(MinecraftClient mc) {
        while (EzVisualsPremium.menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new PremiumGUI());
        }
        if (Cmd.openEditor && mc.currentScreen == null) {
            Cmd.openEditor = false;
            mc.setScreen(new HudEditor(null));
        }
        Perf.tick(mc);
        if (mc.player == null || mc.world == null) return;

        ClientPlayerEntity p = mc.player;
        long win = mc.getWindow().getHandle();
        boolean free = mc.currentScreen == null;

        // ---- module binds ----
        for (Module m : Modules.ALL) {
            if (m.bind < 0) continue;
            boolean down = InputUtil.isKeyPressed(win, m.bind);
            if (free) {
                if (m.hold) m.enabled = down;
                else if (down && !m.prevDown) m.enabled = !m.enabled;
            } else if (m.hold && m.enabled) {
                m.enabled = false;
            }
            m.prevDown = down;
        }

        // ---- command + fast swap binds ----
        for (Binds.Action a : Binds.COMMANDS) {
            boolean d = a.bind >= 0 && InputUtil.isKeyPressed(win, a.bind);
            if (free && d && !a.prev && Modules.CMDBIND.enabled) runCommand(mc, a);
            a.prev = d;
        }
        for (Binds.Action a : Binds.SWAPS) {
            boolean d = a.bind >= 0 && InputUtil.isKeyPressed(win, a.bind);
            if (free && d && !a.prev) fastSwap(mc, a);
            a.prev = d;
        }
        tickFastSwap(mc);

        // ---- combat timer ----
        if (combatTicks > 0) combatTicks--;
        if (Modules.PVP.enabled && p.hurtTime > 0) {
            for (PlayerEntity o : mc.world.getPlayers()) {
                if (o != p && o.squaredDistanceTo(p) < 144) { startCombat(); break; }
            }
        }

        // ---- zoom ----
        if (Modules.ZOOM.enabled && free) {
            if (!zoomActive) { savedFov = mc.options.getFov().getValue(); zoomActive = true; }
            mc.options.getFov().setValue(Modules.ZOOM.get("fov").i());
        } else if (zoomActive) {
            mc.options.getFov().setValue(savedFov);
            zoomActive = false;
        }

        // ---- player glow ----
        if (Modules.ESP.enabled) {
            for (PlayerEntity o : mc.world.getPlayers()) if (o != p) o.setGlowing(true);
            espWas = true;
        } else if (espWas) {
            for (PlayerEntity o : mc.world.getPlayers()) if (o != p) o.setGlowing(false);
            espWas = false;
        }

        // ---- auto sprint ----
        if (Modules.SPRINT.enabled && mc.options.forwardKey.isPressed() && !p.isSneaking()
                && !p.horizontalCollision && !p.isUsingItem() && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }

        // ---- jump circles ----
        boolean og = p.isOnGround();
        if (jumpWasGround && !og && p.getVelocity().y > 0.2) Vfx.addJump(p.getPos());
        jumpWasGround = og;

        bridge(mc, p);
        totem(mc, p);
        Scripts.tick(mc);
    }

    public static void shutdown(MinecraftClient mc) {
        if (zoomActive && mc.options != null) {
            mc.options.getFov().setValue(savedFov);
            zoomActive = false;
        }
        Perf.shutdown(mc);
        Config.saveMain();
    }

    // ---------------- commands ----------------
    private static void runCommand(MinecraftClient mc, Binds.Action a) {
        sendCommand(mc, a.text);
    }

    public static void sendCommand(MinecraftClient mc, String text) {
        String cmd = text.trim();
        if (cmd.isEmpty() || mc.player == null) return;
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (Modules.PVP.enabled && Modules.PVP.get("block").b() && combatTicks > 0) {
            mc.player.sendMessage(Text.literal("[ezvisuals] Blocked in combat (" + (combatTicks / 20 + 1) + "s)"), true);
            return;
        }
        mc.player.networkHandler.sendChatCommand(cmd);
    }

    // ---------------- fast swap ----------------
    private static void fastSwap(MinecraftClient mc, Binds.Action a) {
        if (!Modules.FASTSWAP.enabled || fsStage != 0) return;
        String kw = a.text.trim().toLowerCase(Locale.ROOT);
        if (kw.isEmpty()) return;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            String n = s.getName().getString().toLowerCase(Locale.ROOT);
            String id = Registries.ITEM.getId(s.getItem()).getPath();
            if (n.contains(kw) || id.contains(kw)) {
                if (mc.player.getItemCooldownManager().isCoolingDown(s)) {
                    mc.player.sendMessage(Text.literal("[ezvisuals] " + s.getName().getString() + " is on cooldown"), true);
                    return;
                }
                fsPrev = inv.selectedSlot;
                inv.selectedSlot = i;
                fsStage = 1;
                fsTimer = 1;
                return;
            }
        }
        mc.player.sendMessage(Text.literal("[ezvisuals] Item not found: " + kw), true);
    }

    private static void tickFastSwap(MinecraftClient mc) {
        if (fsStage == 0 || mc.player == null || mc.interactionManager == null) { fsStage = 0; return; }
        if (--fsTimer > 0) return;
        if (fsStage == 1) {
            if (Modules.FASTSWAP.get("use").b()) mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            fsStage = 2;
            fsTimer = 2;
        } else {
            if (Modules.FASTSWAP.get("back").b()) mc.player.getInventory().selectedSlot = fsPrev;
            fsStage = 0;
        }
    }

    // ---------------- full bridge ----------------
    private static void bridge(MinecraftClient mc, ClientPlayerEntity p) {
        if (!Modules.BRIDGE.enabled || mc.currentScreen != null || mc.interactionManager == null) return;
        if (bridgeCd > 0) { bridgeCd--; return; }
        if (!p.isOnGround()) return;
        if (Modules.BRIDGE.get("sneak").b() && !p.isSneaking()) return;
        if (!(p.getMainHandStack().getItem() instanceof BlockItem)) return;
        Vec3d v = p.getVelocity();
        if (v.x * v.x + v.z * v.z < 0.0004) return;
        Direction dir = Direction.getFacing(v.x, 0, v.z);
        BlockPos below = BlockPos.ofFloored(p.getPos()).down();
        if (mc.world.getBlockState(below).isAir()) return;
        BlockPos target = below.offset(dir);
        if (!mc.world.getBlockState(target).isReplaceable()) return;
        double off = (dir.getOffsetX() != 0 ? p.getX() - (below.getX() + 0.5) : p.getZ() - (below.getZ() + 0.5))
                * (dir.getOffsetX() + dir.getOffsetZ());
        if (off < 0.2) return;
        Vec3d hit = new Vec3d(below.getX() + 0.5 + dir.getOffsetX() * 0.5, below.getY() + 0.5,
                below.getZ() + 0.5 + dir.getOffsetZ() * 0.5);
        BlockHitResult bhr = new BlockHitResult(hit, dir, below, false);
        ActionResult r = mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, bhr);
        if (r.isAccepted()) {
            if (Modules.BRIDGE.get("swing").b()) p.swingHand(Hand.MAIN_HAND);
            bridgeCd = Modules.BRIDGE.get("delay").i();
        }
    }

    // ---------------- totem helper ----------------
    private static void totem(MinecraftClient mc, ClientPlayerEntity p) {
        if (!Modules.TOTEM.enabled || !Modules.TOTEM.get("auto").b() || mc.interactionManager == null) return;
        if (totemCd > 0) { totemCd--; return; }
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (p.getHealth() + p.getAbsorptionAmount() > Modules.TOTEM.get("hp").d()) return;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            if (inv.main.get(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? 36 + i : i;
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
                totemCd = 8;
                return;
            }
        }
    }
}
