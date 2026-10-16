package BattleSystem;

import Engine.GraphicsHandler;
import Engine.ScreenManager;
import SpriteFont.MonoSpriteFont;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Shared look of the battle HUD, modeled on the Pokemon HeartGold/SoulSilver battle screen:
//   - light rounded text box with a dark blue frame (bottom left)
//   - four-ish colored rounded buttons (bottom right)
//   - grey slanted name plates with an HP bar (enemy top left, player right of center)
// All text goes through MonoSpriteFont (the VCR OSD sprite font), always in capitals.
public final class BattleUI {
    private BattleUI() {}
    // light box colors
    public static final Color INK = new Color(40, 40, 58);          // dark text on light boxes
    public static final Color INK_SOFT = new Color(104, 108, 128);  // secondary text on light boxes
    public static final Color BOX_FILL = new Color(244, 244, 248);
    public static final Color BOX_FRAME = new Color(52, 64, 100);
    public static final Color BOX_LINE = new Color(150, 168, 206);
    public static final Color PLATE = new Color(222, 224, 230);
    public static final Color PLATE_EDGE = new Color(62, 66, 84);

    // text on dark / colored backgrounds
    public static final Color WHITE = new Color(250, 250, 250);
    public static final Color SHADOW = new Color(24, 24, 36);
    public static final Color GOLD = new Color(255, 210, 58);
    public static final Color GREY = new Color(135, 135, 150);
    public static final Color DARK_GREY = new Color(150, 150, 165);

    // bars
    public static final Color GREEN = new Color(64, 200, 96);
    public static final Color YELLOW = new Color(240, 200, 40);
    public static final Color RED = new Color(222, 64, 56);
    public static final Color BLUE = new Color(70, 140, 255);
    public static final Color HP_TAG = new Color(232, 150, 40);

    // button colors
    public static final Color BTN_MELEE = new Color(214, 78, 88);
    public static final Color BTN_SHIELD = new Color(70, 120, 200);
    public static final Color BTN_RANGED = new Color(70, 160, 90);
    public static final Color BTN_HEAL = new Color(222, 150, 50);
    public static final Color BTN_RUN = new Color(120, 96, 176);

    private static MonoSpriteFont font;

    public static MonoSpriteFont font() {
        if (font == null) font = new MonoSpriteFont();
        return font;
    }

    public static int W() { return ScreenManager.getScreenWidth(); }
    public static int H() { return ScreenManager.getScreenHeight(); }

    // the bottom bar (text box + buttons) sits here; everything above it is the arena
    public static int barTop() { return H() - 160; }
    public static int barHeight() { return 150; }
    public static int boxX() { return 8; }
    public static int boxW() { return 382; }
    public static int boxH() { return 96; }
    public static int btnX() { return 398; }
    public static int btnW() { return W() - 398 - 8; }

    // ------------------------------------------------------------------ text

    public static void text(GraphicsHandler gh, String s, int x, int capTopY, int scale, Color color) {
        font().draw(gh, s.toUpperCase(), x, capTopY, scale, 0, color);
    }

    public static void shadowText(GraphicsHandler gh, String s, int x, int capTopY, int scale, Color color) {
        text(gh, s, x + scale, capTopY + scale, scale, SHADOW);
        text(gh, s, x, capTopY, scale, color);
    }

    public static int textW(String s, int scale) {
        return font().getTextWidth(s.toUpperCase(), scale, 0);
    }

    public static void textRight(GraphicsHandler gh, String s, int xRight, int capTopY, int scale, Color color) {
        text(gh, s, xRight - textW(s, scale), capTopY, scale, color);
    }

    public static void textCenter(GraphicsHandler gh, String s, int xCenter, int capTopY, int scale, Color color) {
        text(gh, s, xCenter - textW(s, scale) / 2, capTopY, scale, color);
    }

    // ------------------------------------------------------------------ shapes

    private static Object smooth(Graphics2D g) {
        Object old = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return old;
    }

    private static void restore(Graphics2D g, Object old) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : old);
    }

    // the light rounded box with a dark frame (the "What will Knight do?" box and the popup windows)
    public static void box(GraphicsHandler gh, int x, int y, int w, int h) {
        Graphics2D g = gh.getGraphics();
        Object old = smooth(g);
        g.setColor(BOX_FRAME);
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(BOX_FILL);
        g.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 12, 12);
        g.setStroke(new BasicStroke(2));
        g.setColor(BOX_LINE);
        g.drawRoundRect(x + 7, y + 7, w - 15, h - 15, 8, 8);
        g.setStroke(new BasicStroke(1));
        restore(g, old);
    }

    // a slanted grey name plate. slantRight = true slants its right edge (enemy plate), false slants the left edge (player plate)
    public static void plate(GraphicsHandler gh, int x, int y, int w, int h, boolean slantRight) {
        Graphics2D g = gh.getGraphics();
        Object old = smooth(g);
        int s = 26;
        int[] xs = slantRight ? new int[]{x, x + w, x + w - s, x} : new int[]{x + s, x + w, x + w, x};
        int[] ys = slantRight ? new int[]{y, y, y + h, y + h} : new int[]{y, y, y + h, y + h};
        // dark outline first, then the lighter plate slightly inset
        g.setColor(PLATE_EDGE);
        g.fillPolygon(xs, ys, 4);
        g.setPaint(new GradientPaint(0, y, new Color(240, 241, 246), 0, y + h, PLATE));
        int[] xi = slantRight ? new int[]{x, x + w - 4, x + w - s - 3, x} : new int[]{x + s + 3, x + w, x + w, x + 4};
        int[] yi = {y + 4, y + 4, y + h - 4, y + h - 4};
        if (!slantRight) { xi = new int[]{x + s + 3, x + w, x + w, x + 4}; }
        g.fillPolygon(xi, yi, 4);
        g.setPaint(null);
        restore(g, old);
    }

    // a rounded colored button with a light top and a white inner line; selected ones get a gold ring
    public static void button(GraphicsHandler gh, int x, int y, int w, int h, Color base, boolean selected, boolean disabled) {
        Graphics2D g = gh.getGraphics();
        Object old = smooth(g);
        Color c = disabled ? new Color(130, 130, 140) : base;
        g.setColor(selected ? GOLD : new Color(30, 30, 44));
        g.fillRoundRect(x - (selected ? 3 : 0), y - (selected ? 3 : 0), w + (selected ? 6 : 0), h + (selected ? 6 : 0), 20, 20);
        g.setColor(new Color(30, 30, 44));
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setPaint(new GradientPaint(0, y, brighten(c, 1.35f), 0, y + h, c.darker()));
        g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 14, 14);
        g.setPaint(null);
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(255, 255, 255, 190));
        g.drawRoundRect(x + 5, y + 5, w - 11, h - 11, 11, 11);
        g.setStroke(new BasicStroke(1));
        restore(g, old);
    }

    private static Color brighten(Color c, float f) {
        return new Color(Math.min(255, (int) (c.getRed() * f)), Math.min(255, (int) (c.getGreen() * f)), Math.min(255, (int) (c.getBlue() * f)));
    }

    // HP / XP bar: dark trough, colored fill, light border
    public static void bar(GraphicsHandler gh, int x, int y, int w, int h, float frac, Color fill) {
        frac = Math.max(0f, Math.min(1f, frac));
        gh.drawFilledRectangle(x, y, w, h, new Color(36, 38, 52));
        int fw = Math.round((w - 4) * frac);
        if (fw > 0) {
            gh.drawFilledRectangle(x + 2, y + 2, fw, h - 4, fill);
            gh.drawFilledRectangle(x + 2, y + 2, fw, Math.max(1, (h - 4) / 3), brighten(fill, 1.35f));   // little highlight
        }
        gh.drawRectangle(x, y, w - 1, h - 1, new Color(250, 250, 250), 2);
    }

    // the orange "HP" tag in front of an HP bar
    public static void hpTag(GraphicsHandler gh, int x, int y) {
        Graphics2D g = gh.getGraphics();
        Object old = smooth(g);
        g.setColor(SHADOW);
        g.fillRoundRect(x, y, 30, 16, 8, 8);
        g.setColor(HP_TAG);
        g.fillRoundRect(x + 1, y + 1, 28, 14, 7, 7);
        restore(g, old);
        text(gh, "HP", x + 8, y + 4, 1, WHITE);
    }

    public static Color hpColor(float frac) {
        if (frac > 0.5f) return GREEN;
        if (frac > 0.2f) return YELLOW;
        return RED;
    }

    // a right-pointing triangle used as the menu cursor
    public static void arrow(GraphicsHandler gh, int x, int y, int size, Color color) {
        Graphics2D g = gh.getGraphics();
        g.setColor(color);
        g.fillPolygon(new int[]{x, x, x + size}, new int[]{y, y + size * 2, y + size}, 3);
    }

    // splits text into lines of at most maxChars characters, breaking on spaces
    public static List<String> wrap(String s, int maxChars) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > maxChars) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    // item names in the code are written like "WraithboneSaber"; this turns them into "Wraithbone Saber"
    // (same rule InventoryScreen already uses)
    public static String prettyName(String itemName) {
        return itemName.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }
}