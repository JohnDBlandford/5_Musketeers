package Story;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.ScreenManager;
import NPCs.World4.GlitchEntity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

// STORY: the conversation box. A dark panel across the bottom of the screen with the speaker's portrait (the same pixel
// sprite, enlarged), a name plate in their faction's colour, and the text typing itself out. ENTER finishes the line or goes
// to the next one. When a line ends in choices they appear in a box above it: UP / DOWN (or W / S) to pick, ENTER to choose.
// A GLITCH line flickers: one frame of static over the screen, a static border, and letters that break apart.
public final class DialogueBox {
    private final Deque<Dialogue.Line> queue = new ArrayDeque<>();
    private Dialogue dialogue;
    private Dialogue.Line line;
    private float shown;          // characters typed so far
    private int cursor, tick, lineTick;
    private boolean open;
    private final KeyLocker keys = new KeyLocker();
    private static final Key[] KEYS = { Key.ENTER, Key.SPACE, Key.UP, Key.DOWN, Key.W, Key.S };

    private static final Color PANEL = new Color(16, 12, 14, 236), FRAME = new Color(214, 178, 110), IRON = new Color(60, 52, 48);
    private static final Color TEXT = new Color(236, 228, 210), DIM = new Color(150, 140, 125);

    public boolean isOpen() { return open; }

    public void open(Dialogue d) {
        dialogue = d;
        queue.clear();
        queue.addAll(d.lines);
        open = true;
        for (Key k : KEYS) keys.lockKey(k);   // the ENTER that started the talk must not skip the first line
        next();
    }

    // returns true while it is still open
    public boolean update() {
        if (!open) return false;
        tick++;
        lineTick++;
        if (line == null) { close(); return false; }
        int len = line.text.length();
        if (shown < len) shown = Math.min(len, shown + 1.6f);
        boolean typed = shown >= len;
        if (typed && !line.choices.isEmpty()) {
            if (tap(Key.UP) | tap(Key.W)) cursor = (cursor + line.choices.size() - 1) % line.choices.size();
            if (tap(Key.DOWN) | tap(Key.S)) cursor = (cursor + 1) % line.choices.size();
        } else { tap(Key.UP); tap(Key.W); tap(Key.DOWN); tap(Key.S); }
        if (tap(Key.ENTER) | tap(Key.SPACE)) {
            if (!typed) shown = len;
            else if (!line.choices.isEmpty()) {
                Dialogue.Choice c = line.choices.get(cursor);
                if (c.effect != null) c.effect.run();
                List<Dialogue.Line> then = new ArrayList<>(c.then);
                for (int i = then.size() - 1; i >= 0; i--) queue.addFirst(then.get(i));
                next();
            } else next();
        }
        return open;
    }

    private void next() {
        line = null;
        while (!queue.isEmpty()) {
            Dialogue.Line l = queue.pollFirst();
            if (l.onShow != null) l.onShow.run();
            if (l.text != null) { line = l; break; }   // a text-less line is only an action
        }
        shown = 0;
        cursor = 0;
        lineTick = 0;
        if (line == null) close();
    }

    private void close() {
        open = false;
        line = null;
        Dialogue d = dialogue;
        dialogue = null;
        if (d != null && d.onEnd != null) d.onEnd.run();
    }

    private boolean tap(Key k) {
        if (Keyboard.isKeyUp(k)) keys.unlockKey(k);
        if (Keyboard.isKeyDown(k) && !keys.isKeyLocked(k)) { keys.lockKey(k); return true; }
        return false;
    }

    // ---------------------------------------------------------------- drawing

    public void draw(GraphicsHandler gh) {
        if (!open || line == null) return;
        int W = ScreenManager.getScreenWidth(), H = ScreenManager.getScreenHeight();
        int x = 14, h = 150, y = H - h - 12, w = W - 28;
        Graphics2D g = gh.getGraphics();

        // a glitch line: one frame of static across the whole screen when it appears
        if (line.glitch && lineTick < 3) staticField(gh, 0, 0, W, H, 6, 200, 7);

        gh.drawFilledRectangle(x, y, w, h, PANEL);
        if (line.glitch) staticField(gh, x - 4, y - 4, w + 8, h + 8, 4, 255, 3);
        gh.drawRectangle(x, y, w, h, FRAME, 2);
        gh.drawRectangle(x + 5, y + 5, w - 10, h - 10, new Color(110, 18, 18), 1);

        // portrait
        int tx = x + 22;
        if (line.look != null) {
            int ps = 116;
            gh.drawFilledRectangle(x + 14, y + 17, ps, ps, new Color(30, 24, 26));
            gh.drawRectangle(x + 14, y + 17, ps, ps, IRON, 2);
            g.setColor(new Color(120, 20, 20, 40 + (int) (20 * Math.sin(tick * 0.05))));
            g.fillOval(x + 30, y + 30, ps - 32, ps - 32);
            BufferedImage img = NPCArt.frames(line.look)[(tick / 40) % 2];
            int sc = Math.max(1, Math.min((ps - 12) / img.getWidth(), (ps - 12) / img.getHeight()));
            int iw = img.getWidth() * sc, ih = img.getHeight() * sc;
            Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, x + 14 + (ps - iw) / 2, y + 17 + ps - ih - 6, iw, ih, null);
            if (hint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            tx = x + 14 + ps + 16;
        }

        // name plate
        if (line.speaker != null && !line.speaker.isEmpty()) {
            int nw = BattleUI.textW(line.speaker, 2) + 24;
            gh.drawFilledRectangle(x + 14, y - 18, nw, 26, new Color(24, 18, 20));
            gh.drawRectangle(x + 14, y - 18, nw, 26, speakerColor(line.speaker), 2);
            BattleUI.text(gh, line.speaker, x + 26, y - 10, 2, speakerColor(line.speaker));
        }

        // the text, typed out
        int maxChars = Math.max(10, (x + w - 20 - tx) / 12);
        String visible = line.text.substring(0, Math.min(line.text.length(), (int) shown));
        List<String> lines = BattleUI.wrap(line.text, maxChars);
        int left = visible.length(), ly = y + 22;
        Random r = new Random(tick / 4 * 17L);
        for (int i = 0; i < lines.size() && i < 5; i++) {
            String full = lines.get(i);
            String part = full.substring(0, Math.max(0, Math.min(full.length(), left)));
            left -= full.length() + 1;
            if (line.glitch) {
                part = GlitchEntity.corrupt(part, tick + i * 7, 0.08);
                if (r.nextInt(5) == 0) BattleUI.text(gh, part, tx + 2, ly, 2, new Color(200, 20, 40, 170));
            }
            BattleUI.text(gh, part, tx, ly, 2, line.speaker == null || line.speaker.isEmpty() ? new Color(196, 186, 168) : TEXT);
            ly += 24;
            if (left <= 0) break;
        }
        boolean typed = shown >= line.text.length();
        if (typed && line.choices.isEmpty() && (tick / 20) % 2 == 0) BattleUI.arrow(gh, x + w - 30, y + h - 30, 7, FRAME);

        // the choices, in a box above the panel
        if (typed && !line.choices.isEmpty()) {
            int cw = 0;
            for (Dialogue.Choice c : line.choices) cw = Math.max(cw, BattleUI.textW(c.label, 2));
            cw += 64;
            int ch = line.choices.size() * 30 + 22, cx = x + w - cw, cy = y - ch - 30;
            gh.drawFilledRectangle(cx, cy, cw, ch, PANEL);
            gh.drawRectangle(cx, cy, cw, ch, FRAME, 2);
            for (int i = 0; i < line.choices.size(); i++) {
                int oy = cy + 14 + i * 30;
                boolean on = i == cursor;
                if (on) {
                    gh.drawFilledRectangle(cx + 8, oy - 6, cw - 16, 26, new Color(70, 52, 24));
                    BattleUI.arrow(gh, cx + 16, oy, 6, BattleUI.GOLD);
                }
                BattleUI.text(gh, line.choices.get(i).label, cx + 36, oy, 2, on ? BattleUI.GOLD : DIM);
            }
        }
    }

    // faction colours for the name plate
    private static Color speakerColor(String s) {
        switch (s) {
            case "CAPTAIN VALE": return new Color(220, 210, 180);
            case "BROTHER ANSEL": case "SISTER MIREL": return new Color(220, 70, 70);
            case "OLD MAUD": case "ELSA": return new Color(130, 200, 120);
            case "THE FAMINE WARDEN": return new Color(200, 160, 80);
            case "KNIGHT": return new Color(170, 190, 230);
            default: return FRAME;
        }
    }

    private static final Color[] STATIC = { new Color(10, 6, 16), new Color(42, 15, 61), new Color(90, 30, 138), new Color(138, 16, 32), new Color(220, 210, 230) };

    // a rectangle of live static (only its border when it is the box)
    private void staticField(GraphicsHandler gh, int x, int y, int w, int h, int cell, int alpha, int border) {
        Random r = new Random(tick * 31L);
        boolean full = border > 6;
        for (int yy = 0; yy < h; yy += cell)
            for (int xx = 0; xx < w; xx += cell) {
                boolean edge = xx < border * cell || yy < border * cell || xx >= w - border * cell || yy >= h - border * cell;
                if (!full && !edge) continue;
                if (full && r.nextInt(3) == 0) continue;
                Color c = STATIC[r.nextInt(STATIC.length)];
                gh.drawFilledRectangle(x + xx, y + yy, cell, cell, new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            }
    }
}