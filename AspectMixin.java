package com.ezvisuals.mixin;

import com.ezvisuals.Aspect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class AspectMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true, require = 0)
    private void ez$aspect(CallbackInfoReturnable<Matrix4f> cir) {
        if (!Aspect.on()) return;
        Matrix4f m = cir.getReturnValue();
        if (m == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float cur = (float) mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());
        m.m00(m.m00() * (cur / Aspect.target()));
        cir.setReturnValue(m);
    }
}
