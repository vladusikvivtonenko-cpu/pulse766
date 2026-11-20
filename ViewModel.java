package com.ezvisuals;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public final class ViewModel {
    private ViewModel() {}

    public static void apply(AbstractClientPlayerEntity player, Hand hand, ItemStack item, MatrixStack m) {
        Module vm = Modules.VIEWMODEL;
        if (!vm.enabled) return;
        boolean main = hand == Hand.MAIN_HAND;
        Arm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == Arm.RIGHT ? 1 : -1;
        String p = main ? "m" : "o";
        float x = vm.get(p + "x").f(), y = vm.get(p + "y").f(), z = vm.get(p + "z").f();
        float rx = vm.get(p + "rx").f(), ry = vm.get(p + "ry").f(), rz = vm.get(p + "rz").f();
        float sc = vm.get(p + "s").f();
        m.translate(x * side, y, z);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry * side));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz * side));
        m.scale(sc, sc, sc);
    }
}
