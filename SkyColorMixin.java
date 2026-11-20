package com.ezvisuals.mixin;

import com.ezvisuals.CW;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class SkyColorMixin {
    @Inject(method = "getSkyColor(Lnet/minecraft/util/math/Vec3d;F)Lnet/minecraft/util/math/Vec3d;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void ez$sky(CallbackInfoReturnable<Vec3d> cir) {
        if (CW.on()) {
            float[] c = CW.color();
            cir.setReturnValue(new Vec3d(c[0], c[1], c[2]));
        }
    }
}
