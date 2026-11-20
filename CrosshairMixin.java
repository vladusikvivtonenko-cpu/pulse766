package com.ezvisuals.mixin;

import com.ezvisuals.Modules;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class CrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void ez$crosshair(CallbackInfo ci) {
        if (Modules.CROSSHAIR.enabled && Modules.CROSSHAIR.get("hide").b()) ci.cancel();
    }
}
