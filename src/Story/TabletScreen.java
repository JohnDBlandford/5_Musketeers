package Story;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.ScreenManager;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// STORY: the hieroglyph tablet. Each of Worlds 1-3 has one carved stone tablet broken into 6 pieces: five from quests and
// secrets and the large centre KEYSTONE that only the world's boss drops. The pieces fit along jagged cracks.
//
// When the last piece is found (assemble = true):
//   1. the empty tablet appears, then the pieces fly in one by one and lock into place, the keystone last with a thud
//   2. a band of light sweeps across the carving and its colours wake up
//   3. the carving is READ: the world's exposition, page by page under the tablet (ENTER = next page)
// Opened from the quest log (assemble = false) it shows the pieces you have; a finished one can be read again.
//
// World 1's tablet is "THE SENTENCE": the gods' eye over the land, a hooded warden carrying the harvest away, kneeling
// villagers in withered fields, and in the middle a dragon rising out of a cracked, dry riverbed.
public final class TabletScreen {
    private static final int PW = 480, PH = 300;   // the carving, in tablet pixels
    private static final int ASSEMBLE = 150, SWEEP = 60;

    private boolean open, assembling;
    private int world, t, page;
    private float typed;
    private final KeyLocker keys = new KeyLocker();
    private final List<float[]> dust = new ArrayList<>();
    private final Random rng = new Random();

    private static final String[] TITLES = { "", "THE SENTENCE", "THE BREAKING", "THE VERDICT" };
    private static final String[][] READING = { {},
            { "BEFORE THE WALLS THERE WAS THE COMPACT. THE BEASTS KEPT THE WATER AND THE SOIL, AND THE PEOPLE TOOK ONLY WHAT THEY NEEDED.",
                    "THE PEOPLE BROKE IT. THE GODS OF THE COUNCIL OF MERCY DID NOT STRIKE THEM DOWN. THEY ASKED A QUESTION INSTEAD, AND SENT A CHILD WITH NO PAST TO ANSWER IT.",
                    "THE CHILD ANSWERED. ITS SENTENCE BEGAN WITH HUNGER, BECAUSE HUNGER GIVES BACK WHAT WAS TAKEN: THE GRAIN RETURNS TO THE EARTH, THE WATER RETURNS TO THE DEEP, AND THE PEOPLE LEARN WHAT THE LAND FELT.",
                    "THE ONE WHO CARRIES THE BASKETS BELIEVES HE SERVES THE JUDGE. HE SERVES ONLY THE HUNGER.",
                    "THIS IS NOT THE END OF THE SENTENCE. IT IS THE FIRST WORD OF IT." },
            { "(THE WORLD 2 TABLET IS NOT CARVED YET.)" },
            { "(THE WORLD 3 TABLET IS NOT CARVED YET.)" } };

    public boolean isOpen() { return open; }

    public void open(int world, boolean assemble) {
        this.world = world;
        this.assembling = assemble && StoryState.tabletComplete(world);
        t = assembling ? 0 : ASSEMBLE + SWEEP;
        page = 0;
        typed = 0;
        dust.clear();
        open = true;
        keys.lockKey(Key.ENTER);
        keys.lockKey(Key.SPACE);
        keys.lockKey(Key.ESC);
        keys.lockKey(Key.Q);
    }

    private boolean tap(Key k) {
        if (Keyboard.isKeyUp(k)) keys.unlockKey(k);
        if (Keyboard.isKeyDown(k) && !keys.isKeyLocked(k)) { keys.lockKey(k); return true; }
        return false;
    }

    // returns false once it has closed
    public boolean update() {
        if (!open) return false;
        t++;
        for (float[] d : dust) { d[0] += d[2]; d[1] += d[3]; d[3] += 0.05f; d[4]--; }
        dust.removeIf(d -> d[4] <= 0);
        boolean complete = StoryState.tabletComplete(world);
        boolean reading = complete && t >= ASSEMBLE + SWEEP;
        String[] pages = READING[world];
        if (reading && page < pages.length) typed = Math.min(pages[page].length(), typed + 1.4f);
        boolean enter = tap(Key.ENTER) | tap(Key.SPACE), esc = tap(Key.ESC) | tap(Key.Q);
        if (esc && !assembling) { open = false; return false; }
        if (enter) {
            if (!reading) {
                if (t < ASSEMBLE + SWEEP && assembling) t = ASSEMBLE + SWEEP;   // skip the animation
                else if (!complete) { open = false; return false; }
            } else if (typed < pages[page].length()) typed = pages[page].length();
            else if (page < pages.length - 1) { page++; typed = 0; }
            else { open = false; return false; }
        }
        // the moment each piece lands: a puff of dust
        if (assembling) for (int i = 0; i < 6; i++) if (t == landAt(i)) puff(i);
        return true;
    }

    private static int landAt(int i) { return i == StoryState.KEYSTONE ? 128 : 20 + order(i) * 20; }

    private static int order(int i) { return i == 4 ? 4 : i; }

    // ================================================================= drawing

    public void draw(GraphicsHandler gh) {
        if (!open) return;
        int W = ScreenManager.getScreenWidth(), H = ScreenManager.getScreenHeight();
        Graphics2D g = gh.getGraphics();
        gh.drawFilledRectangle(0, 0, W, H, new Color(6, 4, 6, 230));
        // the tablet, scaled to fit the top ~62% of the screen
        float s = Math.min((W - 80) / (float) (PW + 80), (H * 0.62f) / (PH + 70));
        int tw = Math.round((PW + 80) * s), th = Math.round((PH + 70) * s);
        int tx = (W - tw) / 2, ty = 18;
        int shake = assembling && t >= landAt(StoryState.KEYSTONE) && t < landAt(StoryState.KEYSTONE) + 10 ? (t % 2 == 0 ? 4 : -4) : 0;
        tx += shake;
        drawTablet(g, world, tx, ty, tw, th, s, true);

        // dust
        for (float[] d : dust) {
            int a = Math.max(0, Math.min(255, (int) (d[4] * 6)));
            g.setColor(new Color(200, 170, 120, a));
            g.fillRect(Math.round(tx + d[0] * s), Math.round(ty + d[1] * s), 3, 3);
        }

        // the light sweep after the keystone lands
        int sw = t - ASSEMBLE;
        if (sw >= 0 && sw < SWEEP && StoryState.tabletComplete(world)) {
            int px0 = tx + Math.round((40 + (PW + 60) * sw / (float) SWEEP - 40) * s);
            Composite oc = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g.setPaint(new GradientPaint(px0 - 60, 0, new Color(255, 230, 160, 0), px0, 0, new Color(255, 236, 180, 220), true));
            g.fillRect(px0 - 60, ty, 120, th);
            g.setPaint(null);
            g.setComposite(oc);
        }

        // the title cartouche
        String title = TITLES[world];
        int cw = BattleUI.textW(title, 2) + 40, cy = ty + th + 8;
        g.setColor(new Color(60, 44, 26));
        g.fillRoundRect(W / 2 - cw / 2, cy, cw, 30, 26, 26);
        g.setColor(new Color(214, 178, 110));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(W / 2 - cw / 2, cy, cw, 30, 26, 26);
        g.setStroke(new BasicStroke(1));
        BattleUI.textCenter(gh, title, W / 2, cy + 8, 2, new Color(240, 210, 140));

        // the reading
        int py = cy + 40, ph = H - py - 14;
        gh.drawFilledRectangle(30, py, W - 60, ph, new Color(18, 14, 12, 240));
        gh.drawRectangle(30, py, W - 60, ph, new Color(110, 84, 50), 2);
        int n = StoryState.pieceCount(world);
        if (n < 6) {
            BattleUI.textCenter(gh, n + " OF 6 PIECES", W / 2, py + 18, 2, new Color(230, 200, 140));
            BattleUI.textCenter(gh, "THE CARVING CANNOT BE READ UNTIL IT IS WHOLE.", W / 2, py + 46, 1, new Color(170, 156, 136));
            BattleUI.textCenter(gh, "THE KEYSTONE IS CARRIED BY THIS WORLD'S BOSS.", W / 2, py + 62, 1, new Color(170, 156, 136));
            BattleUI.textCenter(gh, "ENTER / ESC: CLOSE", W / 2, py + ph - 20, 1, BattleUI.GREY);
            return;
        }
        if (t < ASSEMBLE + SWEEP) {
            if ((t / 20) % 2 == 0) BattleUI.textCenter(gh, "...", W / 2, py + 20, 2, new Color(170, 156, 136));
            BattleUI.textCenter(gh, "ENTER: SKIP", W / 2, py + ph - 20, 1, BattleUI.GREY);
            return;
        }
        String[] pages = READING[world];
        String text = pages[page].substring(0, Math.min(pages[page].length(), (int) typed));
        int chars = Math.max(20, (W - 100) / 12), ly = py + 16;
        for (String line : BattleUI.wrap(pages[page], chars)) {
            String part = line.substring(0, Math.max(0, Math.min(line.length(), text.length())));
            text = text.length() > line.length() ? text.substring(Math.min(text.length(), line.length() + 1)) : "";
            BattleUI.text(gh, part, 50, ly, 2, new Color(236, 220, 190));
            ly += 24;
        }
        BattleUI.textRight(gh, (page + 1) + "/" + pages.length, W - 46, py + ph - 20, 1, BattleUI.GREY);
        if (typed >= pages[page].length() && (t / 20) % 2 == 0)
            BattleUI.textCenter(gh, page < pages.length - 1 ? "ENTER: NEXT" : "ENTER: CLOSE", W / 2, py + ph - 20, 1, new Color(214, 178, 110));
    }

    private void puff(int i) {
        int[] c = pieceCentre(i);
        int count = i == StoryState.KEYSTONE ? 60 : 18;
        for (int k = 0; k < count; k++) {
            double a = rng.nextDouble() * Math.PI * 2, sp = 1 + rng.nextDouble() * (i == StoryState.KEYSTONE ? 4 : 2);
            dust.add(new float[]{ 40 + c[0], 35 + c[1], (float) (Math.cos(a) * sp), (float) (Math.sin(a) * sp) - 1, 30 + rng.nextInt(30) });
        }
    }

    // draws the stone slab with its border band of glyphs and the carving inside (pieces present, slots for the rest)
    private void drawTablet(Graphics2D g, int world, int x, int y, int w, int h, float s, boolean animate) {
        Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(slab(), x, y, w, h, null);
        int ix = x + Math.round(40 * s), iy = y + Math.round(35 * s), iw = Math.round(PW * s), ih = Math.round(PH * s);
        g.setColor(new Color(46, 32, 22));
        g.fillRect(ix - 3, iy - 3, iw + 6, ih + 6);
        BufferedImage[] pieces = pieces(world);
        int[][] box = boxes(world);
        for (int i = 0; i < 6; i++) {
            int bx = ix + Math.round(box[i][0] * s), by = iy + Math.round(box[i][1] * s);
            int bw = Math.round(box[i][2] * s), bh = Math.round(box[i][3] * s);
            g.drawImage(slot(world)[i], bx, by, bw, bh, null);   // the empty recess
            if (!StoryState.hasPiece(world, i)) continue;
            float k = 1;
            if (animate && assembling) {
                int start = landAt(i) - 24;
                k = Math.max(0, Math.min(1, (t - start) / 24f));
                if (k <= 0) continue;
            }
            float e = 1 - (1 - k) * (1 - k) * (1 - k);
            int[] from = flyFrom(i);
            int ox = Math.round(from[0] * (1 - e) * s), oy = Math.round(from[1] * (1 - e) * s);
            g.drawImage(pieces[i], bx + ox, by + oy, bw, bh, null);
        }
        // the colours waking up during the sweep: pigment shows only once the tablet has been swept
        if (StoryState.tabletComplete(world) && (!assembling || t >= ASSEMBLE + SWEEP / 2)) {
            float a = !assembling ? 1f : Math.min(1f, (t - ASSEMBLE - SWEEP / 2) / (float) (SWEEP / 2));
            Composite oc = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.drawImage(pigment(world), ix, iy, iw, ih, null);
            g.setComposite(oc);
        }
        if (hint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
    }

    // a small view of a tablet for the quest log
    static void drawThumbnail(GraphicsHandler gh, int world, int x, int y, int w, int h) {
        Graphics2D g = gh.getGraphics();
        Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(slab(), x, y, w, h, null);
        float sx = w / (float) (PW + 80), sy = h / (float) (PH + 70);
        int ix = x + Math.round(40 * sx), iy = y + Math.round(35 * sy);
        int[][] box = boxes(world);
        for (int i = 0; i < 6; i++) {
            int bx = ix + Math.round(box[i][0] * sx), by = iy + Math.round(box[i][1] * sy);
            int bw = Math.round(box[i][2] * sx), bh = Math.round(box[i][3] * sy);
            g.drawImage(StoryState.hasPiece(world, i) ? pieces(world)[i] : slot(world)[i], bx, by, bw, bh, null);
        }
        if (world > 1 && StoryState.pieceCount(world) == 0) BattleUI.textCenter(gh, "?", x + w / 2, y + h / 2 - 8, 3, new Color(90, 70, 50));
        if (hint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
    }

    // where each piece flies in from (tablet pixels, relative to where it lands)
    private static int[] flyFrom(int i) {
        switch (i) {
            case 0: return new int[]{ -320, -220 };
            case 1: return new int[]{ -340, 260 };
            case 2: return new int[]{ 340, -240 };
            case 3: return new int[]{ 330, 260 };
            case 4: return new int[]{ 0, 340 };
            default: return new int[]{ 0, -420 };
        }
    }

    private static int[] pieceCentre(int i) {
        int[] b = boxes(1)[i];
        return new int[]{ b[0] + b[2] / 2, b[1] + b[3] / 2 };
    }

    // ================================================================= the art (built once)

    private static BufferedImage slabImg;
    private static final BufferedImage[][] PIECES = new BufferedImage[4][], SLOTS = new BufferedImage[4][];
    private static final BufferedImage[] PIGMENT = new BufferedImage[4];
    private static final int[][][] BOXES = new int[4][][];
    private static int[] owner;   // which piece each carving pixel belongs to

    // the jagged crack lines between the pieces (tablet pixels)
    private static int jag(int v, int seed) {
        long h = (v / 6) * 2654435761L + seed * 97L;
        return (int) ((h >>> 7) % 13) - 6;
    }

    // piece of a carving pixel: 0 left-top, 1 left-bottom, 2 right-top, 3 right-bottom, 4 centre-bottom, 5 keystone (centre)
    private static int pieceAt(int x, int y) {
        int left = 144 + jag(y, 1), right = 336 + jag(y, 2);
        if (x < left) return y < 150 + jag(x, 3) ? 0 : 1;
        if (x >= right) return y < 150 + jag(x, 4) ? 2 : 3;
        return y < 214 + jag(x, 5) ? 5 : 4;
    }

    private static void buildOwner() {
        if (owner != null) return;
        owner = new int[PW * PH];
        for (int y = 0; y < PH; y++) for (int x = 0; x < PW; x++) owner[y * PW + x] = pieceAt(x, y);
    }

    private static int[][] boxes(int world) {
        if (BOXES[world] == null) {
            buildOwner();
            int[][] b = new int[6][];
            int[] x0 = { PW, PW, PW, PW, PW, PW }, y0 = { PH, PH, PH, PH, PH, PH }, x1 = new int[6], y1 = new int[6];
            for (int y = 0; y < PH; y++) for (int x = 0; x < PW; x++) {
                int p = owner[y * PW + x];
                x0[p] = Math.min(x0[p], x); y0[p] = Math.min(y0[p], y); x1[p] = Math.max(x1[p], x); y1[p] = Math.max(y1[p], y);
            }
            for (int i = 0; i < 6; i++) b[i] = new int[]{ x0[i], y0[i], x1[i] - x0[i] + 1, y1[i] - y0[i] + 1 };
            BOXES[world] = b;
        }
        return BOXES[world];
    }

    private static BufferedImage[] pieces(int world) {
        if (PIECES[world] == null) cut(world);
        return PIECES[world];
    }

    private static BufferedImage[] slot(int world) {
        if (SLOTS[world] == null) cut(world);
        return SLOTS[world];
    }

    private static BufferedImage pigment(int world) {
        if (PIGMENT[world] == null) cut(world);
        return PIGMENT[world];
    }

    // cuts the carving into its 6 pieces (with a dark crack along each edge) and makes the matching empty slots
    private static void cut(int world) {
        buildOwner();
        int[][] box = boxes(world);
        BufferedImage[] carving = carve(world);
        BufferedImage pic = carving[0];
        BufferedImage[] ps = new BufferedImage[6], ss = new BufferedImage[6];
        for (int i = 0; i < 6; i++) {
            int[] b = box[i];
            BufferedImage p = new BufferedImage(b[2], b[3], BufferedImage.TYPE_INT_ARGB);
            BufferedImage sl = new BufferedImage(b[2], b[3], BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < b[3]; y++) for (int x = 0; x < b[2]; x++) {
                int gx = b[0] + x, gy = b[1] + y;
                if (owner[gy * PW + gx] != i) continue;
                boolean edge = false;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = gx + d[0], ny = gy + d[1];
                    if (nx >= 0 && ny >= 0 && nx < PW && ny < PH && owner[ny * PW + nx] != i) edge = true;
                }
                p.setRGB(x, y, edge ? 0xFF2E2016 : pic.getRGB(gx, gy));
                // the empty slot: a dark recess, a little lighter toward its lower right
                int shade = 30 + (x + y) % 3 + (edge ? -10 : 0);
                sl.setRGB(x, y, 0xFF000000 | (shade + 12) << 16 | (shade + 4) << 8 | shade);
            }
            ps[i] = p;
            ss[i] = sl;
        }
        PIECES[world] = ps;
        SLOTS[world] = ss;
        PIGMENT[world] = carving[1];
    }

    // the stone slab: weathered sandstone with chipped edges and a border band of small carved glyphs
    private static BufferedImage slab() {
        if (slabImg != null) return slabImg;
        int w = PW + 80, h = PH + 70;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Random r = new Random(5);
        Path2D.Float outline = new Path2D.Float();
        outline.moveTo(6, 10);
        for (int x = 6; x < w - 6; x += 14) outline.lineTo(x, 4 + r.nextInt(8));
        for (int y = 10; y < h - 6; y += 14) outline.lineTo(w - 4 - r.nextInt(7), y);
        for (int x = w - 10; x > 6; x -= 14) outline.lineTo(x, h - 3 - r.nextInt(8));
        for (int y = h - 10; y > 10; y -= 14) outline.lineTo(3 + r.nextInt(7), y);
        outline.closePath();
        g.setColor(new Color(48, 34, 22));
        g.translate(4, 5);
        g.fill(outline);
        g.translate(-4, -5);
        g.setPaint(new GradientPaint(0, 0, new Color(206, 176, 124), 0, h, new Color(160, 128, 84)));
        g.fill(outline);
        g.setPaint(null);
        // grain and pits
        for (int i = 0; i < 1400; i++) {
            int x = r.nextInt(w), y = r.nextInt(h);
            if (!outline.contains(x, y)) continue;
            g.setColor(r.nextBoolean() ? new Color(150, 118, 76, 90) : new Color(232, 206, 158, 70));
            g.fillRect(x, y, 1 + r.nextInt(2), 1);
        }
        // the glyph band: two rows of small carved signs above and below the picture
        int[] rows = { 12, h - 30 };
        for (int ry : rows)
            for (int i = 0, gx = 40; gx < w - 50; gx += 26, i++) glyph(g, i % 7, gx, ry, r);
        // side columns
        for (int gy = 50; gy < h - 50; gy += 26) { glyph(g, (gy / 26) % 7, 8, gy, r); glyph(g, (gy / 26 + 3) % 7, w - 30, gy, r); }
        g.dispose();
        slabImg = img;
        return img;
    }

    // one small carved sign (20x16): sun, eye, water, reed, bird, hand, a tally of marks
    private static void glyph(Graphics2D g, int kind, int x, int y, Random r) {
        Color deep = new Color(96, 70, 42), lite = new Color(230, 204, 156);
        g.setStroke(new BasicStroke(2));
        for (int pass = 0; pass < 2; pass++) {
            g.setColor(pass == 0 ? lite : deep);
            int o = pass == 0 ? 1 : 0;
            switch (kind) {
                case 0: g.drawOval(x + 4 + o, y + 2 + o, 12, 12); g.fillOval(x + 9 + o, y + 7 + o, 3, 3); break;     // sun
                case 1: g.drawArc(x + 2 + o, y + 4 + o, 16, 10, 0, 180); g.drawArc(x + 2 + o, y + 2 + o, 16, 10, 180, 180);
                    g.fillOval(x + 8 + o, y + 6 + o, 4, 4); break;                                                       // eye
                case 2: for (int k = 0; k < 2; k++) g.drawPolyline(new int[]{ x + 2 + o, x + 6 + o, x + 10 + o, x + 14 + o, x + 18 + o },
                        new int[]{ y + 5 + k * 6 + o, y + 2 + k * 6 + o, y + 5 + k * 6 + o, y + 2 + k * 6 + o, y + 5 + k * 6 + o }, 5); break;   // water
                case 3: g.drawLine(x + 10 + o, y + 1 + o, x + 10 + o, y + 15 + o); g.drawLine(x + 10 + o, y + 4 + o, x + 15 + o, y + 1 + o); break; // reed
                case 4: g.drawArc(x + 3 + o, y + 4 + o, 10, 8, 0, 180); g.drawLine(x + 13 + o, y + 8 + o, x + 18 + o, y + 6 + o);
                    g.drawLine(x + 6 + o, y + 12 + o, x + 6 + o, y + 15 + o); break;                                    // bird
                case 5: g.drawLine(x + 3 + o, y + 8 + o, x + 13 + o, y + 8 + o); for (int k = 0; k < 3; k++)
                    g.drawLine(x + 13 + o, y + 8 + o, x + 18 + o, y + 4 + k * 4 + o); break;                           // hand
                default: for (int k = 0; k < 4; k++) g.drawLine(x + 4 + k * 4 + o, y + 2 + o, x + 4 + k * 4 + o, y + 14 + o);
                    g.drawLine(x + 2 + o, y + 12 + o, x + 18 + o, y + 4 + o); break;                                     // tally
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    // ---------------------------------------------------------------- the carving of each world
    // returns { the carving (sandstone relief), its pigment layer (red ochre / faience blue / gold, shown once read) }
    private static BufferedImage[] carve(int world) {
        BufferedImage pic = new BufferedImage(PW, PH, BufferedImage.TYPE_INT_ARGB);
        BufferedImage pig = new BufferedImage(PW, PH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = pic.createGraphics(), p = pig.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setPaint(new GradientPaint(0, 0, new Color(214, 184, 132), 0, PH, new Color(184, 150, 102)));
        g.fillRect(0, 0, PW, PH);
        g.setPaint(null);
        Random r = new Random(11);
        for (int i = 0; i < 900; i++) {
            g.setColor(r.nextBoolean() ? new Color(160, 128, 84, 80) : new Color(236, 212, 166, 60));
            g.fillRect(r.nextInt(PW), r.nextInt(PH), 1 + r.nextInt(2), 1);
        }
        if (world == 1) carveSentence(g, p);
        else {   // Worlds 2 and 3: not carved yet, just the frame lines
            relief(g, new Rectangle(20, 20, PW - 40, PH - 40));
        }
        g.dispose();
        p.dispose();
        return new BufferedImage[]{ pic, pig };
    }

    private static final Color FILL = new Color(196, 162, 110), DEEP = new Color(92, 64, 38), LITE = new Color(236, 212, 164);

    // draws a shape as carved relief: a dark cut below/right, a light edge above/left, the raised face in between
    private static void relief(Graphics2D g, Shape s) {
        g.translate(2, 2);
        g.setColor(DEEP);
        g.fill(s);
        g.translate(-3, -3);
        g.setColor(LITE);
        g.fill(s);
        g.translate(1, 1);
        g.setColor(FILL);
        g.fill(s);
    }

    // a carved line (an incised groove)
    private static void groove(Graphics2D g, int x0, int y0, int x1, int y1, float w) {
        g.setStroke(new BasicStroke(w + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(LITE);
        g.drawLine(x0 + 1, y0 + 1, x1 + 1, y1 + 1);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(DEEP);
        g.drawLine(x0, y0, x1, y1);
        g.setStroke(new BasicStroke(1));
    }

    private static Shape poly(int... xy) {
        Path2D.Float s = new Path2D.Float();
        s.moveTo(xy[0], xy[1]);
        for (int i = 2; i + 1 < xy.length; i += 2) s.lineTo(xy[i], xy[i + 1]);
        s.closePath();
        return s;
    }

    private static void paint(Graphics2D p, Shape s, Color c) {
        p.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
        p.fill(s);
    }

    // WORLD 1: THE SENTENCE
    private static void carveSentence(Graphics2D g, Graphics2D p) {
        Color OCHRE = new Color(176, 58, 36), BLUE = new Color(52, 98, 142), GOLDP = new Color(214, 170, 64), BLACK = new Color(40, 28, 22);
        // the ground line and the register line under the sky
        groove(g, 8, 268, PW - 8, 268, 3);
        groove(g, 8, 44, PW - 8, 44, 2);
        // the top register: a row of small signs (repeating the border)
        Random r = new Random(3);
        for (int x = 14; x < PW - 20; x += 24) glyph(g, (x / 24) % 7, x, 20, r);

        // PIECE 0 (left top): the gods' eye inside the sun, its rays ending in open hands, reaching down over the land
        Shape sun = new java.awt.geom.Ellipse2D.Float(40, 58, 56, 56);
        relief(g, sun);
        paint(p, sun, GOLDP);
        Shape eyeW = new java.awt.geom.Ellipse2D.Float(50, 76, 36, 18);
        g.setColor(LITE); g.fill(eyeW);
        groove(g, 50, 85, 86, 85, 1);
        g.setColor(DEEP); g.fillOval(62, 78, 12, 12);
        paint(p, new java.awt.geom.Ellipse2D.Float(64, 80, 8, 8), BLUE);
        for (int k = 0; k < 6; k++) {
            int x0 = 52 + k * 7, x1 = 18 + k * 22;
            groove(g, x0, 114, x1, 140, 2);
            relief(g, new java.awt.geom.Ellipse2D.Float(x1 - 4, 138, 9, 7));   // a small open hand at the end of each ray
        }

        // PIECE 1 (left bottom): withered wheat and three villagers kneeling, arms raised
        for (int k = 0; k < 7; k++) {
            int x = 14 + k * 18;
            groove(g, x, 266, x + 4, 200 + (k % 3) * 8, 2);
            groove(g, x + 4, 200 + (k % 3) * 8, x + 10, 212 + (k % 3) * 8, 2);   // the head droops
        }
        for (int k = 0; k < 3; k++) {
            int x = 18 + k * 38, y = 236;
            Shape body = poly(x, y + 30, x + 6, y + 6, x + 18, y + 4, x + 22, y + 30);
            relief(g, body);
            relief(g, new java.awt.geom.Ellipse2D.Float(x + 8, y - 8, 12, 12));
            groove(g, x + 16, y + 6, x + 30, y - 14, 2);   // raised arm
            paint(p, body, k == 1 ? OCHRE : BLUE);
        }

        // KEYSTONE (centre): the dragon rising from the river, wings spread, the judge
        Shape wingL = poly(238, 120, 154, 58, 176, 96, 150, 104, 180, 124, 156, 140, 226, 148);
        Shape wingR = poly(262, 120, 346, 58, 324, 96, 350, 104, 320, 124, 344, 140, 274, 148);
        relief(g, wingL);
        relief(g, wingR);
        paint(p, wingL, BLACK);
        paint(p, wingR, BLACK);
        for (int k = 0; k < 3; k++) {   // wing ribs
            groove(g, 236, 124, 162 + k * 10, 66 + k * 28, 1.5f);
            groove(g, 264, 124, 338 - k * 10, 66 + k * 28, 1.5f);
        }
        Shape body = poly(232, 214, 236, 150, 244, 110, 256, 110, 264, 150, 268, 214);
        relief(g, body);
        paint(p, body, BLACK);
        for (int y = 130; y < 210; y += 10) groove(g, 240, y, 260, y, 1);   // belly scales
        Shape neck = poly(244, 112, 238, 86, 246, 66, 258, 64, 262, 86, 256, 112);
        relief(g, neck);
        paint(p, neck, BLACK);
        Shape head = poly(240, 70, 248, 52, 266, 54, 282, 64, 266, 72, 254, 74);
        relief(g, head);
        paint(p, head, BLACK);
        groove(g, 250, 54, 240, 40, 2);   // horns
        groove(g, 258, 54, 256, 38, 2);
        g.setColor(DEEP); g.fillOval(256, 58, 6, 4);
        paint(p, new java.awt.geom.Ellipse2D.Float(256, 58, 6, 4), new Color(230, 30, 20));
        // the water spilling down off its body (the river taken down into the earth)
        for (int k = 0; k < 4; k++) groove(g, 226 - k * 6, 196 + k * 4, 214 - k * 8, 214, 1.5f);
        for (int k = 0; k < 4; k++) groove(g, 274 + k * 6, 196 + k * 4, 286 + k * 8, 214, 1.5f);

        // PIECE 4 (centre bottom): the riverbed, its wave signs broken by cracks
        for (int row = 0; row < 3; row++) {
            int y = 226 + row * 14;
            for (int x = 156; x < 324; x += 16) {
                if ((x / 16 + row) % 4 == 1) continue;   // broken: missing waves
                groove(g, x, y + 4, x + 4, y, 2);
                groove(g, x + 4, y, x + 8, y + 4, 2);
                paint(p, new Rectangle(x, y, 9, 5), BLUE);
            }
        }
        groove(g, 190, 222, 206, 266, 1.5f);
        groove(g, 206, 266, 198, 240, 1);
        groove(g, 290, 220, 276, 264, 1.5f);

        // PIECE 2 (right top): the hooded warden, walking away with the harvest in a basket on his back
        Shape cloak = poly(380, 146, 390, 84, 404, 68, 420, 70, 428, 92, 436, 146);
        relief(g, cloak);
        paint(p, cloak, new Color(90, 60, 40));
        Shape hood = poly(398, 84, 404, 62, 420, 60, 426, 80);
        relief(g, hood);
        Shape basket = poly(422, 72, 458, 70, 454, 102, 426, 104);
        relief(g, basket);
        paint(p, basket, GOLDP);
        for (int k = 0; k < 3; k++) groove(g, 424, 80 + k * 8, 456, 80 + k * 8, 1);
        for (int k = 0; k < 5; k++) relief(g, new java.awt.geom.Ellipse2D.Float(426 + k * 6, 62 - (k % 2) * 3, 7, 7));   // grain heaped up
        groove(g, 384, 146, 376, 146, 2);
        groove(g, 470, 140, 444, 140, 1);   // footprints walking away

        // PIECE 3 (right bottom): the emptied land: a tally of marks (the count), and an empty basket, upturned
        for (int k = 0; k < 12; k++) {
            int x = 352 + (k % 6) * 18, y = 170 + (k / 6) * 40;
            for (int m = 0; m < 4; m++) groove(g, x + m * 3, y, x + m * 3, y + 22, 1.5f);
            groove(g, x - 2, y + 18, x + 12, y + 4, 1.5f);
        }
        Shape empty = poly(380, 266, 384, 246, 420, 246, 424, 266);
        relief(g, empty);
        paint(p, empty, OCHRE);
    }
}