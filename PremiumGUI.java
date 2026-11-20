package com.ezvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class PremiumGUI extends Screen {
    private static final String[] TABS = {"Combat", "Movement", "Visuals", "HUD", "Optimize", "Binds", "Themes", "Scripts", "Pictures"};
    private static final String[] CATS = {"Combat", "Movement", "Visuals", "HUD", "Optimize"};

    private record Hit(int x, int y, int w, int h, IntConsumer click, DoubleConsumer drag) {}

    private int tab = 0;
    private final float[] scroll = new float[TABS.length];
    private int contentH, maxScroll, visTop, visBottom, mouseX, mouseY;
    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;
    private Object listening;
    private Binds.Action editing;
    private String status = "";
    private long statusUntil;
    private final long openedAt = System.currentTimeMillis();

    public PremiumGUI() {
        super(Text.literal("ezvisuals"));
        Pics.ensureLoaded();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // no vanilla blur
    }

    @Override
    public void removed() {
        Config.saveMain();
    }

    private void msg(String s) {
        status = s;
        statusUntil = System.currentTimeMillis() + 2500;
    }

    // ---------------- hit helpers ----------------
    private void hitRaw(int x, int y, int w, int h, IntConsumer click) {
        hits.add(new Hit(x, y, w, h, click, null));
    }

    private void hit(int x, int y, int w, int h, IntConsumer click) {
        hitDrag(x, y, w, h, click, null);
    }

    private void hitDrag(int x, int y, int w, int h, IntConsumer click, DoubleConsumer drag) {
        int ny = Math.max(y, visTop), nb = Math.min(y + h, visBottom);
        if (nb <= ny) return;
        hits.add(new Hit(x, ny, w, nb - ny, click, drag));
    }

    private boolean over(int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= Math.max(y, visTop) && mouseY < Math.min(y + h, visBottom)
                && mouseY >= y && mouseY < y + h;
    }

    private String trim(String s, int w) {
        return client.textRenderer.trimToWidth(s, Math.max(10, w));
    }

    // ---------------- main render ----------------
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        mouseX = mx; mouseY = my;
        hits.clear();
        Theme t = Theme.cur();
        float a = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
        float e = 1f - (1f - a) * (1f - a) * (1f - a);

        int W = Math.min(540, width - 16), H = Math.min(340, height - 16);
        int x0 = (width - W) / 2, y0 = (height - H) / 2 + (int) ((1f - e) * 14);
        int SB = 106;

        Draw.rrect(c, x0 - 1, y0 - 1, W + 2, H + 2, 7, Draw.alpha(t.accent, 0x60));
        Draw.rrect(c, x0, y0, W, H, 6, t.bg);
        Draw.rrect(c, x0, y0, SB, H, 6, t.panel);
        c.fill(x0 + SB - 6, y0, x0 + SB, y0 + H, t.panel);
        fog(c, x0, y0, W, H, t, 1f);

        // logo
        Draw.scaled(c, "ez", x0 + 12, y0 + 10, 1.4f, t.accent, true);
        int lw = (int) (Draw.w("ez") * 1.4f);
        Draw.scaled(c, "visuals", x0 + 12 + lw, y0 + 10, 1.4f, 0xFFFFFFFF, true);
        Draw.text(c, "premium", x0 + 12, y0 + 26, Draw.SUB);

        // tabs
        int ty = y0 + 42;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int ry = ty + i * 18;
            boolean sel = tab == i;
            boolean hov = mx >= x0 + 6 && mx < x0 + SB - 6 && my >= ry && my < ry + 17;
            if (sel) {
                Draw.rrect(c, x0 + 6, ry, SB - 12, 17, 4, Draw.alpha(t.accent, 0x2A));
                c.fill(x0 + 6, ry + 3, x0 + 8, ry + 14, t.accent);
            } else if (hov) {
                Draw.rrect(c, x0 + 6, ry, SB - 12, 17, 4, 0x14FFFFFF);
            }
            Draw.rrect(c, x0 + 13, ry + 6, 5, 5, 2, sel ? t.accent : 0xFF4A4A5A);
            Draw.text(c, TABS[i], x0 + 23, ry + 4, sel ? 0xFFFFFFFF : (hov ? 0xFFD0D0DC : Draw.SUB));
            hitRaw(x0 + 6, ry, SB - 12, 17, b -> { tab = idx; listening = null; editing = null; });
        }

        // mascot
        int mTop = ty + TABS.length * 18 + 2, mBot = y0 + H - 16;
        if (mBot - mTop >= 40) {
            if (Pics.mascotLoaded()) {
                Pics.drawMascot(c, x0 + 4, mTop, SB - 8, mBot - mTop);
            } else {
                Draw.textC(c, "mascot.png", x0 + SB / 2, mBot - 24, 0xFF3A3A4A);
                Draw.textC(c, "in config/ezvisuals", x0 + SB / 2, mBot - 14, 0xFF3A3A4A);
            }
        }
        Draw.text(c, "v1.0  MC 1.21.4", x0 + 10, y0 + H - 12, 0xFF55556A);

        // top bar
        int x1 = x0 + SB;
        Draw.scaled(c, TABS[tab], x1 + 12, y0 + 9, 1.2f, 0xFFFFFFFF, true);
        if (System.currentTimeMillis() < statusUntil) {
            Draw.text(c, status, x1 + 24 + (int) (Draw.w(TABS[tab]) * 1.2f), y0 + 10, t.accent);
        }
        topStats(c, x0 + W - 12, y0 + 10, t);
        Draw.hgrad(c, x1 + 8, y0 + 27, W - SB - 16, 1, t.accent, t.accent2);

        // content
        int cx = x1 + 8, cy = y0 + 33, cw = W - SB - 16, ch = H - 33 - 8;
        visTop = cy; visBottom = cy + ch;
        maxScroll = Math.max(0, contentH - ch);
        scroll[tab] = Math.max(0, Math.min(maxScroll, scroll[tab]));
        int innerW = cw - (maxScroll > 0 ? 8 : 0);

        c.enableScissor(cx - 2, cy, cx + cw + 2, cy + ch);
        int y = cy - (int) scroll[tab];
        int startY = y;
        if (tab < CATS.length) y = modules(c, CATS[tab], cx, y, innerW);
        else if (tab == 5) y = binds(c, cx, y, innerW);
        else if (tab == 6) y = themes(c, cx, y, innerW);
        else if (tab == 7) y = scripts(c, cx, y, innerW);
        else y = pictures(c, cx, y, innerW, ch);
        c.disableScissor();
        contentH = y - startY;

        if (maxScroll > 0) {
            int bh = Math.max(20, ch * ch / Math.max(ch, contentH));
            int by = cy + (int) ((ch - bh) * (scroll[tab] / maxScroll));
            Draw.rrect(c, x0 + W - 7, by, 3, bh, 1, Draw.alpha(t.accent, 0xAA));
        }
        if (GuiStyle.FOG_OVER.b()) fog(c, x0, y0, W, H, t, 0.3f);
    }

    /** Soft drifting clouds of color inside the menu window. k scales the strength. */
    private void fog(DrawContext c, int x0, int y0, int W, int H, Theme t, float k) {
        if (!GuiStyle.FOG.b()) return;
        int n = GuiStyle.FOG_N.i();
        double tm = System.currentTimeMillis() * 0.00012 * GuiStyle.FOG_SPEED.d();
        int total = (int) (GuiStyle.FOG_INT.d() * 2.55 * 0.55 * k);
        int layers = 6;
        int a = Math.max(1, total / layers);
        c.enableScissor(x0, y0, x0 + W, y0 + H);
        for (int i = 0; i < n; i++) {
            double ph = i * 2.1;
            int cx = x0 + (int) (W * (0.5 + 0.5 * Math.sin(tm * 1.0 + ph * 1.3)));
            int cy = y0 + (int) (H * (0.5 + 0.5 * Math.cos(tm * 0.8 + ph)));
            int R = (int) (Math.min(W, H) * (0.30 + 0.06 * Math.sin(tm * 1.7 + ph)));
            int col = switch (GuiStyle.FOG_COL.i()) {
                case 1 -> (i % 2 == 0) ? t.accent : t.accent2;
                case 2 -> 0xFFFFFFFF;
                default -> t.accent;
            };
            for (int L = 0; L < layers; L++) {
                int r = R * (layers - L) / layers;
                int fc = Draw.alpha(col, a);
                for (int dy = -r; dy < r; dy += 2) {
                    int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
                    if (half > 0) c.fill(cx - half, cy + dy, cx + half, cy + dy + 2, fc);
                }
            }
        }
        c.disableScissor();
    }

    private void topStats(DrawContext c, int xRight, int y, Theme t) {
        String[][] seg = {
                {"FPS", String.valueOf(client.getCurrentFps())},
                {"TPS", Tps.str()},
                {"XYZ", client.player == null ? "-" : (int) Math.floor(client.player.getX()) + " "
                        + (int) Math.floor(client.player.getY()) + " " + (int) Math.floor(client.player.getZ())}
        };
        int total = 0;
        for (String[] s : seg) total += Draw.w(s[0]) + 3 + Draw.w(s[1]) + 12;
        int x = xRight - total + 12;
        for (String[] s : seg) {
            Draw.text(c, s[0], x, y, Draw.SUB);
            x += Draw.w(s[0]) + 3;
            Draw.text(c, s[1], x, y, t.accent);
            x += Draw.w(s[1]) + 12;
        }
    }

    // ---------------- module tabs ----------------
    private int hudEditorRow(DrawContext c, int x, int y, int w) {
        Theme t = Theme.cur();
        int h = 28;
        if (y + h >= visTop && y <= visBottom) {
            Draw.rrect(c, x, y, w, h, 5, t.card);
            Draw.text(c, "Move HUD blocks", x + 10, y + 5, 0xFFE6E6F0);
            Draw.text(c, trim("Drag buffs, cooldowns, Disorient/Trap timers... Chat: .hud", w - 110), x + 10, y + 16, Draw.SUB);
            button(c, x + w - 78, y + 7, 70, 14, "Open editor", t.accent);
            hit(x + w - 78, y + 7, 70, 14, b -> client.setScreen(new HudEditor(this)));
        }
        return y + h + 4;
    }

    private int modules(DrawContext c, String cat, int x, int y, int w) {
        Theme t = Theme.cur();
        if (cat.equals("HUD")) y = hudEditorRow(c, x, y, w);
        for (Module m : Modules.ALL) {
            if (!m.category.equals(cat)) continue;
            int hh = 28;
            int sh = (m.expanded && !m.settings.isEmpty()) ? m.settings.size() * 20 + 26 : 0;
            int total = hh + sh;
            m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.3f;
            boolean vis = y + total >= visTop && y <= visBottom;
            if (vis) {
                boolean hover = over(x, y, w, hh);
                Draw.rrect(c, x, y, w, total, 5, hover ? Draw.lerp(t.card, 0xFFFFFFFF, 0.05f) : t.card);
                if (m.anim > 0.02f) c.fill(x, y + 6, x + 2, y + hh - 6, Draw.alpha(t.accent, (int) (255 * m.anim)));
                String arrow = m.settings.isEmpty() ? " " : (m.expanded ? "v" : ">");
                Draw.text(c, arrow, x + 7, y + 10, Draw.SUB);
                Draw.text(c, m.name, x + 17, y + 5, Draw.lerp(0xFFD0D0DC, t.accent, m.anim));

                int swx = x + w - 30;
                String bl = listening == m ? "..." : Binds.keyName(m.bind) + (m.bind >= 0 ? (m.hold ? " H" : " T") : "");
                int bw = Draw.w(bl) + 10;
                int bx = swx - 8 - bw;
                Draw.text(c, trim(m.desc, bx - x - 24), x + 17, y + 16, Draw.SUB);

                Draw.sw(c, swx, y + 9, m.anim, t.accent);
                Draw.rrect(c, bx, y + 7, bw, 14, 3, listening == m ? Draw.alpha(t.accent, 0x70) : 0xFF23232F);
                Draw.text(c, bl, bx + 5, y + 10, 0xFFE6E6F0);

                if (!m.settings.isEmpty()) hit(x, y, bx - x - 4, hh, b -> m.expanded = !m.expanded);
                hit(swx - 2, y + 4, 26, 20, b -> m.enabled = !m.enabled);
                hit(bx, y + 7, bw, 14, b -> {
                    if (b == 1) m.hold = !m.hold;
                    else { listening = m; editing = null; }
                });

                int ry = y + hh;
                if (sh > 0) {
                    for (Setting s : m.settings) {
                        setting(c, s, x, ry, w, t);
                        ry += 20;
                    }
                    button(c, x + w - 12 - 84, ry + 2, 84, 14, "Reset settings", 0xFFFF4D6D);
                    hit(x + w - 12 - 84, ry + 2, 84, 14, b -> { for (Setting st : m.settings) st.reset(); });
                }
            }
            y += total + 4;
        }
        return y;
    }

    private void setting(DrawContext c, Setting s, int x, int ry, int w, Theme t) {
        Draw.text(c, s.name, x + 17, ry + 5, 0xFFB4B4C4);
        int right = x + w - 12;
        if (s.type == Setting.BOOL) {
            s.anim += ((s.b() ? 1f : 0f) - s.anim) * 0.3f;
            Draw.sw(c, right - 22, ry + 4, s.anim, t.accent);
            hit(right - 26, ry + 1, 30, 18, b -> s.set(s.b() ? 0 : 1));
        } else if (s.type == Setting.NUM) {
            int tw = 100, tx = right - tw, ty = ry + 8;
            double f = (s.value - s.min) / (s.max - s.min);
            int fw = (int) (tw * f);
            Draw.rrect(c, tx, ty, tw, 4, 2, 0xFF2A2A36);
            Draw.rrect(c, tx, ty, Math.max(2, fw), 4, 2, t.accent);
            Draw.rrect(c, tx + fw - 3, ty - 2, 6, 8, 3, 0xFFFFFFFF);
            Draw.textR(c, s.display(), tx - 8, ry + 5, Draw.TEXT);
            final int ftx = tx;
            hitDrag(tx - 4, ry + 1, tw + 8, 18, b -> {}, mxv -> s.setFraction((mxv - ftx) / (double) tw));
        } else {
            String o = s.display();
            int bw = Draw.w(o) + 12, bx = right - bw;
            Draw.rrect(c, bx, ry + 3, bw, 14, 3, 0xFF23232F);
            Draw.textC(c, o, bx + bw / 2, ry + 6, t.accent);
            hit(bx, ry + 3, bw, 14, b -> s.cycle(b == 1 ? -1 : 1));
        }
    }

    // ---------------- binds tab ----------------
    private int section(DrawContext c, String title, String sub, int x, int y, int w) {
        Theme t = Theme.cur();
        Draw.text(c, title, x + 2, y + 2, t.accent);
        Draw.text(c, trim(sub, w - 4), x + 2, y + 13, Draw.SUB);
        return y + 26;
    }

    private int binds(DrawContext c, int x, int y, int w) {
        y = section(c, "Command Binds", "Key sends the command. Blocked during PvP if PvP Safe is on.", x, y, w);
        for (Binds.Action a : Binds.COMMANDS) y = actionRow(c, a, x, y, w);
        y += 8;
        y = section(c, "Fast Swap Items", "Item name or id part (trap, disorient...). Key swaps, uses, swaps back.", x, y, w);
        for (Binds.Action a : Binds.SWAPS) y = actionRow(c, a, x, y, w);
        Draw.text(c, "Fast Swap skips items that are on cooldown.", x + 2, y + 6, Draw.SUB);
        return y + 20;
    }

    private int actionRow(DrawContext c, Binds.Action a, int x, int y, int w) {
        Theme t = Theme.cur();
        int h = 22;
        if (y + h >= visTop && y <= visBottom) {
            Draw.rrect(c, x, y, w, h, 4, t.card);
            Draw.text(c, a.label, x + 8, y + 7, Draw.SUB);
            int fx = x + 76, fw = w - 76 - 96;
            boolean ed = editing == a;
            Draw.rrect(c, fx, y + 4, fw, 14, 3, ed ? Draw.alpha(t.accent, 0x60) : 0xFF23232F);
            String txt = a.text + (ed && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            Draw.text(c, trim(txt, fw - 8), fx + 4, y + 7, 0xFFE6E6F0);
            int bx = x + w - 88;
            boolean ls = listening == a;
            Draw.rrect(c, bx, y + 4, 80, 14, 3, ls ? Draw.alpha(t.accent, 0x70) : 0xFF23232F);
            Draw.textC(c, ls ? "press key..." : Binds.keyName(a.bind), bx + 40, y + 7, ls ? 0xFFFFFFFF : t.accent);
            hit(fx, y + 4, fw, 14, b -> { editing = a; listening = null; });
            hit(bx, y + 4, 80, 14, b -> {
                if (b == 1) a.bind = -1;
                else { listening = a; editing = null; }
            });
        }
        return y + h + 3;
    }

    // ---------------- themes ----------------
    private int themes(DrawContext c, int x, int y, int w) {
        Theme cur = Theme.cur();
        int cw2 = (w - 8) / 2, h = 44;
        for (int i = 0; i < Theme.ALL.length; i++) {
            Theme th = Theme.ALL[i];
            int col = i % 2, row = i / 2;
            int tx = x + col * (cw2 + 8), ty = y + row * (h + 6);
            if (ty + h >= visTop && ty <= visBottom) {
                boolean sel = Theme.index == i;
                if (sel) Draw.rrect(c, tx - 1, ty - 1, cw2 + 2, h + 2, 6, th.accent);
                Draw.rrect(c, tx, ty, cw2, h, 5, th.card);
                Draw.rrect(c, tx + 8, ty + 8, 16, 16, 4, th.accent);
                Draw.rrect(c, tx + 28, ty + 8, 16, 16, 4, th.accent2);
                Draw.rrect(c, tx + 48, ty + 8, 16, 16, 4, th.panel);
                Draw.hgrad(c, tx + 8, ty + 30, cw2 - 16, 3, th.accent, th.accent2);
                Draw.text(c, th.name, tx + 72, ty + 12, sel ? 0xFFFFFFFF : 0xFFC0C0CC);
                final int idx = i;
                hit(tx, ty, cw2, h, b -> { Theme.index = idx; msg("Theme: " + Theme.ALL[idx].name); });
            }
        }
        int rows = (Theme.ALL.length + 1) / 2;
        int end = y + rows * (h + 6);
        Draw.text(c, "Theme colors apply to the menu and the HUD.", x + 2, end + 4, Draw.SUB);

        Theme t = Theme.cur();
        int y2 = section(c, "Custom color", "Select the Custom theme above, then pick your color.", x, end + 18, w);
        y2 = settingsCard(c, GuiStyle.COLOR, x, y2, w, t);
        y2 += 6;
        y2 = section(c, "Menu fog", "Soft moving haze inside the window.", x, y2, w);
        y2 = settingsCard(c, GuiStyle.FOGS, x, y2, w, t);
        button(c, x + w - 120, y2 + 4, 120, 14, "Reset color & fog", 0xFFFF4D6D);
        hit(x + w - 120, y2 + 4, 120, 14, b -> { GuiStyle.reset(); msg("Color & fog reset"); });
        return y2 + 26;
    }

    private int settingsCard(DrawContext c, Setting[] list, int x, int y, int w, Theme t) {
        int ch = list.length * 20 + 6;
        if (y + ch >= visTop && y <= visBottom) {
            Draw.rrect(c, x, y, w, ch, 5, t.card);
            for (int i = 0; i < list.length; i++) setting(c, list[i], x, y + 3 + i * 20, w, t);
        }
        return y + ch;
    }

    // ---------------- scripts ----------------
    private int scripts(DrawContext c, int x, int y, int w) {
        Theme t = Theme.cur();
        Draw.text(c, trim("Drop .lua files into config/ezvisuals/scripts (auto reload). API.md is in that folder.", w - 4), x + 2, y + 2, Draw.SUB);
        y += 16;
        if (y + 16 >= visTop && y <= visBottom) {
            button(c, x, y, 80, 14, "Reload all", t.accent);
            hit(x, y, 80, 14, b -> { Scripts.scan(); Scripts.reloadAll(); msg("Scripts reloaded"); });
        }
        y += 22;
        if (Scripts.LIST.isEmpty()) {
            Draw.text(c, "No scripts yet. Ask Claude to write one and drop the .lua file here.", x + 2, y + 4, Draw.SUB);
            y += 20;
        }
        for (Scripts.Script sc : Scripts.LIST) {
            int h = sc.failed ? 40 : 28;
            sc.anim += ((sc.enabled ? 1f : 0f) - sc.anim) * 0.3f;
            if (y + h >= visTop && y <= visBottom) {
                Draw.rrect(c, x, y, w, h, 5, t.card);
                if (sc.anim > 0.02f) c.fill(x, y + 6, x + 2, y + 22, Draw.alpha(t.accent, (int) (255 * sc.anim)));
                Draw.text(c, sc.name, x + 10, y + 5, Draw.lerp(0xFFD0D0DC, t.accent, sc.anim));
                String st = sc.failed ? "error" : sc.enabled ? "running" : "disabled";
                int stc = sc.failed ? 0xFFFF5555 : sc.enabled ? 0xFF5CFF7A : 0xFF666677;
                Draw.text(c, st, x + 10, y + 16, stc);
                int swx = x + w - 30;
                Draw.sw(c, swx, y + 9, sc.anim, t.accent);
                button(c, swx - 8 - 46, y + 7, 46, 14, "Reload", t.accent2);
                hit(swx - 2, y + 4, 26, 20, b -> Scripts.setEnabled(sc, !sc.enabled));
                hit(swx - 8 - 46, y + 7, 46, 14, b -> { Scripts.reload(sc); msg("Reloaded " + sc.name); });
                if (sc.failed) Draw.text(c, trim(sc.error, w - 20), x + 10, y + 29, 0xFFFF7777);
            }
            y += h + 4;
        }
        y += 6;
        Draw.text(c, "Log", x + 2, y, t.accent);
        y += 12;
        int lines = Math.min(8, Scripts.LOG.size());
        int lh = 12 + Math.max(1, lines) * 10;
        if (y + lh >= visTop && y <= visBottom) {
            Draw.rrect(c, x, y, w, lh, 5, t.card);
            if (lines == 0) Draw.text(c, "(empty)", x + 8, y + 6, 0xFF55556A);
            int i = 0, skip = Scripts.LOG.size() - lines;
            for (String line : Scripts.LOG) {
                if (i++ < skip) continue;
                Draw.text(c, trim(line, w - 16), x + 8, y + 6 + (i - 1 - skip) * 10, 0xFFC8C8D4);
            }
        }
        return y + lh + 8;
    }

    private void button(DrawContext c, int x, int y, int w, int h, String label, int col) {
        boolean hov = over(x, y, w, h);
        Draw.rrect(c, x, y, w, h, 3, hov ? Draw.lerp(0xFF23232F, col, 0.6f) : Draw.lerp(0xFF23232F, col, 0.25f));
        Draw.textC(c, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    // ---------------- pictures ----------------
    private int pictures(DrawContext c, int x, int y, int w, int ch) {
        Theme t = Theme.cur();
        Draw.text(c, "Custom Pictures", x + 2, y + 2, t.accent);
        Draw.text(c, "Put 1.png .. 4.png into config/ezvisuals/pictures", x + 2, y + 13, Draw.SUB);
        button(c, x + w - 56, y + 4, 54, 14, "Reload", t.accent);
        hit(x + w - 56, y + 4, 54, 14, b -> { Pics.reload(); msg("Pictures reloaded"); });
        y += 28;
        int cw2 = (w - 8) / 2;
        int hh = Math.max(70, (ch - 36 - 8) / 2);
        for (int i = 0; i < Pics.N; i++) {
            int col = i % 2, row = i / 2;
            int px = x + col * (cw2 + 8), py = y + row * (hh + 8);
            if (py + hh >= visTop && py <= visBottom) {
                Draw.rrect(c, px, py, cw2, hh, 5, t.card);
                if (Pics.has(i)) {
                    Pics.draw(c, i, px + 6, py + 6, cw2 - 12, hh - 22);
                } else {
                    Draw.textC(c, "Drop " + (i + 1) + ".png here", px + cw2 / 2, py + hh / 2 - 8, 0xFF55556A);
                }
                Draw.text(c, "Picture " + (i + 1) + (Pics.has(i) ? "  " + Pics.W[i] + "x" + Pics.H[i] : ""),
                        px + 8, py + hh - 13, Draw.SUB);
            }
        }
        return y + 2 * (hh + 8);
    }

    // ---------------- input ----------------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                if (h.drag() != null) { dragging = h; h.drag().accept(mx); }
                if (h.click() != null) h.click().accept(button);
                return true;
            }
        }
        listening = null;
        editing = null;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && dragging.drag() != null) {
            dragging.drag().accept(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll[tab] -= (float) (vAmount * 18);
        scroll[tab] = Math.max(0, Math.min(maxScroll, scroll[tab]));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                int v = (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) ? -1 : key;
                if (listening instanceof Module m) m.bind = v;
                else if (listening instanceof Binds.Action a) a.bind = v;
            }
            listening = null;
            return true;
        }
        if (editing != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_KP_ENTER) {
                editing = null;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editing.text.isEmpty()) editing.text = editing.text.substring(0, editing.text.length() - 1);
            } else if (key == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                String clip = client.keyboard.getClipboard();
                if (clip != null && editing.text.length() + clip.length() <= 40) editing.text += clip;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            int d = (mods & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
            tab = (tab + d + TABS.length) % TABS.length;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || EzVisualsPremium.menuKey.matchesKey(key, scan)) {
            close();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (editing != null) {
            if (chr >= 32 && chr != 127 && editing.text.length() < 40) editing.text += chr;
            return true;
        }
        return super.charTyped(chr, mods);
    }
}
