package com.ezvisuals;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import java.util.Locale;

public final class U {
    private U() {}
    public static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static int armorPct(LivingEntity e) {
        int cur = 0, max = 0;
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = e.getEquippedStack(s);
            if (!st.isEmpty() && st.isDamageable()) {
                cur += st.getMaxDamage() - st.getDamage();
                max += st.getMaxDamage();
            }
        }
        return max == 0 ? -1 : Math.round(cur * 100f / max);
    }

    public static int itemPct(ItemStack s) {
        if (s.isEmpty() || !s.isDamageable()) return -1;
        return Math.round((s.getMaxDamage() - s.getDamage()) * 100f / s.getMaxDamage());
    }

    public static int pctColor(int p) {
        return p > 60 ? 0xFF5CFF7A : p > 30 ? 0xFFFFD34D : 0xFFFF5555;
    }

    public static String time(int ticks) {
        int s = Math.max(0, ticks / 20);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    public static String roman(int n) {
        return n >= 0 && n < ROMAN.length ? ROMAN[n] : String.valueOf(n);
    }

    public static int hl(ItemStack s) {
        if (s.isIn(ItemTags.SWORDS)) return 0xFFFF4D4D;
        if (s.isIn(ItemTags.AXES)) return 0xFFFF9F43;
        if (s.isIn(ItemTags.PICKAXES)) return 0xFF4DD0FF;
        if (s.isIn(ItemTags.SHOVELS)) return 0xFFC8A165;
        if (s.isIn(ItemTags.HOES)) return 0xFF9AE66E;
        if (s.isOf(Items.BOW) || s.isOf(Items.CROSSBOW) || s.isOf(Items.TRIDENT)) return 0xFF6BFF6B;
        if (s.isOf(Items.TOTEM_OF_UNDYING)) return 0xFFFFD34D;
        if (s.isOf(Items.ENDER_PEARL)) return 0xFFB36BFF;
        if (s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return 0xFFFFE066;
        if (s.isOf(Items.SPLASH_POTION)) return 0xFFFF6BD6;
        if (s.isOf(Items.SHIELD)) return 0xFF9AA5B1;
        return 0;
    }
}
