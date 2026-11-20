package com.ezvisuals.mixin;

import com.ezvisuals.Tps;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class TpsMixin {
    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"), require = 0)
    private void ez$time(CallbackInfo ci) {
        Tps.onUpdate();
    }
}
