package com.ezvisuals;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/** Items that can be picked for the separate cooldown timers (Disorient / Trap). */
public final class CdItems {
    private CdItems() {}

    public static final String[] NAMES = {
            "Ender Eye", "Netherite Ingot", "Phantom Membrane", "Sugar", "Dried Kelp", "Snowball",
            "Ender Pearl", "Chorus Fruit", "Totem of Undying", "Golden Apple", "Ench. Golden Apple",
            "Fire Charge", "Wind Charge", "Firework Rocket", "Shield"
    };

    private static Item[] items;

    public static Item item(int i) {
        if (items == null) {
            items = new Item[]{Items.ENDER_EYE, Items.NETHERITE_INGOT, Items.PHANTOM_MEMBRANE, Items.SUGAR,
                    Items.DRIED_KELP, Items.SNOWBALL, Items.ENDER_PEARL, Items.CHORUS_FRUIT,
                    Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
                    Items.FIRE_CHARGE, Items.WIND_CHARGE, Items.FIREWORK_ROCKET, Items.SHIELD};
        }
        return items[Math.max(0, Math.min(items.length - 1, i))];
    }
}
