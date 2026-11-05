package Screens.Battle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

// GLITCH: the flashbacks GRIEVANCE throws at the knight. 11 original pixel-art scenes drawn in code, in a stark
// black / bone-white / blood-red / violet palette with ordered dithering and a dark vignette (inspired by the look of
// old lo-fi horror games, not copied from any of them).
//
// Each scene is painted ONCE onto a small canvas (about 192 x 136 pixels), cached, and blown up with hard pixel edges.
// While it is on screen, draw() adds slow glitch tears, film grain and scanlines on top. There is no strobing:
// PlayBattleScreen fades each scene in and out slowly.
//
//   GriefFlashbacks.COUNT             how many scenes there are
//   GriefFlashbacks.line(i)           the line of text shown under scene i
//   GriefFlashbacks.draw(g, i, w, h, frame)   draws scene i over the whole w x h screen
public final class GriefFlashbacks {
    private GriefFlashbacks() { }

    // the art is designed on this grid; the canvas is grown to fill the screen's shape and the art is centred in it
    private static final int DW = 192, DH = 136;

    // ---------------------------------------------------------------- palette
    private static final Color BLACK = new Color(0, 0, 0);
    private static final Color INK = new Color(9, 5, 13);
    private static final Color NIGHT = new Color(20, 10, 30);
    private static final Color DUSK = new Color(42, 15, 61);
    private static final Color PINE = new Color(28, 12, 40);
    private static final Color VIOLET = new Color(96, 22, 170);
    private static final Color LILAC = new Color(156, 98, 206);
    private static final Color BLOOD = new Color(96, 6, 10);
    private static final Color RED = new Color(168, 18, 18);
    private static final Color BRIGHT = new Color(228, 46, 38);
    private static final Color EMBER = new Color(240, 122, 40);
    private static final Color FLAME = new Color(252, 206, 96);
    private static final Color BONE = new Color(236, 230, 240);
    private static final Color ASH = new Color(150, 144, 160);
    private static final Color STONE = new Color(88, 84, 100);
    private static final Color DARKSTONE = new Color(44, 40, 54);
    private static final Color MOON = new Color(250, 246, 222);
    private static final Color GLASSBLUE = new Color(28, 30, 112);
    private static final Color GOLD = new Color(196, 154, 60);

    // ---------------------------------------------------------------- the scenes

    private static final String[] TITLES = {
            "CALVARY", "THE YARD", "THE FAMINE", "THE MINSTER", "THE RITE", "THE SHIELD",
            "THE WOOD", "THE OSSUARY", "THE WINDOW", "THE DROWNED", "THE MOURNER" };

    private static final String[] LINES = {
            "THEY HUNG THE ONES WHO SPOKE FOR IT.",
            "EVERY NAME HERE WAS SOMEONE YOU KNEW.",
            "NOTHING GREW AGAIN.",
            "THEY SANG HYMNS WHILE IT BURNED.",
            "THE PRAYERS ONLY MADE IT WORSE.",
            "HE SHIELDED IT WITH HIS OWN BODY.",
            "IT WAS WATCHING THE WHOLE TIME.",
            "IT COUNTED EVERY ONE OF THEM.",
            "THE SAINTS LOOKED AWAY.",
            "NOBODY CAME BACK FOR THEM.",
            "YOU WERE TOLD IT WAS A MONSTER." };

    public static final int COUNT = LINES.length;

    public static String line(int i) { return LINES[Math.floorMod(i, COUNT)]; }
    public static String title(int i) { return TITLES[Math.floorMod(i, COUNT)]; }

    // two different scenes for one GRIEVANCE (one per fade), never the same as the previous pair's last scene
    public static int[] pickPair(Random rng, int last) {
        int a;
        do a = rng.nextInt(COUNT); while (a == last && COUNT > 1);
        int b;
        do b = rng.nextInt(COUNT); while ((b == a || b == last) && COUNT > 2);
        return new int[] { a, b };
    }

    // ---------------------------------------------------------------- drawing to the screen

    private static final BufferedImage[] cache = new BufferedImage[COUNT];
    private static int cacheW = -1, cacheH = -1;

    private static int scaleFor(int w, int h) { return Math.max(1, Math.min(w / DW, h / DH)); }

    // the painted scene (built once per screen size)
    public static BufferedImage image(int i, int w, int h) {
        i = Math.floorMod(i, COUNT);
        if (w != cacheW || h != cacheH) {
            for (int k = 0; k < COUNT; k++) cache[k] = null;
            cacheW = w;
            cacheH = h;
        }
        if (cache[i] == null) {
            int s = scaleFor(w, h);
            int cw = (w + s - 1) / s, ch = (h + s - 1) / s;
            BufferedImage img = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(BLACK);
            g.fillRect(0, 0, cw, ch);
            paint(g, i, cw, ch);
            g.dispose();
            vignette(img);
            cache[i] = img;
        }
        return cache[i];
    }

    // draws scene i over the whole screen. frame = frames since it started (drives the tears and grain).
    public static void draw(Graphics2D g, int i, int w, int h, int frame) {
        BufferedImage img = image(i, w, h);
        int s = scaleFor(w, h);
        int cw = img.getWidth(), ch = img.getHeight();
        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(img, 0, 0, cw * s, ch * s, null);

        // glitch tears: every so often a thin band of the picture slides sideways for a few frames
        Random r = new Random(frame / 5 * 977L + i * 31L);
        if (r.nextInt(3) == 0) {
            int bands = 1 + r.nextInt(2);
            for (int b = 0; b < bands; b++) {
                int by = r.nextInt(ch - 6), bh = 1 + r.nextInt(5), off = (r.nextBoolean() ? 1 : -1) * (2 + r.nextInt(7));
                g.drawImage(img, off * s, by * s, (cw + off) * s, (by + bh) * s, 0, by, cw, by + bh, null);
            }
        }
        if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);

        // film grain: a scatter of dark and pale specks, new every 2 frames
        Random grain = new Random(frame / 2 * 7919L);
        for (int k = 0; k < 220; k++) {
            int gx = grain.nextInt(cw), gy = grain.nextInt(ch);
            g.setColor(grain.nextInt(4) == 0 ? new Color(236, 230, 240, 40) : new Color(0, 0, 0, 110));
            g.fillRect(gx * s, gy * s, s, s);
        }
        // scanlines
        g.setColor(new Color(0, 0, 0, 80));
        for (int y = 0; y < h; y += 3) g.fillRect(0, y, w, 1);
    }

    private static void paint(Graphics2D g, int i, int cw, int ch) {
        switch (i) {
            case 0: calvary(g, cw, ch); break;
            case 1: graveyard(g, cw, ch); break;
            case 2: famine(g, cw, ch); break;
            case 3: minster(g, cw, ch); break;
            case 4: rite(g, cw, ch); break;
            case 5: shield(g, cw, ch); break;
            case 6: wood(g, cw, ch); break;
            case 7: ossuary(g, cw, ch); break;
            case 8: window(g, cw, ch); break;
            case 9: drowned(g, cw, ch); break;
            default: mourner(g, cw, ch); break;
        }
    }

    // ================================================================ the 11 scenes
    // Each fills the whole canvas with its backdrop, then moves the origin so (0,0)-(192,136) is the centred art area.

    // 0. three crosses on a hill, a huge moon behind the empty middle one, crows; bodies hang on the outer two
    private static void calvary(Graphics2D g, int cw, int ch) {
        vGrad(g, 0, 0, cw, ch, BLACK, BLOOD, 0.08, 0.62);
        origin(g, cw, ch);
        halo(g, 96, 40, 30, RED);
        disc(g, 96, 40, 21, MOON);
        speck(g, 88, 34, 3, ASH); speck(g, 103, 46, 2, ASH); speck(g, 99, 30, 2, new Color(214, 208, 190)); speck(g, 86, 49, 2, new Color(214, 208, 190));
        // the hill
        poly(g, BLACK, -40, 108, 20, 96, 60, 86, 96, 80, 132, 86, 170, 96, 232, 106, 232, 180, -40, 180);
        grassEdge(g, -40, 232, 7);
        // the middle cross (empty), a black shape against the moon
        rect(g, 94, 30, 5, 52, BLACK);
        rect(g, 80, 42, 33, 4, BLACK);
        drip(g, 96, 82, 5, RED);
        // the outer crosses, with hanging bodies
        for (int side = -1; side <= 1; side += 2) {
            int cx = 96 + side * 50, top = 56 + (side < 0 ? 4 : 2), foot = top + 36;
            rect(g, cx - 1, top, 3, foot - top, BLACK);
            rect(g, cx - 11, top + 6, 23, 3, BLACK);
            // body: head bowed, arms along the bar, legs together
            disc(g, cx + side, top + 12, 2, BLACK);
            line(g, cx, top + 14, cx, top + 25, BLACK);
            line(g, cx - 1, top + 14, cx - 1, top + 24, BLACK);
            line(g, cx, top + 25, cx + side, top + 31, BLACK);
            line(g, cx - 9, top + 9, cx, top + 14, BLACK);
            line(g, cx + 9, top + 9, cx, top + 14, BLACK);
        }
        crow(g, 70, 26); crow(g, 124, 18); crow(g, 150, 38); crow(g, 40, 44); crow(g, 112, 52);
    }

    // 1. a graveyard at night: leaning stones, a dead tree, low fog, and a tall pale figure standing among the graves
    private static void graveyard(Graphics2D g, int cw, int ch) {
        vGrad(g, 0, 0, cw, ch, BLACK, NIGHT, 0.0, 0.70);
        origin(g, cw, ch);
        // crescent moon
        disc(g, 152, 22, 9, MOON);
        disc(g, 157, 19, 8, NIGHT);
        // distant chapel
        rect(g, 158, 70, 22, 22, DARKSTONE);
        poly(g, DARKSTONE, 156, 70, 169, 58, 182, 70);
        rect(g, 167, 50, 3, 10, DARKSTONE); rect(g, 164, 53, 9, 2, DARKSTONE);
        rect(g, 168, 76, 2, 4, EMBER);
        // ground
        rect(g, -40, 92, 272, 90, INK);
        dither(g, -40, 92, 272, 10, DARKSTONE, 4);
        // dead tree
        rect(g, 22, 40, 4, 56, DUSK);
        branch(g, new Random(11), 24, 44, -90, 26, 5, DUSK);
        // back row of stones
        Random r = new Random(3);
        for (int k = 0; k < 7; k++) stone(g, 50 + k * 18 + r.nextInt(5), 90, 7, 11, STONE, ASH, r.nextInt(3) - 1);
        // the figure: tall, thin, a long pale robe, arms hanging far too low, standing behind the graves
        Color pale = new Color(222, 218, 230);
        poly(g, pale, 116, 92, 117, 58, 120, 54, 123, 58, 124, 92);
        poly(g, ASH, 120, 92, 120, 60, 124, 60, 124, 92);
        disc(g, 120, 50, 3, pale);
        rect(g, 117, 47, 7, 2, pale);
        line(g, 116, 58, 113, 84, pale); line(g, 115, 58, 112, 84, ASH);
        line(g, 124, 58, 127, 84, pale); line(g, 125, 58, 128, 84, ASH);
        line(g, 112, 84, 111, 88, pale); line(g, 113, 84, 113, 89, pale);
        line(g, 128, 84, 129, 88, pale); line(g, 127, 84, 127, 89, pale);
        rect(g, 119, 50, 3, 1, BLACK);
        dot(g, 119, 50, BRIGHT); dot(g, 121, 50, BRIGHT);
        // fog
        dither(g, -40, 86, 272, 8, ASH, 2);
        dither(g, -40, 94, 272, 6, ASH, 3);
        // front row of stones, bigger
        stone(g, 34, 116, 12, 18, ASH, BONE, 0);
        stone(g, 70, 112, 10, 15, ASH, BONE, 1);
        stone(g, 146, 118, 13, 20, ASH, BONE, -1);
        cross(g, 104, 96, 18, 9, ASH);
        cross(g, 176, 98, 16, 8, STONE);
        dither(g, -40, 118, 272, 8, ASH, 1);
    }

    // 2. a barn floor of shrouded bodies, one small, a hooded woman kneeling at a single candle
    private static void famine(Graphics2D g, int cw, int ch) {
        rect(g, 0, 0, cw, ch, BLACK);
        origin(g, cw, ch);
        // plank wall
        for (int x = -40; x < 232; x += 9) {
            rect(g, x, 0, 8, 70, new Color(24, 16, 22));
            line(g, x + 8, 0, x + 8, 70, BLACK);
        }
        dither(g, -40, 0, 272, 70, BLACK, 8);
        // floor
        rect(g, -40, 70, 272, 70, new Color(16, 10, 14));
        for (int y = 74; y < 140; y += 7) line(g, -40, y, 232, y, new Color(10, 6, 10));
        // candlelight
        for (int rr = 44; rr > 0; rr -= 8) ditherDisc(g, 178, 94, rr, BLOOD, 2 + (44 - rr) / 8);
        ditherDisc(g, 178, 90, 10, EMBER, 4);
        // shrouds: two rows, the back one smaller; one is a child's
        shroud(g, 58, 84, 44, 9);
        shroud(g, 110, 84, 26, 8);     // the small one
        shroud(g, 44, 104, 62, 13);
        shroud(g, 112, 106, 60, 13);
        line(g, 134, 113, 136, 122, ASH); rect(g, 135, 122, 3, 2, ASH);   // a thin arm slipped out from under the sheet
        // candle
        rect(g, 177, 92, 3, 8, BONE);
        flame(g, 178, 90);
        // the mourner, kneeling, hood bowed toward the bodies
        poly(g, VIOLET, 8, 126, 12, 104, 18, 92, 26, 86, 34, 88, 38, 96, 40, 110, 46, 126);
        poly(g, new Color(60, 14, 104), 26, 88, 34, 88, 38, 96, 36, 104, 30, 102);
        disc(g, 33, 97, 3, BLACK);                       // the face, lost in the hood
        line(g, 36, 106, 44, 110, LILAC); line(g, 36, 107, 44, 111, VIOLET);   // arm reaching to the nearest body
        dot(g, 45, 110, BONE);
        dither(g, 8, 104, 38, 22, BLACK, 4);
    }

    // 3. the minster burning against a red sky, the mob below with torches and forks
    private static void minster(Graphics2D g, int cw, int ch) {
        vGrad(g, 0, 0, cw, ch / 2, BLACK, BLOOD, 0.1, 1.0);
        vGrad(g, 0, ch / 2, cw, ch - ch / 2, BLOOD, RED, 0.0, 0.8);
        origin(g, cw, ch);
        // smoke
        Random r = new Random(5);
        for (int k = 0; k < 9; k++) ditherDisc(g, 70 + r.nextInt(70), 6 + r.nextInt(30), 10 + r.nextInt(12), DARKSTONE, 3 + r.nextInt(3));
        // the church
        rect(g, 54, 64, 74, 40, BLACK);
        poly(g, BLACK, 50, 66, 91, 42, 132, 66);
        rect(g, 120, 30, 18, 74, BLACK);
        poly(g, BLACK, 118, 32, 129, 8, 140, 32);
        rect(g, 128, 0, 3, 10, BLACK); rect(g, 124, 3, 11, 2, BLACK);
        for (int k = 0; k < 4; k++) archWindow(g, 62 + k * 14, 72, 6, 16, k % 2 == 0 ? FLAME : EMBER);
        archWindow(g, 126, 46, 6, 14, EMBER);
        // fire on the roof
        for (int k = 0; k < 7; k++) {
            int fx = 58 + k * 10, fh = 10 + (k * 7) % 13;
            poly(g, RED, fx - 6, 66 - (k % 3) * 4, fx, 66 - fh - 10, fx + 6, 66 - (k % 3) * 4);
            poly(g, EMBER, fx - 3, 64 - (k % 3) * 4, fx, 64 - fh, fx + 3, 64 - (k % 3) * 4);
        }
        // ground and the mob
        rect(g, -40, 104, 272, 40, BLACK);
        int[] xs = { -10, 6, 20, 36, 50, 64, 140, 156, 170, 184, 200 };
        for (int k = 0; k < xs.length; k++) {
            int x = xs[k], top = 98 + (k * 5) % 7;
            person(g, x, top, BLACK);
            if (k % 3 == 0) torch(g, x + 4, top - 14);
            else if (k % 3 == 1) fork(g, x - 3, top - 16);
            else line(g, x + 3, top + 4, x + 7, top - 6, BLACK);
        }
    }

    // 4. a priest raising a crucifix over a body twisted on a bed, red marks on the wall, moonlight through a window
    private static void rite(Graphics2D g, int cw, int ch) {
        rect(g, 0, 0, cw, ch, BLACK);
        origin(g, cw, ch);
        // wall and floor
        rect(g, -40, 0, 272, 98, INK);
        rect(g, -40, 98, 272, 40, new Color(14, 8, 16));
        // window with moonlight
        rect(g, 140, 18, 30, 36, DUSK);
        dither(g, 141, 19, 28, 34, LILAC, 3);
        rect(g, 154, 18, 2, 36, BLACK); rect(g, 140, 34, 30, 2, BLACK);
        for (int k = 0; k < 40; k++) dither(g, 100 - k, 54 + k, 42, 1, LILAC, 1);
        // red marks on the wall
        line(g, 30, 22, 44, 30, RED); line(g, 31, 24, 46, 34, RED); line(g, 36, 16, 33, 42, BLOOD);
        handprint(g, 76, 24, RED); handprint(g, 90, 36, BLOOD);
        cross(g, 116, 26, 16, 8, BLOOD);   // hung upside down
        rect(g, 115, 18, 3, 2, BLOOD);
        // the bed
        rect(g, 72, 86, 82, 6, new Color(60, 40, 44));
        rect(g, 72, 92, 4, 14, new Color(40, 26, 30));
        rect(g, 150, 92, 4, 14, new Color(40, 26, 30));
        rect(g, 70, 74, 4, 32, new Color(40, 26, 30));
        rect(g, 74, 80, 78, 7, ASH);
        dither(g, 74, 84, 78, 3, STONE, 3);
        // the body, arched up off the sheets, head thrown back, hair spilling down
        poly(g, BONE, 84, 82, 96, 70, 112, 66, 128, 70, 140, 80, 132, 80, 116, 74, 100, 76);
        disc(g, 82, 80, 4, BONE);
        line(g, 80, 84, 76, 98, BLACK); line(g, 82, 84, 79, 100, BLACK); line(g, 84, 84, 83, 96, BLACK);
        line(g, 112, 70, 118, 52, BONE); line(g, 118, 52, 122, 48, BONE); line(g, 118, 52, 116, 47, BONE);
        dot(g, 81, 79, RED);
        // the priest, a black shape edged in pale light, crucifix held out
        Polygon priest = new Polygon(new int[] { 18, 24, 30, 36, 40, 44, 50, 46, 40, 14 }, new int[] { 132, 70, 60, 58, 62, 74, 132, 132, 132, 132 }, 10);
        rim(g, priest, ASH);
        disc(g, 34, 52, 5, BLACK);
        rimDisc(g, 34, 52, 5, ASH);
        line(g, 40, 72, 58, 64, BLACK); line(g, 40, 73, 58, 65, BLACK);
        line(g, 40, 71, 58, 63, ASH);
        rect(g, 59, 52, 2, 16, GOLD); rect(g, 55, 56, 10, 2, GOLD);
        dot(g, 60, 51, FLAME);
        rect(g, 32, 64, 4, 3, BONE);   // collar
    }

    // 5. a man curled over a small creature with violet eyes, stones in the air, the mob closing with torches
    private static void shield(Graphics2D g, int cw, int ch) {
        vGrad(g, 0, 0, cw, ch, BLACK, BLOOD, 0.3, 0.85);
        origin(g, cw, ch);
        rect(g, -40, 104, 272, 40, BLACK);
        // the mob in a half-ring behind
        int[] xs = { -8, 10, 26, 42, 150, 166, 182, 198 };
        for (int k = 0; k < xs.length; k++) {
            int x = xs[k], top = 84 + (k * 7) % 9;
            person(g, x, top, BLACK);
            line(g, x + 4, top + 4, x + 8, top - 8, BLACK);
            if (k % 2 == 0) torch(g, x + 8, top - 16);
            else disc(g, x + 8, top - 9, 2, STONE);
        }
        // stones mid-air, with streaks
        int[][] st = { { 60, 54 }, { 74, 40 }, { 128, 46 }, { 140, 62 }, { 100, 30 } };
        for (int[] s : st) {
            disc(g, s[0], s[1], 2, ASH);
            dither(g, s[0] + (s[0] < 96 ? -9 : 3), s[1] - 1, 7, 2, ASH, 2);
        }
        // the man, kneeling and curled over it: folded legs, a rounded back, head bowed down low at the front
        Polygon man = new Polygon(new int[] { 58, 60, 66, 74, 86, 100, 112, 120, 124, 118, 110, 100, 84, 70 },
                new int[] { 122, 110, 98, 88, 80, 76, 78, 84, 94, 102, 104, 106, 112, 122 }, 14);
        rim(g, man, BONE);
        disc(g, 124, 102, 6, BLACK);
        rimDisc(g, 124, 102, 6, BONE);
        rect(g, 118, 96, 6, 8, BLACK);   // neck into the head
        poly(g, BLACK, 52, 122, 58, 114, 70, 116, 74, 122);   // feet under him
        line(g, 52, 122, 58, 114, BONE);
        // his back, bleeding
        drip(g, 92, 79, 7, RED); drip(g, 102, 77, 5, RED); drip(g, 82, 83, 6, BRIGHT); drip(g, 110, 79, 4, BLOOD);
        dot(g, 96, 77, BRIGHT); dot(g, 106, 76, BRIGHT);
        // the creature underneath, only its eyes
        rect(g, 96, 110, 26, 12, BLACK);
        dot(g, 112, 115, LILAC); dot(g, 113, 115, VIOLET);
        dot(g, 118, 115, LILAC); dot(g, 119, 115, VIOLET);
        // his arm wrapped around it, his hand flat on the ground
        line(g, 100, 92, 96, 112, BONE); line(g, 96, 112, 122, 121, BONE);
        rect(g, 122, 119, 4, 2, BONE);
    }

    // 6. a pine wood at night, layer behind layer, a path into it, and eyes everywhere
    private static void wood(Graphics2D g, int cw, int ch) {
        vGrad(g, 0, 0, cw, ch, NIGHT, BLACK, 0.0, 0.9);
        origin(g, cw, ch);
        disc(g, 40, 18, 6, MOON); disc(g, 43, 16, 6, NIGHT);
        Random r = new Random(9);
        for (int x = -40; x < 232; x += 10 + r.nextInt(6)) pine(g, x, 70 + r.nextInt(6), 22 + r.nextInt(10), DUSK);
        // the path
        poly(g, DARKSTONE, 84, 140, 108, 140, 100, 82, 96, 82);
        dither(g, 70, 82, 50, 58, BLACK, 4);
        for (int x = -40; x < 232; x += 14 + r.nextInt(8)) if (Math.abs(x - 96) > 14) pine(g, x, 92 + r.nextInt(6), 40 + r.nextInt(12), PINE);
        // eyes in the dark
        int[][] eyes = { { 20, 74 }, { 52, 66 }, { 70, 84 }, { 128, 70 }, { 150, 80 }, { 176, 62 }, { 116, 92 }, { 36, 96 }, { 160, 100 }, { 96, 76 } };
        for (int k = 0; k < eyes.length; k++) {
            Color c = k % 4 == 3 ? BRIGHT : BONE;
            dot(g, eyes[k][0], eyes[k][1], c);
            dot(g, eyes[k][0] + 3, eyes[k][1], c);
        }
        for (int x = -40; x < 232; x += 26 + r.nextInt(10)) if (Math.abs(x - 96) > 24) pine(g, x, 140, 80 + r.nextInt(20), BLACK);
        // cut stumps at the edge of the path
        stump(g, 66, 124); stump(g, 128, 128); stump(g, 150, 116);
    }

    // 7. a wall of skulls under a stone arch, candles burning on the ledge
    private static void ossuary(Graphics2D g, int cw, int ch) {
        rect(g, 0, 0, cw, ch, BLACK);
        origin(g, cw, ch);
        // the arch (stone) and its dark opening
        arch(g, 96, 10, 66, 120, DARKSTONE);
        arch(g, 96, 16, 58, 114, STONE);
        arch(g, 96, 20, 52, 110, BLACK);
        // skulls stacked inside, darker toward the top
        Shape old = g.getClip();
        g.clip(archShape(96, 20, 52, 110));
        for (int y = 24, row = 0; y < 106; y += 8, row++) {
            for (int x = 40 + (row % 2) * 4; x < 152; x += 9) {
                Color c = (x / 9 + row) % 5 == 0 ? ASH : BONE;
                skull(g, x, y, c);
            }
        }
        dither(g, 40, 20, 112, 30, BLACK, 10);
        dither(g, 40, 50, 112, 20, BLACK, 6);
        g.setClip(old);
        // ledge, bones, candles
        rect(g, 20, 106, 152, 6, STONE);
        rect(g, 20, 112, 152, 2, DARKSTONE);
        for (int cx : new int[] { 34, 58, 134, 158 }) {
            ditherDisc(g, cx, 98, 12, BLOOD, 3);
            rect(g, cx - 1, 98, 3, 8, BONE);
            drip(g, cx + 1, 100, 3, ASH);
            flame(g, cx, 96);
        }
        for (int k = 0; k < 6; k++) line(g, 30 + k * 26, 126 - (k % 2) * 3, 42 + k * 26, 122, ASH);
    }

    // 8. a tall stained-glass window of a veiled saint weeping blood, cracked and missing panes, coloured light on stone
    private static void window(Graphics2D g, int cw, int ch) {
        rect(g, 0, 0, cw, ch, BLACK);
        origin(g, cw, ch);
        // stone wall
        for (int y = -10, row = 0; y < 140; y += 8, row++)
            for (int x = -40 + (row % 2) * 8; x < 232; x += 16) {
                rect(g, x, y, 15, 7, new Color(18, 14, 24));
                dot(g, x + 2, y + 2, DARKSTONE);
            }
        // the window
        arch(g, 96, 6, 60, 104, DARKSTONE);
        Shape glass = archShape(96, 10, 52, 98);
        Shape old = g.getClip();
        g.clip(glass);
        Color[] panes = { VIOLET, BLOOD, GLASSBLUE, DUSK, new Color(70, 14, 120), RED };
        Random r = new Random(21);
        for (int y = 8; y < 110; y += 9)
            for (int x = 68; x < 126; x += 9) {
                Color c = panes[r.nextInt(panes.length)];
                if (r.nextInt(9) == 0) c = BLACK;   // a missing pane
                rect(g, x, y, 9, 9, c);
                dither(g, x, y, 9, 9, BLACK, 3);
                rect(g, x, y, 9, 1, BLACK);
                rect(g, x, y, 1, 9, BLACK);
            }
        // the saint
        ditherDisc(g, 96, 34, 13, GOLD, 6);
        disc(g, 96, 34, 11, BLACK);
        poly(g, new Color(54, 50, 120), 84, 104, 86, 46, 96, 26, 106, 46, 108, 104);
        poly(g, new Color(80, 76, 160), 88, 104, 90, 52, 96, 40, 102, 52, 104, 104);
        disc(g, 96, 38, 6, BONE);
        poly(g, new Color(54, 50, 120), 89, 36, 96, 28, 103, 36, 102, 32, 96, 30, 90, 32);
        dot(g, 94, 38, BLACK); dot(g, 98, 38, BLACK);
        line(g, 94, 39, 93, 50, BRIGHT); line(g, 98, 39, 99, 54, BRIGHT);
        drip(g, 99, 54, 6, RED);
        rect(g, 93, 60, 6, 8, BONE);   // folded hands
        line(g, 96, 60, 96, 67, ASH);
        // cracks from a hole near the top
        disc(g, 110, 22, 3, BLACK);
        Random cr = new Random(4);
        for (int k = 0; k < 7; k++) {
            int x = 110, y = 22;
            double ang = cr.nextDouble() * Math.PI * 2;
            for (int seg = 0; seg < 4; seg++) {
                int nx = x + (int) (Math.cos(ang) * (6 + cr.nextInt(6))), ny = y + (int) (Math.sin(ang) * (6 + cr.nextInt(6)));
                line(g, x, y, nx, ny, BONE);
                x = nx; y = ny;
                ang += cr.nextDouble() - 0.5;
            }
        }
        g.setClip(old);
        // coloured light falling on the floor
        for (int y = 110; y < 140; y++) {
            int spread = (y - 110) / 2;
            dither(g, 70 - spread, y, 52 + spread * 2, 1, VIOLET, 2);
            dither(g, 84 - spread / 2, y, 24 + spread, 1, BLOOD, 2);
        }
    }

    // 9. black water under the moon, its reflection broken, pale hands reaching up out of it, a lantern far off
    private static void drowned(Graphics2D g, int cw, int ch) {
        vGrad(g, 0, 0, cw, ch, BLACK, NIGHT, 0.0, 0.5);
        origin(g, cw, ch);
        halo(g, 140, 26, 20, DUSK);
        disc(g, 140, 26, 12, MOON);
        speck(g, 136, 22, 2, ASH);
        // water
        rect(g, -40, 66, 272, 80, INK);
        for (int y = 68; y < 140; y += 3) {
            int seed = y * 13;
            for (int x = -40; x < 232; x += 12 + (seed + x) % 9) line(g, x, y, x + 4 + (seed + x) % 6, y, DUSK);
        }
        // the moon's broken reflection
        for (int y = 70, k = 0; y < 130; y += 4, k++) {
            int wob = (k * 5) % 7 - 3;
            line(g, 134 + wob, y, 146 + wob - k / 4, y, k % 3 == 0 ? ASH : MOON);
        }
        // a boat with a lantern, far off
        poly(g, BLACK, 14, 70, 44, 70, 40, 76, 18, 76);
        rect(g, 30, 58, 1, 12, BLACK);
        disc(g, 30, 64, 1, FLAME);
        ditherDisc(g, 30, 64, 5, EMBER, 4);
        // hands
        int[][] hands = { { 40, 104, 20 }, { 64, 92, 12 }, { 86, 114, 26 }, { 108, 98, 14 }, { 158, 110, 22 }, { 178, 92, 10 }, { 24, 124, 18 }, { 124, 126, 24 } };
        for (int k = 0; k < hands.length; k++) hand(g, hands[k][0], hands[k][1], hands[k][2], k % 2 == 0 ? 1 : -1);
        // ripples where the arms break the water
        for (int[] hd : hands) line(g, hd[0] - 5, hd[1], hd[0] + 6, hd[1], STONE);
    }

    // 10. the hooded mourner from before, redrawn: a red arm reaching up out of the hood, hands rising, two crosses
    private static void mourner(Graphics2D g, int cw, int ch) {
        rect(g, 0, 0, cw, ch, BLACK);
        origin(g, cw, ch);
        for (int cx : new int[] { 20, 172 }) cross(g, cx, 8, 20, 12, BONE);
        // hands rising at the sides
        for (int side = -1; side <= 1; side += 2) {
            int bx = 96 + side * 60;
            hand(g, bx, 140, 46, side);
            hand(g, bx + side * 18, 140, 36, -side);
        }
        // the hooded figure
        poly(g, VIOLET, 76, 140, 82, 92, 90, 78, 102, 78, 110, 92, 116, 140);
        poly(g, new Color(60, 14, 104), 86, 90, 96, 80, 106, 90, 104, 100, 88, 100);
        disc(g, 96, 94, 4, BLACK);
        dither(g, 76, 100, 40, 40, new Color(60, 14, 104), 4);
        // the red arm out of the hood, fingers spread wide
        line(g, 95, 90, 97, 24, RED); line(g, 96, 90, 98, 24, RED); line(g, 94, 90, 96, 24, BLOOD);
        int hx = 97, hy = 22;
        disc(g, hx, hy, 3, RED);
        for (int f = -2; f <= 2; f++) line(g, hx, hy, hx + f * 5, hy - 13 + Math.abs(f) * 3, RED);
        drip(g, 97, 60, 6, BRIGHT);
    }

    // ================================================================ drawing helpers (all in canvas pixels)

    // moves (0,0) so the 192 x 136 art area is centred in the canvas
    private static void origin(Graphics2D g, int cw, int ch) {
        g.translate((cw - DW) / 2, (ch - DH) / 2);
    }

    private static final int[][] BAYER = { { 0, 8, 2, 10 }, { 12, 4, 14, 6 }, { 3, 11, 1, 9 }, { 15, 7, 13, 5 } };

    private static void dot(Graphics2D g, int x, int y, Color c) {
        g.setColor(c);
        g.fillRect(x, y, 1, 1);
    }

    private static void rect(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    // fills the area with c on an ordered-dither pattern; level 0 (none) .. 16 (solid)
    private static void dither(Graphics2D g, int x, int y, int w, int h, Color c, int level) {
        g.setColor(c);
        for (int yy = y; yy < y + h; yy++)
            for (int xx = x; xx < x + w; xx++)
                if (BAYER[Math.floorMod(yy, 4)][Math.floorMod(xx, 4)] < level) g.fillRect(xx, yy, 1, 1);
    }

    // a vertical blend from top to bottom made of dither (from..to = where in the height the blend runs)
    private static void vGrad(Graphics2D g, int x, int y, int w, int h, Color top, Color bottom, double from, double to) {
        for (int yy = 0; yy < h; yy++) {
            double f = Math.max(0, Math.min(1, ((double) yy / Math.max(1, h - 1) - from) / Math.max(0.001, to - from)));
            int level = (int) Math.round(f * 16);
            for (int xx = 0; xx < w; xx++) {
                g.setColor(BAYER[(y + yy) & 3][(x + xx) & 3] < level ? bottom : top);
                g.fillRect(x + xx, y + yy, 1, 1);
            }
        }
    }

    private static void line(Graphics2D g, int x1, int y1, int x2, int y2, Color c) {
        g.setColor(c);
        int dx = Math.abs(x2 - x1), dy = -Math.abs(y2 - y1), sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1, err = dx + dy;
        while (true) {
            g.fillRect(x1, y1, 1, 1);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    private static void disc(Graphics2D g, int cx, int cy, int r, Color c) {
        g.setColor(c);
        for (int y = -r; y <= r; y++) {
            int span = (int) Math.floor(Math.sqrt(r * r + r * 0.8 - y * y));
            g.fillRect(cx - span, cy + y, span * 2 + 1, 1);
        }
    }

    private static void ditherDisc(Graphics2D g, int cx, int cy, int r, Color c, int level) {
        g.setColor(c);
        for (int y = -r; y <= r; y++)
            for (int x = -r; x <= r; x++)
                if (x * x + y * y <= r * r && BAYER[Math.floorMod(cy + y, 4)][Math.floorMod(cx + x, 4)] < level) g.fillRect(cx + x, cy + y, 1, 1);
    }

    // a soft dithered glow fading out from r/2 to r
    private static void halo(Graphics2D g, int cx, int cy, int r, Color c) {
        for (int rr = r; rr > r / 2; rr -= 3) ditherDisc(g, cx, cy, rr, c, 2 + (r - rr) / 2);
    }

    private static void speck(Graphics2D g, int x, int y, int r, Color c) { disc(g, x, y, Math.max(0, r - 1), c); }

    private static void poly(Graphics2D g, Color c, int... pts) {
        Polygon p = new Polygon();
        for (int k = 0; k + 1 < pts.length; k += 2) p.addPoint(pts[k], pts[k + 1]);
        g.setColor(c);
        g.fillPolygon(p);
    }

    // a black shape with a one-pixel edge of light around it
    private static void rim(Graphics2D g, Polygon p, Color edge) {
        g.setColor(edge);
        for (int[] d : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } }) {
            p.translate(d[0], d[1]);
            g.fillPolygon(p);
            p.translate(-d[0], -d[1]);
        }
        g.setColor(BLACK);
        g.fillPolygon(p);
    }

    private static void rimDisc(Graphics2D g, int cx, int cy, int r, Color edge) {
        Color fill = BLACK;
        disc(g, cx, cy, r + 1, edge);
        disc(g, cx, cy, r, fill);
    }

    private static void drip(Graphics2D g, int x, int y, int len, Color c) {
        rect(g, x, y, 1, len, c);
        dot(g, x, y + len, c);
        dot(g, x - 1, y + len, c);
    }

    private static void cross(Graphics2D g, int cx, int top, int h, int w, Color c) {
        rect(g, cx - 1, top, 3, h, c);
        rect(g, cx - w / 2, top + h / 4, w, 2, c);
    }

    private static void crow(Graphics2D g, int x, int y) {
        line(g, x - 4, y - 2, x, y, BLACK);
        line(g, x, y, x + 4, y - 2, BLACK);
        line(g, x - 3, y - 1, x, y + 1, BLACK);
        line(g, x, y + 1, x + 3, y - 1, BLACK);
    }

    private static void grassEdge(Graphics2D g, int x1, int x2, int seed) {
        // tufts poking up along the hill's outline (found by walking down each column until the hill starts)
        Random r = new Random(seed);
        for (int x = x1; x < x2; x += 2 + r.nextInt(3)) {
            int y = hillY(x);
            line(g, x, y, x + r.nextInt(3) - 1, y - 2 - r.nextInt(3), BLACK);
        }
    }

    private static int hillY(int x) {
        int[] xs = { -40, 20, 60, 96, 132, 170, 232 }, ys = { 108, 96, 86, 80, 86, 96, 106 };
        for (int k = 0; k + 1 < xs.length; k++)
            if (x >= xs[k] && x <= xs[k + 1]) return ys[k] + (ys[k + 1] - ys[k]) * (x - xs[k]) / (xs[k + 1] - xs[k]);
        return 108;
    }

    private static void branch(Graphics2D g, Random r, int x, int y, double angle, double len, int depth, Color c) {
        if (depth == 0 || len < 3) return;
        int nx = x + (int) Math.round(Math.cos(Math.toRadians(angle)) * len), ny = y + (int) Math.round(Math.sin(Math.toRadians(angle)) * len);
        line(g, x, y, nx, ny, c);
        if (depth > 3) line(g, x + 1, y, nx + 1, ny, c);
        branch(g, r, nx, ny, angle - 18 - r.nextInt(22), len * (0.6 + r.nextDouble() * 0.15), depth - 1, c);
        branch(g, r, nx, ny, angle + 18 + r.nextInt(22), len * (0.6 + r.nextDouble() * 0.15), depth - 1, c);
    }

    // a gravestone with a rounded top, lit from the left, leaning by tilt pixels
    private static void stone(Graphics2D g, int x, int baseY, int w, int h, Color body, Color light, int tilt) {
        int top = baseY - h;
        for (int y = top; y < baseY; y++) {
            int shift = tilt * (baseY - y) / Math.max(1, h / 2);
            int inset = y - top < w / 2 ? (w / 2) - (int) Math.sqrt(Math.max(0, (w / 2) * (w / 2) - (w / 2 - (y - top)) * (w / 2 - (y - top)))) : 0;
            rect(g, x - w / 2 + inset + shift, y, w - inset * 2, 1, body);
            dot(g, x - w / 2 + inset + shift, y, light);
        }
        rect(g, x - 1 + tilt, top + h / 3, 3, h / 3, DARKSTONE);
        rect(g, x - w / 4 + tilt, top + h / 3 + 2, w / 2, 1, DARKSTONE);
    }

    // a body under a sheet, lying sideways: round head, broad shoulders, chest rise, narrowing legs, feet poking up
    private static void shroud(Graphics2D g, int x, int y, int len, int th) {
        int r = th / 2;
        int sx = x + r * 2;   // shoulders
        poly(g, ASH, sx - 1, y - r - 1, sx + len / 3, y - r - 2, x + len - 3, y - r / 2, x + len, y + 1, x + len - 3, y + r + 1, sx, y + r + 1);
        poly(g, BONE, sx, y - r, sx + len / 3, y - r - 1, x + len - 4, y - r / 2 + 1, x + len - 1, y, x + len - 4, y + r - 1, sx, y + r - 1);
        disc(g, x + r + 1, y - 1, r, BONE);                               // head
        rect(g, x + len - 5, y - r / 2 - 2, 2, 3, BONE);                       // toes lifting the sheet
        dither(g, x, y + 1, len + 1, r + 1, STONE, 7);                     // shadowed underside
        line(g, sx + 2, y - r + 1, sx + 5, y + r - 2, ASH);               // folds
        line(g, sx + len / 3, y - r, sx + len / 3 - 2, y + r - 2, ASH);
        line(g, x + len * 3 / 4, y - r / 2, x + len * 3 / 4 + 2, y + r - 2, ASH);
        rect(g, x - 2, y + r + 1, len + 6, 1, BLACK);                     // contact shadow
    }

    private static void flame(Graphics2D g, int x, int y) {
        dot(g, x, y - 3, EMBER);
        rect(g, x - 1, y - 2, 3, 2, EMBER);
        rect(g, x, y - 2, 1, 2, FLAME);
    }

    private static void archWindow(Graphics2D g, int x, int y, int w, int h, Color c) {
        rect(g, x, y + w / 2, w, h - w / 2, c);
        disc(g, x + w / 2, y + w / 2, w / 2, c);
        rect(g, x + w / 2, y, 1, h, BLACK);
    }

    // a person seen from behind: head and shoulders
    private static void person(Graphics2D g, int x, int top, Color c) {
        disc(g, x + 4, top, 3, c);
        poly(g, c, x - 3, top + 40, x - 2, top + 6, x + 10, top + 6, x + 11, top + 40);
    }

    private static void torch(Graphics2D g, int x, int y) {
        rect(g, x, y, 1, 16, BLACK);
        ditherDisc(g, x, y - 2, 6, EMBER, 4);
        poly(g, EMBER, x - 2, y, x, y - 7, x + 2, y);
        dot(g, x, y - 2, FLAME);
    }

    private static void fork(Graphics2D g, int x, int y) {
        line(g, x, y + 22, x + 4, y, BLACK);
        line(g, x + 1, y, x + 7, y + 1, BLACK);
        line(g, x + 1, y, x + 1, y - 5, BLACK);
        line(g, x + 4, y, x + 4, y - 6, BLACK);
        line(g, x + 7, y + 1, x + 7, y - 4, BLACK);
    }

    private static void handprint(Graphics2D g, int x, int y, Color c) {
        rect(g, x, y, 5, 5, c);
        for (int f = 0; f < 4; f++) rect(g, x + f + (f > 1 ? 1 : 0), y - 3 - (f == 1 || f == 2 ? 1 : 0), 1, 3, c);
        line(g, x - 1, y + 2, x - 3, y, c);
        drip(g, x + 2, y + 5, 4, c);
    }

    private static void pine(Graphics2D g, int x, int baseY, int h, Color c) {
        rect(g, x - 1, baseY - h / 5, 2, h / 5, c);
        for (int tier = 0; tier < 4; tier++) {
            int ty = baseY - h / 5 - tier * h / 5, half = (h / 4) - tier * (h / 18);
            poly(g, c, x - half, ty, x, ty - h / 3, x + half, ty);
        }
    }

    private static void stump(Graphics2D g, int x, int y) {
        rect(g, x - 4, y - 5, 9, 6, DARKSTONE);
        g.setColor(ASH);
        g.fillOval(x - 4, y - 7, 9, 4);
        g.setColor(STONE);
        g.drawOval(x - 2, y - 6, 4, 2);
        dot(g, x, y - 5, DARKSTONE);
    }

    private static final String[] SKULL = {
            ".#####.",
            "#######",
            "#..#..#",
            "#######",
            ".##.##.",
            "..#.#.." };

    private static void skull(Graphics2D g, int x, int y, Color c) {
        for (int row = 0; row < SKULL.length; row++)
            for (int col = 0; col < SKULL[row].length(); col++)
                if (SKULL[row].charAt(col) == '#') dot(g, x + col, y + row, c);
                else if (SKULL[row].charAt(col) == '.' && row == 2) dot(g, x + col, y + row, BLACK);
    }

    // a pointed (equilateral) gothic arch: straight sides, then two arcs of radius w meeting in a point at the top
    private static Shape archShape(int cx, int top, int w, int h) {
        int half = w / 2;
        double rise = w * 0.866;          // height of the pointed part
        double spring = top + rise;       // where the arcs start
        Polygon p = new Polygon();
        p.addPoint(cx - half, top + h);
        for (int k = 0; k <= 16; k++) {   // left arc, centred on the right springing point
            double a = Math.toRadians(180 - 60 * k / 16.0);
            p.addPoint((int) Math.round(cx + half + w * Math.cos(a)), (int) Math.round(spring - w * Math.sin(a)));
        }
        for (int k = 16; k >= 0; k--) {   // right arc, centred on the left springing point
            double a = Math.toRadians(60 * k / 16.0);
            p.addPoint((int) Math.round(cx - half + w * Math.cos(a)), (int) Math.round(spring - w * Math.sin(a)));
        }
        p.addPoint(cx + half, top + h);
        return p;
    }

    private static void arch(Graphics2D g, int cx, int top, int w, int h, Color c) {
        g.setColor(c);
        g.fill(archShape(cx, top, w, h));
    }

    // a pale forearm and hand reaching up out of the water / ground, fingers spread
    private static void hand(Graphics2D g, int x, int baseY, int len, int lean) {
        int topY = baseY - len;
        int tx = x + lean * len / 8;
        poly(g, ASH, x - 3, baseY, tx - 2, topY + 6, tx + 3, topY + 6, x + 3, baseY);
        poly(g, BONE, x - 2, baseY, tx - 1, topY + 6, tx + 2, topY + 6, x + 1, baseY);
        rect(g, tx - 3, topY + 1, 7, 6, BONE);
        dot(g, tx + 3, topY + 6, ASH);
        for (int f = 0; f < 4; f++) line(g, tx - 3 + f * 2, topY + 1, tx - 5 + f * 3, topY - 5 + (f == 1 || f == 2 ? -1 : 1), BONE);
        line(g, tx - 3, topY + 4, tx - 7, topY + 1, BONE);   // thumb
    }

    // darken the canvas edges with dither so the eye goes to the middle
    private static void vignette(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        double cx = w / 2.0, cy = h / 2.0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double dx = (x - cx) / cx, dy = (y - cy) / cy;
                double d = Math.sqrt(dx * dx * 0.9 + dy * dy * 1.1);
                int level = (int) Math.round(Math.max(0, Math.min(1, (d - 0.72) / 0.45)) * 16);
                if (BAYER[y & 3][x & 3] < level) img.setRGB(x, y, 0);
            }
    }
}