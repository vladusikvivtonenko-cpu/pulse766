package com.ezvisuals;

import net.minecraft.client.util.InputUtil;

public final class Binds {
    private Binds() {}

    public static class Action {
        public final String label, defText;
        public String text;
        public int bind = -1;
        public boolean prev;

        Action(String label, String text) {
            this.label = label; this.text = text; this.defText = text;
        }

        public void reset() { text = defText; bind = -1; }
    }

    public static final Action[] COMMANDS = {
            new Action("Command 1", "/home"),
            new Action("Command 2", "/hub"),
            new Action("Command 3", "/darena"),
            new Action("Command 4", "")
    };

    public static final Action[] SWAPS = {
            new Action("Swap 1", "трап"),
            new Action("Swap 2", "дезор"),
            new Action("Swap 3", "явн"),
            new Action("Swap 4", "божь"),
            new Action("Swap 5", "смерч"),
            new Action("Swap 6", "wind"),
            new Action("Swap 7", ""),
            new Action("Swap 8", ""),
            new Action("Swap 9", ""),
            new Action("Swap 10", "")
    };

    public static String keyName(int key) {
        if (key < 0) return "None";
        try {
            String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
            return s.length() > 11 ? s.substring(0, 11) : s;
        } catch (Exception e) {
            return "?";
        }
    }
}
