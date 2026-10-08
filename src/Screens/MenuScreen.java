package Screens;

import Engine.*;
import Game.GameState;
import Game.ScreenCoordinator;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import SpriteFont.MonoSpriteFont;
import java.util.Random;

// Title screen styled after Faith: The Unholy Trinity, built to make the player feel dread.
// Behind the UI, the three forms of Dabbah fade in and out at very low opacity:
//   1. a pitch black dragon
//   2. the same dragon engulfed in flames, with a dark aura that traces its outline
//   3. unbound ruin: a hydra whose heads each fire a different dark beam
// Dread layers: slow heartbeat pulse, creeping vignette, drifting ash, eyes that open in the dark (it is watching), whispers, and flicker.
// Everything is drawn in code except the font, which is a sprite sheet: put VCROSDMono.png in your Resources folder (next to Cat.png, etc.)
public class MenuScreen extends Screen {

    // ------------------------------------------------------------------ tweak these
    private static final String TITLE_TOP = "WHEN";
    private static final String TITLE = "DABBAH";
    private static final String TITLE_BOTTOM = "RISES";
    private static final String FOOTER = "V0.910  (C)2026 THE 5 MUSKETEERS";
    private static final String[] ITEMS = { "BEGIN", "CREDITS", "ABITUS" }; // 0 = start game, 1 = credits, 2 = exit
    private static final float BG_MAX_ALPHA = 0.22f;               // how visible Dabbah is (keep low)
    private static final float[] FORM_ALPHA = { 1.0f, 1.0f, 1.4f }; // dark beams need a little extra to be seen
    private static final float BEAM_ALPHA = 0.7f;                 // beams get their own, much stronger layer so they can be seen
    private static final double FIRE_THICKNESS = 5.5;             // how thick the fire outline is around the dragon (beams are 5.5-10 wide)
    private static final int PHASE_FRAMES = 600;                   // how long each form is shown (60 fps -> 10 s)
    private static final int FADE_FRAMES = 150;                    // cross-fade length
    private static final int LOWRES_SCALE = 4;                     // bigger = chunkier background pixels

    private static final Color RED = new Color(255, 0, 0);
    private static final Color DARK_RED = new Color(110, 0, 0);
    private static final Color YELLOW = new Color(255, 230, 0);
    private static final Color SCANLINE = new Color(0, 0, 0, 70);

    // ------------------------------------------------------------------ state
    protected ScreenCoordinator screenCoordinator;
    protected int selected = 0;
    protected KeyLocker keyLocker = new KeyLocker();
    private int frame = 0;

    private int lw, lh;                       // low-res layer size
    private BufferedImage[] layers = new BufferedImage[4];
    private Graphics2D[] layerG = new Graphics2D[4];
    private double grow = 0;                  // used to inflate the dragon silhouette for its aura

    private MonoSpriteFont font;          // VCR OSD Mono as a sprite sheet (Resources/VCROSDMono.png)
    private int titleScale, subScale, menuScale, smallScale; // whole-number pixel scales for each text size
    private int layoutW = -1;

    public MenuScreen(ScreenCoordinator screenCoordinator) {
        this.screenCoordinator = screenCoordinator;
    }

    @Override
    public void initialize() {
        selected = 0;
        frame = 0;
        layoutW = -1;
        font = new MonoSpriteFont();
        lw = Math.max(16, ScreenManager.getScreenWidth() / LOWRES_SCALE);
        lh = Math.max(16, ScreenManager.getScreenHeight() / LOWRES_SCALE);
        for (int i = 0; i < 4; i++) {
            layers[i] = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
            layerG[i] = layers[i].createGraphics();
        }
        // lock keys so a key held from the previous screen does not instantly trigger something
        keyLocker.lockKey(Key.SPACE);
        keyLocker.lockKey(Key.ENTER);
        keyLocker.lockKey(Key.UP);
        keyLocker.lockKey(Key.DOWN);
    }

    // ------------------------------------------------------------------ update
    // true once per key press (not while held)
    private boolean tap(Key key) {
        if (Keyboard.isKeyDown(key) && !keyLocker.isKeyLocked(key)) {
            keyLocker.lockKey(key);
            return true;
        }
        if (Keyboard.isKeyUp(key)) {
            keyLocker.unlockKey(key);
        }
        return false;
    }

    @Override
    public void update() {
        frame++;
        boolean up = tap(Key.UP) | tap(Key.W);
        boolean down = tap(Key.DOWN) | tap(Key.S);
        boolean confirm = tap(Key.SPACE) | tap(Key.ENTER);

        if (up) selected = (selected + ITEMS.length - 1) % ITEMS.length;
        if (down) selected = (selected + 1) % ITEMS.length;

        if (confirm) {
            switch (selected) {
                case 0: screenCoordinator.setGameState(GameState.LEVEL); break;
                case 1: screenCoordinator.setGameState(GameState.CREDITS); break;
                case 2: System.exit(0); break;
            }
        }
    }

    // ------------------------------------------------------------------ draw
    @Override
    public void draw(GraphicsHandler gh) {
        int w = ScreenManager.getScreenWidth();
        int h = ScreenManager.getScreenHeight();
        setupLayout(w, h);
        double beat = heartbeat();

        gh.drawFilledRectangle(0, 0, w, h, Color.BLACK);
        drawBackground(gh, w, h, beat);
        drawAsh(gh, w, h);
        drawVignette(gh.getGraphics(), w, h, beat);
        drawEyes(gh, w, h);

        drawTitle(gh, w, h);
        drawMenu(gh, w, h);
        font.draw(gh, FOOTER, (w - font.getTextWidth(FOOTER, smallScale, smallScale)) / 2, h - 14 - font.getCapHeight(smallScale), smallScale, smallScale, new Color(105, 105, 105));

        Random fr = new Random(frame / 3); // random brownouts, like a failing screen
        if (fr.nextInt(100) > 93) gh.drawFilledRectangle(0, 0, w, h, new Color(0, 0, 0, 20 + fr.nextInt(40)));
        for (int y = 0; y < h; y += 3) {
            gh.drawFilledRectangle(0, y, w, 1, SCANLINE);
        }
    }

    // picks whole-number pixel scales relative to the window so the layout holds at any size
    private void setupLayout(int w, int h) {
        if (layoutW == w) return;
        layoutW = w;
        titleScale = 1;
        while (font.getTextWidth(TITLE, titleScale + 1, titleScale + 1) <= w * 0.62) titleScale++; // biggest title that fits
        subScale = Math.max(1, titleScale / 3);
        menuScale = Math.max(2, Math.round(h * 0.055f * 0.7f / 7f));
        smallScale = Math.max(1, h / 280);
    }

    // "WHEN" small above, "DABBAH" huge, "RISES" small below
    private void drawTitle(GraphicsHandler gh, int w, int h) {
        int whenY = (int) (h * 0.06);
        int mainY = whenY + font.getCapHeight(subScale) + subScale * 4;
        int risesY = mainY + font.getCapHeight(titleScale) + subScale * 4;

        Random fr = new Random(frame / 4);
        int x = (w - font.getTextWidth(TITLE, titleScale, titleScale)) / 2;
        if (frame % 300 < 3) x += (frame % 2 == 0) ? 6 : -6; // tiny glitch every 5 seconds
        Color main = fr.nextInt(100) > 95 ? DARK_RED : RED;   // the title gutters like a dying light
        int sh = Math.max(2, titleScale / 3);
        font.draw(gh, TITLE, x + sh, mainY + sh, titleScale, titleScale, DARK_RED);
        font.draw(gh, TITLE, x, mainY, titleScale, titleScale, main);

        int tracking = subScale * 2, ssh = Math.max(1, subScale / 2);
        int tx = (w - font.getTextWidth(TITLE_TOP, subScale, tracking)) / 2;
        font.draw(gh, TITLE_TOP, tx + ssh, whenY + ssh, subScale, tracking, DARK_RED);
        font.draw(gh, TITLE_TOP, tx, whenY, subScale, tracking, RED);
        int bx = (w - font.getTextWidth(TITLE_BOTTOM, subScale, tracking)) / 2;
        font.draw(gh, TITLE_BOTTOM, bx + ssh, risesY + ssh, subScale, tracking, DARK_RED);
        font.draw(gh, TITLE_BOTTOM, bx, risesY, subScale, tracking, RED);
    }

    private void drawMenu(GraphicsHandler gh, int w, int h) {
        int tracking = menuScale * 2;
        int lineH = (int) (font.getCapHeight(menuScale) * 2.6);
        int blockW = 0;
        for (String s : ITEMS) blockW = Math.max(blockW, font.getTextWidth(s, menuScale, tracking));
        int x0 = (w - blockW) / 2 + 28;
        int y0 = (int) (h * 0.55);
        for (int i = 0; i < ITEMS.length; i++) {
            int y = y0 + i * lineH;
            boolean on = i == selected;
            int shake = (on && frame % 47 < 2) ? (frame % 2 == 0 ? 1 : -1) : 0; // the selected word trembles now and then
            font.draw(gh, ITEMS[i], x0 + shake, y, menuScale, tracking, on ? Color.WHITE : new Color(150, 150, 150));
            if (on) {
                int px = Math.max(2, menuScale * 7 / 5);
                drawCursor(gh, x0 - px * 7 - 16, y + (font.getCapHeight(menuScale) - 5 * px) / 2, px);
            }
        }
    }

    // cursor: Dabbah's slit-pupiled eye, watching your choice (it blinks now and then). Swap the pattern to change the symbol.
    private static final String[] CURSOR = { "..###..", ".##.##.", "###.###", ".##.##.", "..###.." };
    private static final String[] CURSOR_BLINK = { ".......", ".......", "#######", ".......", "......." };

    private void drawCursor(GraphicsHandler gh, int x, int y, int px) {
        String[] pat = frame % 220 < 6 ? CURSOR_BLINK : CURSOR;
        for (int r = 0; r < pat.length; r++)
            for (int c = 0; c < pat[r].length(); c++)
                if (pat[r].charAt(c) == '#') gh.drawFilledRectangle(x + c * px, y + r * px, px, px, YELLOW);
    }

    // ------------------------------------------------------------------ dread
    // slow double-beat: a "lub-dub" every ~1.8 seconds, 0..1
    private double heartbeat() {
        double t = frame % 110;
        return Math.max(Math.exp(-Math.pow(t / 5.0, 2)), 0.7 * Math.exp(-Math.pow((t - 16) / 5.0, 2)));
    }

    // darkness creeping in from the edges; it flinches back on each heartbeat
    private void drawVignette(Graphics2D g, int w, int h, double beat) {
        float r = (float) (Math.max(w, h) * 0.75 * (1 - 0.03 * beat));
        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.46f), r, new float[] { 0f, 0.5f, 1f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 70), new Color(0, 0, 0, 240) });
        Paint old = g.getPaint();
        g.setPaint(p);
        g.fillRect(0, 0, w, h);
        g.setPaint(old);
    }

    // dim ash drifting slowly down through the dark
    private void drawAsh(GraphicsHandler gh, int w, int h) {
        for (int i = 0; i < 45; i++) {
            float x = (i * 97 + frame * (0.10f + (i % 3) * 0.06f)) % w;
            float y = (i * 53 + frame * (0.25f + (i % 4) * 0.08f)) % h;
            gh.drawFilledRectangle((int) x, (int) y, 2, 2, new Color(150, 150, 150, 25 + (i % 5) * 10));
        }
    }

    // pairs of red eyes open in the dark at the edges of the screen, watch, blink, and close
    private static final double[][] EYE_POS = { { .07, .30 }, { .93, .38 }, { .12, .80 }, { .88, .76 }, { .50, .93 } };

    private void drawEyes(GraphicsHandler gh, int w, int h) {
        for (int i = 0; i < EYE_POS.length; i++) {
            int period = 500 + i * 113, len = 170, local = (frame + i * 211) % period;
            if (local >= len || local % 67 < 5) continue; // not open yet, or mid-blink
            float a = (float) Math.sin(Math.PI * local / len);
            int ex = (int) (w * EYE_POS[i][0]), ey = (int) (h * EYE_POS[i][1]);
            gh.drawFilledRectangle(ex - 18, ey - 4, 36, 11, new Color(160, 0, 0, (int) (a * 25)));
            gh.drawFilledRectangle(ex - 12, ey, 8, 3, new Color(200, 0, 0, (int) (a * 170)));
            gh.drawFilledRectangle(ex + 4, ey, 8, 3, new Color(200, 0, 0, (int) (a * 170)));
        }
    }

    // ------------------------------------------------------------------ Dabbah background
    private float phaseAlpha(int phase) {
        int cycle = 3 * PHASE_FRAMES;
        int local = frame - phase * PHASE_FRAMES;
        if (local < 0) return 0f;
        local %= cycle;
        if (local >= PHASE_FRAMES + FADE_FRAMES) return 0f;
        if (local < FADE_FRAMES) return local / (float) FADE_FRAMES;
        if (local < PHASE_FRAMES) return 1f;
        return 1f - (local - PHASE_FRAMES) / (float) FADE_FRAMES;
    }

    private void drawBackground(GraphicsHandler gh, int w, int h, double beat) {
        Graphics2D g = gh.getGraphics();
        Composite old = g.getComposite();
        for (int p = 0; p < 4; p++) { // layers 0-2 are the three forms, layer 3 is the hydra's beams (drawn last, on top)
            float a = p == 3 ? Math.min(1f, phaseAlpha(2) * BEAM_ALPHA * (1f + 0.5f * (float) beat))
                    : Math.min(1f, phaseAlpha(p) * BG_MAX_ALPHA * FORM_ALPHA[p] * (1f + 0.4f * (float) beat)); // everything swells with the heartbeat
            if (a < 0.003f) continue;
            renderLayer(p);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.drawImage(layers[p], 0, 0, w, h, null); // nearest neighbor scaling keeps the pixels chunky
        }
        g.setComposite(old);
    }

    private void renderLayer(int p) {
        Graphics2D g = layerG[p];
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, lw, lh);
        g.setComposite(AlphaComposite.SrcOver);
        switch (p) {
            case 0: drawBlackDragon(g); break;
            case 1: drawFlameDragon(g); break;
            case 3: drawBeams(g); break;
            default: drawHydra(g); break;
        }
    }

    // helpers: shapes are authored in a 0-100 "design space" and scaled to the low-res layer
    private int dx(double v) { return (int) Math.round(v * lw / 100.0); }
    private int dy(double v) { return (int) Math.round(v * lh / 100.0); }

    private void poly(Graphics2D g, double... xy) {
        int n = xy.length / 2;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) { xs[i] = dx(xy[2 * i]); ys[i] = dy(xy[2 * i + 1]); }
        g.fillPolygon(xs, ys, n);
        if (grow > 0) { // thick outline = the shape inflated outward
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke((float) (grow * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawPolygon(xs, ys, n);
            g.setStroke(old);
        }
    }

    // draws a polygon and its mirror image across the vertical center line
    private void polyBoth(Graphics2D g, double... xy) {
        poly(g, xy);
        double[] m = new double[xy.length];
        for (int i = 0; i < xy.length; i += 2) { m[i] = 100 - xy[i]; m[i + 1] = xy[i + 1]; }
        poly(g, m);
    }

    private void blob(Graphics2D g, double cx, double cy, double r) {
        int rr = Math.max(1, dx(r) + (int) Math.round(grow));
        g.fillOval(dx(cx) - rr, dy(cy) - rr, rr * 2, rr * 2);
    }

    // stamps circles along a path of {x, y, radius} keypoints (necks, tails, bodies)
    private void stamp(Graphics2D g, double[][] k) {
        for (int i = 0; i < k.length - 1; i++) {
            for (int s = 0; s <= 12; s++) {
                double t = s / 12.0;
                blob(g, k[i][0] + (k[i + 1][0] - k[i][0]) * t, k[i][1] + (k[i + 1][1] - k[i][1]) * t, k[i][2] + (k[i + 1][2] - k[i][2]) * t);
            }
        }
    }

    // stepped dark red glow behind a form so a black silhouette is readable on a black screen
    private void haze(Graphics2D g, double cx, double cy, double r) {
        for (int k = 6; k >= 1; k--) {
            int c = 18 + (6 - k) * 15;
            g.setColor(new Color(c, 2, 2));
            int rr = dx(r * k / 6.0);
            g.fillOval(dx(cx) - rr, dy(cy) - rr, rr * 2, rr * 2);
        }
    }

    // the dragon silhouette; growPx > 0 draws it inflated by that many low-res pixels (used for the aura outline)
    private void dragonBody(Graphics2D g, Color color, double growPx) {
        grow = growPx;
        g.setColor(color);
        double f = Math.sin(frame * 0.04) * 2.5; // wing flap
        polyBoth(g, 47, 46, 24, 12 + f, 30, 24 + f * .5, 15, 20 + f, 24, 34 + f * .5, 9, 38 + f, 27, 48, 43, 54); // wings
        polyBoth(g, 43, 70, 33, 80, 38, 90, 46, 82);                                                              // legs
        stamp(g, new double[][] { { 50, 80, 8 }, { 47, 90, 5 }, { 55, 96, 3 }, { 68, 94, 1.5 } });               // tail
        stamp(g, new double[][] { { 50, 72, 11 }, { 50, 52, 9 }, { 50, 36, 6 } });                               // torso + neck
        poly(g, 41, 24, 50, 14, 59, 24, 56, 33, 50, 38, 44, 33);                                                  // skull
        polyBoth(g, 43, 21, 31, 6, 36, 17, 41, 27);                                                               // horns
        polyBoth(g, 45, 36, 41, 44, 49, 39);                                                                      // jaw spikes
        grow = 0;
    }

    private void eyes(Graphics2D g) {
        if (frame % 160 > 140) return; // blink
        g.setColor(new Color(255, 20, 20));
        g.fillRect(dx(45) - 1, dy(25), 3, 1);
        g.fillRect(dx(55) - 1, dy(25), 3, 1);
    }

    // FORM 1: pitch black dragon
    private void drawBlackDragon(Graphics2D g) {
        haze(g, 50, 40, 55);
        dragonBody(g, Color.BLACK, 0);
        eyes(g);
    }

    // FORM 2: a thin layer of fire hugging the dragon's outline, with a wider dark aura around that
    private void drawFlameDragon(Graphics2D g) {
        haze(g, 50, 40, 40);
        double pulse = Math.sin(frame * 0.05);
        for (int k = 4; k >= 1; k--) { // dark aura: outermost ring first, each a bigger copy of the silhouette
            dragonBody(g, new Color(75, 0, 115, 60 + (4 - k) * 28), FIRE_THICKNESS + 1 + k * 1.6 + pulse * 0.6);
        }
        // fire: three stacked, slightly inflated copies of the silhouette, shifted upward so the flames lick up off the edges
        Random r = new Random(frame / 4); // flicker ~15 times a second
        Color[] fire = { new Color(190, 25, 0), new Color(255, 120, 0), new Color(255, 225, 70) };
        for (int i = 0; i < 3; i++) {
            double inflate = FIRE_THICKNESS * (1 - i * 0.37) + r.nextDouble() * 1.2;
            int rise = (int) Math.round((2 - i) * 1.5 + r.nextDouble() * 2), tx = r.nextInt(3) - 1;
            g.translate(tx, -rise);
            dragonBody(g, fire[i], inflate);
            g.translate(-tx, rise);
        }
        dragonBody(g, Color.BLACK, 0); // the black body covers the middle, leaving only the fire outline
        for (int i = 0; i < 18; i++) { // embers rising off it
            double ex = 12 + (i * 53) % 76, ey = 70 - ((frame * 0.3 + i * 29) % 70);
            g.setColor(new Color(255, 140 + (i % 3) * 40, 0));
            g.fillRect(dx(ex), dy(ey), 1, 1);
        }
        eyes(g);
    }

    // FORM 3: unbound ruin, a hydra whose heads each fire a different dark beam (dried blood, bruise, drowned blue, rot, rust)
    private static final Color[] BEAMS = { new Color(170, 0, 25), new Color(110, 0, 175), new Color(0, 55, 180), new Color(0, 125, 45), new Color(185, 85, 0) };
    private static final double[][] BEAM_END = { { -30, 18 }, { 15, -35 }, { 50, -45 }, { 85, -35 }, { 130, 18 } };

    private double[][] hydraHeads() {
        double[][] heads = new double[5][2];
        for (int i = 0; i < 5; i++) {
            heads[i][0] = 50 + (i - 2) * 17 + Math.sin(frame * 0.02 + i * 1.3) * 4;
            heads[i][1] = 42 + Math.abs(i - 2) * 6 + Math.cos(frame * 0.025 + i) * 3;
        }
        return heads;
    }

    // dark beams with a near-black core (a hole cut in the world); drawn on their own, stronger layer
    private void drawBeams(Graphics2D g) {
        double[][] heads = hydraHeads();
        for (int i = 0; i < 5; i++) {
            Random r = new Random((frame / 3) * 7919L + i); // jitter ~20 times a second
            int segs = 8;
            int[] xs = new int[segs + 1], ys = new int[segs + 1];
            for (int s = 0; s <= segs; s++) {
                double t = s / (double) segs, j = (s == 0 || s == segs) ? 0 : (r.nextDouble() - .5) * 3;
                xs[s] = dx(heads[i][0] + (BEAM_END[i][0] - heads[i][0]) * t + j);
                ys[s] = dy(heads[i][1] + (BEAM_END[i][1] - heads[i][1]) * t + j);
            }
            double pulse = Math.sin(frame * 0.12 + i * 1.7);
            Color c = BEAMS[i];
            g.setStroke(new BasicStroke((float) (10 + pulse * 1.5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150)); g.drawPolyline(xs, ys, segs + 1);
            g.setStroke(new BasicStroke((float) (5.5 + pulse), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(c);                                                     g.drawPolyline(xs, ys, segs + 1);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(c.getRed() / 6, c.getGreen() / 6, c.getBlue() / 6)); g.drawPolyline(xs, ys, segs + 1);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawHydra(Graphics2D g) {
        haze(g, 50, 70, 60);

        // drifting debris (the ruin is coming apart)
        g.setColor(new Color(70, 18, 18));
        for (int i = 0; i < 16; i++) {
            double x = (i * 37) % 100, y = 100 - ((frame * 0.15 + i * 23) % 100);
            g.fillRect(dx(x), dy(y), 1 + i % 2, 1 + i % 2);
        }

        double[][] heads = hydraHeads();

        // body mass
        g.setColor(Color.BLACK);
        g.fillOval(dx(16), dy(80), dx(68), dy(30));
        g.setColor(new Color(200, 20, 20)); // glowing cracks
        Random cr = new Random(42);
        for (int i = 0; i < 7; i++) {
            int x1 = dx(26 + cr.nextInt(48)), y1 = dy(86 + cr.nextInt(10));
            g.drawLine(x1, y1, x1 + cr.nextInt(7) - 3, y1 + 2 + cr.nextInt(3));
        }

        // necks and heads
        for (int i = 0; i < 5; i++) {
            double bx = 50 + (i - 2) * 9, sway = Math.sin(frame * 0.03 + i * 2.1) * 5;
            double cx = 50 + (i - 2) * 20 + sway, cy = 70;
            double hx = heads[i][0], hy = heads[i][1] + 4;
            g.setColor(Color.BLACK);
            for (int s = 0; s <= 24; s++) {
                double t = s / 24.0, u = 1 - t;
                blob(g, u * u * bx + 2 * u * t * cx + t * t * hx, u * u * 90 + 2 * u * t * cy + t * t * hy, 5 - 2.5 * t);
            }
            poly(g, heads[i][0] - 4, heads[i][1] + 3, heads[i][0], heads[i][1] - 3.5, heads[i][0] + 4, heads[i][1] + 3, heads[i][0] + 2, heads[i][1] + 6, heads[i][0] - 2, heads[i][1] + 6);
            Color c = BEAMS[i];
            g.setColor(new Color(Math.min(255, c.getRed() * 2), Math.min(255, c.getGreen() * 2), Math.min(255, c.getBlue() * 2))); // eye glints brighter than the beam
            g.fillRect(dx(heads[i][0]), dy(heads[i][1]), 1, 1);
        }
    }
}