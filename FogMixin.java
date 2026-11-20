package com.ezvisuals.mixin;

import com.ezvisuals.CW;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Fog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class FogMixin {
    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true, require = 0)
    private static void ez$fog(CallbackInfoReturnable<Fog> cir) {
        if (!CW.fog()) return;
        Fog o = cir.getReturnValue();
        if (o == null) return;
        float[] c = CW.color();
        cir.setReturnValue(new Fog(o.start(), o.end(), o.shape(), c[0], c[1], c[2], o.alpha()));
    }
}
