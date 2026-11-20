package com.ezvisuals.mixin;

import com.ezvisuals.CW;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class TimeMixin {
    @Inject(method = "getTimeOfDay()J", at = @At("HEAD"), cancellable = true, require = 0)
    private void ez$timeOfDay(CallbackInfoReturnable<Long> cir) {
        if (((Object) this) instanceof ClientWorld && CW.timeActive()) cir.setReturnValue(CW.time());
    }

    @Inject(method = "getLunarTime()J", at = @At("HEAD"), cancellable = true, require = 0)
    private void ez$lunar(CallbackInfoReturnable<Long> cir) {
        if (((Object) this) instanceof ClientWorld && CW.timeActive()) cir.setReturnValue(CW.time());
    }
}
