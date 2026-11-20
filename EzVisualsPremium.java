package com.ezvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class EzVisualsPremium implements ClientModInitializer {

    public static final String MOD_ID = "ezvisuals";
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ezvisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.ezvisuals"));

        Config.loadMain();
        Scripts.init();
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay) Scripts.chat(msg); });

        ClientSendMessageEvents.ALLOW_CHAT.register(msg -> !Cmd.onChat(msg));

        ClientTickEvents.END_CLIENT_TICK.register(Core::tick);
        HudRenderCallback.EVENT.register((ctx, tc) -> Hud.render(ctx, tc.getTickDelta(false)));
        ClientLifecycleEvents.CLIENT_STOPPING.register(Core::shutdown);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) {
                if (entity instanceof PlayerEntity) Core.startCombat();
                if (entity instanceof LivingEntity) Vfx.onHit(entity);
            }
            return ActionResult.PASS;
        });
    }
}
