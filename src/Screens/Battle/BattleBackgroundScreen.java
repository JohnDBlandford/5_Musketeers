package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// The animated battle backdrop. Every world is a detailed pixel-art scene that is painted in code (no image files needed):
// dithered skies, drifting clouds, shaded mountains, timber houses, trees that sway, perspective ground and a few living details.
// It is drawn at half resolution and scaled up without smoothing so the pixels stay crisp.
//
//   WORLD_1  The Reckoning (opening)    a starving, half-dead village under a dust-brown sky
//   WORLD_2  The Long Erosion           the old green world, still colourful, with lake, mountains and wheat
//   WORLD_3  The Age of Watching        a village square that slowly shifts between festival warmth and plague-village dread
//   WORLD_4  The Reckoning (final)      a blood sky with a black eclipse over the ruined gate of Dabbah's Hollow
//
// It is a FINAL class that extends PlayBattleScreen (as the design asks) but only overrides the three screen methods to draw the backdrop;
// it uses PlayBattleScreen's protected no-argument constructor, so building it does not build another battle (no endless loop).
public final class BattleBackgroundScreen extends PlayBattleScreen {
    private static final int SCALE = 2;
    private static final int HZ = 96;                 // the horizon row in the half-size picture

    private final BattleWorld world;
    private int w, h, lw, lh, frame;
    private BufferedImage lowRes;
    private Graphics2D lg;
    private Scene[] scenes;                           // one scene, or two (festival and plague) for world 3
    private BufferedImage vignette;

    private interface Anim { void draw(Graphics2D g, int t); }

    private static final class Scene {
        BufferedImage sky, clouds, scenery;
        double cloudSpeed = 0.1;
        final List<Anim> anims = new ArrayList<>();
        BufferedImage out;
        Graphics2D og;
        int[] glowPixels = new int[0];                // lava pixels that pulse (world 4)
        int glowColor;
    }

    public BattleBackgroundScreen(BattleWorld world) {
        super();
        this.world = world;
    }

    @Override
    public void initialize() {
        frame = 0;
        w = BattleUI.W();
        h = BattleUI.H();
        lw = w / SCALE;
        lh = h / SCALE;
        lowRes = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
        lg = lowRes.createGraphics();
        switch (world) {
            case WORLD_2: scenes = new Scene[]{buildWorld2()}; break;
            case WORLD_3: scenes = new Scene[]{buildWorld3(false), buildWorld3(true)}; break;
            case WORLD_4: scenes = new Scene[]{buildWorld4()}; break;
            default: scenes = new Scene[]{buildWorld1()}; break;
        }
        for (Scene s : scenes) {
            s.out = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
            s.og = s.out.createGraphics();
        }
        vignette = makeVignette();
    }

    @Override
    public void update() {
        frame++;
    }

    @Override
    public void draw(GraphicsHandler gh) {
        Graphics2D g = gh.getGraphics();
        for (Scene s : scenes) renderScene(s, frame);
        lg.setComposite(AlphaComposite.Src);
        lg.drawImage(scenes[0].out, 0, 0, null);
        if (scenes.length > 1) {
            lg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) mood()));
            lg.drawImage(scenes[1].out, 0, 0, null);
        }
        lg.setComposite(AlphaComposite.SrcOver);

        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(lowRes, 0, 0, w, h, null);
        if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        g.drawImage(vignette, 0, 0, null);
    }

    // world 3 only: 0 = festival, 1 = plague. Holds each for a while, fades slowly between them.
    private double mood() {
        int period = 1500, t = frame % period;
        double p = t / (double) period;
        double k = 0.5 - 0.5 * Math.cos(p * 2 * Math.PI);   // 0 -> 1 -> 0
        return Math.min(1, Math.max(0, (k - 0.25) / 0.5));  // flat tops and bottoms
    }

    private double heartbeat() {
        int t = frame % 96;
        if (t < 8) return Math.sin(Math.PI * t / 8.0);
        if (t >= 14 && t < 22) return 0.6 * Math.sin(Math.PI * (t - 14) / 8.0);
        return 0;
    }

    private void renderScene(Scene s, int t) {
        Graphics2D g = s.og;
        g.setComposite(AlphaComposite.Src);
        g.drawImage(s.sky, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver);
        int cw = s.clouds.getWidth();
        int off = (int) ((t * s.cloudSpeed) % cw);
        g.drawImage(s.clouds, -off, 0, null);
        g.drawImage(s.clouds, cw - off, 0, null);
        g.drawImage(s.scenery, 0, 0, null);
        if (s.glowPixels.length > 0) {
            double pulse = 0.55 + 0.45 * Math.sin(t * 0.05) + 0.35 * heartbeat();
            int base = s.glowColor;
            int r = clamp((int) (((base >> 16) & 255) * pulse), 0, 255), gr = clamp((int) (((base >> 8) & 255) * pulse), 0, 255), b = clamp((int) ((base & 255) * pulse), 0, 255);
            g.setColor(new Color(r, gr, b));
            for (int i = 0; i < s.glowPixels.length; i++) {
                int p = s.glowPixels[i];
                g.fillRect(p % lw, p / lw, 1, 1);
            }
        }
        for (Anim a : s.anims) a.draw(g, t);
    }

    // ================================================================= small helpers

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static int pmod(int a, int m) { return ((a % m) + m) % m; }
    private static double pmod(double a, double m) { return ((a % m) + m) % m; }

    private static int lerp(int c1, int c2, double t) {
        t = clamp01(t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int mul(int c, double f) {
        int r = clamp((int) (((c >> 16) & 255) * f), 0, 255), g = clamp((int) (((c >> 8) & 255) * f), 0, 255), b = clamp((int) ((c & 255) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static double hash(int x, int y, int seed) {
        int hh = x * 374761393 + y * 668265263 + seed * 1442695041;
        hh = (hh ^ (hh >>> 13)) * 1274126177;
        hh ^= hh >>> 16;
        return (hh & 0xFFFFFF) / (double) 0x1000000;
    }

    private static double vn(double x, double y, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi, seed), b = hash(xi + 1, yi, seed), c = hash(xi, yi + 1, seed), d = hash(xi + 1, yi + 1, seed);
        double top = a + (b - a) * fx, bot = c + (d - c) * fx;
        return top + (bot - top) * fy;
    }

    // value noise that repeats every `per` lattice cells in x (so the cloud strip tiles without a seam)
    private static double vnp(double x, double y, int per, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        int x0 = pmod(xi, per), x1 = pmod(xi + 1, per);
        double a = hash(x0, yi, seed), b = hash(x1, yi, seed), c = hash(x0, yi + 1, seed), d = hash(x1, yi + 1, seed);
        double top = a + (b - a) * fx, bot = c + (d - c) * fx;
        return top + (bot - top) * fy;
    }

    private static double fbm(double x, double y, int seed, int oct) {
        double sum = 0, amp = 0.5, norm = 0;
        for (int o = 0; o < oct; o++) {
            sum += amp * vn(x, y, seed + o * 17);
            norm += amp;
            x *= 2;
            y *= 2;
            amp *= 0.5;
        }
        return sum / norm;
    }

    private static double fbmp(double u, double v, int per, int seed, int oct) {
        double sum = 0, amp = 0.5, norm = 0;
        int f = 1;
        for (int o = 0; o < oct; o++) {
            sum += amp * vnp(u * f, v * f, per * f, seed + o * 31);
            norm += amp;
            amp *= 0.5;
            f *= 2;
        }
        return sum / norm;
    }

    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
    private static double bay(int x, int y) { return (BAYER[y & 3][x & 3] + 0.5) / 16.0; }

    // pick a colour from a ramp for t in 0..1, dithering between neighbours so there are no smooth blends (pixel-art look)
    private static int ramp(int[] r, double t, int x, int y) {
        t = clamp01(t) * (r.length - 1);
        int i = Math.min(r.length - 2, (int) t);
        double f = t - i;
        return f > bay(x, y) ? r[i + 1] : r[i];
    }

    // ================================================================= the pixel canvas

    private final class P {
        final int pw, ph;
        final int[] a;

        P(int pw, int ph) { this.pw = pw; this.ph = ph; a = new int[pw * ph]; }

        void set(int x, int y, int rgb) { if (x >= 0 && y >= 0 && x < pw && y < ph) a[y * pw + x] = 0xFF000000 | rgb; }
        void erase(int x, int y) { if (x >= 0 && y >= 0 && x < pw && y < ph) a[y * pw + x] = 0; }
        boolean has(int x, int y) { return x >= 0 && y >= 0 && x < pw && y < ph && (a[y * pw + x] >>> 24) != 0; }
        int get(int x, int y) { return (x >= 0 && y >= 0 && x < pw && y < ph) ? a[y * pw + x] & 0xFFFFFF : 0; }

        void rect(int x, int y, int rw, int rh, int rgb) {
            for (int j = y; j < y + rh; j++) for (int i = x; i < x + rw; i++) set(i, j, rgb);
        }

        void disc(int cx, int cy, double r, int rgb) {
            for (int j = (int) -r; j <= r; j++) for (int i = (int) -r; i <= r; i++) if (i * i + j * j <= r * r) set(cx + i, cy + j, rgb);
        }

        void line(int x0, int y0, int x1, int y1, int rgb) {
            int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
            while (true) {
                set(x0, y0, rgb);
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 >= dy) { err += dy; x0 += sx; }
                if (e2 <= dx) { err += dx; y0 += sy; }
            }
        }

        void thickLine(int x0, int y0, int x1, int y1, double th, int rgb) {
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
            if (steps == 0) { disc(x0, y0, th / 2, rgb); return; }
            for (int i = 0; i <= steps; i++) {
                double f = i / (double) steps;
                disc((int) Math.round(x0 + (x1 - x0) * f), (int) Math.round(y0 + (y1 - y0) * f), th / 2, rgb);
            }
        }

        void darken(int x, int y, double f) {
            if (has(x, y)) { int c = a[y * pw + x] & 0xFFFFFF; a[y * pw + x] = 0xFF000000 | mul(c, f); }
        }

        void tint(int x, int y, int rgb, double t) {
            if (has(x, y)) { int c = a[y * pw + x] & 0xFFFFFF; a[y * pw + x] = 0xFF000000 | lerp(c, rgb, t); }
        }

        // light from the upper left: edges that face it get the highlight colour, edges that face away get the shade colour
        void rim(int hi, int lo) {
            int[] src = a.clone();
            for (int y = 0; y < ph; y++) for (int x = 0; x < pw; x++) {
                if ((src[y * pw + x] >>> 24) == 0) continue;
                boolean l = x == 0 || (src[y * pw + x - 1] >>> 24) == 0, u = y == 0 || (src[(y - 1) * pw + x] >>> 24) == 0;
                boolean r = x == pw - 1 || (src[y * pw + x + 1] >>> 24) == 0, d = y == ph - 1 || (src[(y + 1) * pw + x] >>> 24) == 0;
                if (l || u) a[y * pw + x] = 0xFF000000 | hi;
                else if (r || d) a[y * pw + x] = 0xFF000000 | lo;
            }
        }

        BufferedImage image() {
            BufferedImage b = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            b.setRGB(0, 0, pw, ph, a, 0, pw);
            return b;
        }
    }

    // ================================================================= generators

    private BufferedImage skyImage(int[] ramp) {
        P p = new P(lw, lh);
        for (int y = 0; y < lh; y++) for (int x = 0; x < lw; x++) p.set(x, y, ramp(ramp, y / (double) (HZ + 6), x, y));
        return p.image();
    }

    // a wide, seamless strip of clouds (tones: shadow, body, light, highlight)
    private BufferedImage cloudStrip(int seed, double cover, int[] tones, int yTop, int yBot, double stretch) {
        int cw = lw * 2, ch = HZ + 10;
        double[][] d = new double[ch][cw];
        for (int y = 0; y < ch; y++) {
            double env = (y < yTop || y > yBot) ? 0 : Math.pow(Math.sin(Math.PI * (y - yTop) / (double) (yBot - yTop)), 0.8);
            for (int x = 0; x < cw; x++) {
                double n = fbmp(x / (double) cw, y / stretch, 6, seed, 4);
                d[y][x] = n * (0.3 + 0.7 * env);
            }
        }
        P p = new P(cw, ch);
        int[][] up = new int[ch][cw], down = new int[ch][cw];
        for (int x = 0; x < cw; x++) {
            int run = 0;
            for (int y = 0; y < ch; y++) { run = d[y][x] > cover ? run + 1 : 0; up[y][x] = run; }
            run = 0;
            for (int y = ch - 1; y >= 0; y--) { run = d[y][x] > cover ? run + 1 : 0; down[y][x] = run; }
        }
        for (int y = 0; y < ch; y++) for (int x = 0; x < cw; x++) {
            if (d[y][x] <= cover) continue;
            int u = up[y][x], dn = down[y][x];
            int c;
            if (u <= 1) c = tones[3];
            else if (u <= 3) c = bay(x, y) > 0.5 ? tones[3] : tones[2];
            else if (u <= 6) c = tones[2];
            else if (dn <= 2) c = tones[0];
            else if (dn <= 5) c = bay(x, y) > 0.5 ? tones[0] : tones[1];
            else c = (u < 9 && bay(x, y) > 0.7) ? tones[2] : tones[1];
            p.set(x, y, c);
        }
        return p.image();
    }

    // rows of mountains. tones: shadow, body, lit. Snow (optional) on the high faces.
    private void mountains(P p, int seed, int base, double amp, double freq, int[] tones, int snow, int snowShade) {
        int[] top = new int[lw];
        for (int x = 0; x < lw; x++) {
            double r = 1 - Math.abs(2 * fbm(x * freq, seed * 3.7, seed, 4) - 1);
            top[x] = (int) (base - amp * Math.pow(r, 1.3));
        }
        for (int x = 0; x < lw; x++) {
            int xl = Math.max(0, x - 2), xr = Math.min(lw - 1, x + 2);
            double slope = top[xr] - top[xl];
            boolean lit = slope < 0.5;
            for (int y = top[x]; y < lh; y++) {
                double depth = (y - top[x]);
                double n = vn(x * 0.22, y * 0.5, seed + 3);
                double t = (lit ? 0.7 : 0.18) + (n - 0.5) * 0.55 - Math.min(depth / 70.0, 0.35);
                int c = ramp(tones, t, x, y);
                if (snow != 0 && top[x] < base - amp * 0.5 && depth < 5 + n * 12 + (base - amp * 0.5 - top[x]) * 0.35 && depth < 26) {
                    c = lit ? (n > 0.62 && depth > 8 ? snowShade : snow) : snowShade;
                }
                p.set(x, y, c);
            }
        }
    }

    // round clumps of treetops along a ridge line to make forested hills
    private void forestEdge(P p, int seed, int[] greens, int y0, int y1) {
        Random r = new Random(seed);
        for (int x = 0; x < lw; x += 3 + r.nextInt(3)) {
            int top = -1;
            for (int y = y0; y < y1; y++) if (p.has(x, y)) { top = y; break; }
            if (top < 0) continue;
            int rad = 3 + r.nextInt(3);
            for (int j = -rad; j <= rad; j++) for (int i = -rad; i <= rad; i++) {
                if (i * i + j * j > rad * rad) continue;
                double light = (-i * 0.5 - j * 0.8) / rad;
                p.set(x + i, top + j + 1, ramp(greens, 0.45 + light * 0.4 + (hash(x + i, top + j, seed) - 0.5) * 0.35, x + i, top + j));
            }
        }
    }

    // soft haze along the horizon so layers sit back in the air
    private void haze(P p, int y0, int y1, int rgb, double peak) {
        for (int y = y0; y <= y1; y++) {
            double t = 1 - Math.abs((y - (y0 + y1) / 2.0)) / ((y1 - y0) / 2.0);
            double s = clamp01(t) * peak;
            for (int x = 0; x < lw; x++) if (p.has(x, y) && s > bay(x, y)) p.set(x, y, rgb);
        }
    }

    // perspective ground. kind: 0 dry earth with cracks, 1 grass and dirt path, 2 cobblestones, 3 scorched stone with lava
    private void ground(P p, int seed, int kind, int[] cols, int[] alt, List<Integer> glow) {
        for (int y = HZ; y < lh; y++) {
            double dy = y - HZ + 5;
            double wz = Math.log(dy / 4.0) * 10;
            double fade = clamp01((dy - 9) / 26.0);
            for (int x = 0; x < lw; x++) {
                double wx = (x - lw / 2.0) * 10.0 / dy;
                double detail = fbm(wx * 0.5 + 40, wz * 0.5, seed, 3);
                int c;
                double pc = Math.sin(wz * 0.33 + seed) * 3.2 + Math.sin(wz * 0.11) * 4.5;
                double pwid = 5.5 + 2.0 * vn(wz * 0.4, 3, seed);
                boolean onPath = Math.abs(wx - pc) < pwid;
                switch (kind) {
                    case 0: {
                        c = ramp(cols, 0.12 + detail * 0.75 + (onPath ? 0.2 : 0), x, y);
                        double crack = Math.abs(vn(wx * 0.8, wz * 0.8, seed + 9) - 0.5);
                        if (crack < 0.022 + 0.01 * (1 - fade)) c = cols[0];
                        if (hash(x, y, seed) > 0.985) c = cols[cols.length - 1];
                        if (hash(x >> 1, y, seed + 2) > 0.992) c = alt[0];
                        break;
                    }
                    case 1: {
                        if (onPath) {
                            double e = Math.abs(wx - pc) / pwid;
                            c = ramp(alt, 0.25 + detail * 0.7 - e * 0.15, x, y);
                            if (e > 0.82 && hash(x, y, seed) > 0.4) c = ramp(cols, 0.4 + detail * 0.5, x, y);
                            if (hash(x, y, seed + 4) > 0.97) c = alt[0];
                        } else {
                            c = ramp(cols, 0.15 + detail * 0.9, x, y);
                            double blades = hash(x, y >> 1, seed + 1);
                            if (blades > 0.9) c = cols[cols.length - 1];
                            else if (blades < 0.06) c = cols[0];
                        }
                        break;
                    }
                    case 2: {
                        double row = Math.floor(wz * 0.8), offs = ((int) row & 1) * 0.5;
                        double cx = wx / 1.9 + offs, col = Math.floor(cx);
                        double fz = wz * 0.8 - row, fx = cx - col;
                        double tone = hash((int) col, (int) row, seed) * 0.55 + detail * 0.45;
                        c = ramp(cols, tone, x, y);
                        if (fade > 0.3 && (fz < 0.13 || fx < 0.06)) c = alt[0];
                        else if (fade > 0.3 && fz > 0.8 && hash(x, y, seed) > 0.5) c = cols[1];
                        break;
                    }
                    default: {
                        boolean road = Math.abs(x - lw / 2.0) < 6 + dy * 0.2;
                        c = ramp(cols, 0.1 + detail * 0.8, x, y);
                        double crack = Math.abs(vn(wx * 0.55, wz * 0.55, seed + 21) - 0.5);
                        double crack2 = Math.abs(vn(wx * 1.1 + 30, wz * 1.1, seed + 22) - 0.5);
                        if (!road && fade > 0.15 && (crack < 0.018 || crack2 < 0.012)) {
                            c = alt[0];
                            if (glow != null) glow.add(y * lw + x);
                        } else if (!road && fade > 0.15 && crack < 0.03 && hash(x, y, seed) > 0.4) c = cols[0];
                    }
                }
                p.set(x, y, c);
            }
        }
    }

    // ---------------------------------------------------------------- trees

    private BufferedImage leafyTree(int seed, int trunkH, int rad, int[] greens, int bark, int barkLit) {
        int sw = rad * 3 + 8, sh = trunkH + rad * 2 + 10;
        P s = new P(sw, sh);
        Random r = new Random(seed);
        int cx = sw / 2, cy = rad + 6;
        int tw = Math.max(3, rad / 4);
        for (int y = cy; y < sh; y++) {   // trunk with a flare at the bottom
            int flare = y > sh - 6 ? (y - (sh - 6)) / 2 + 1 : 0;
            for (int x = cx - tw / 2 - flare; x <= cx + tw / 2 + flare; x++) {
                double lt = (x - (cx - tw / 2 - flare)) / (double) (tw + 2 * flare);
                s.set(x, y, lt < 0.3 ? barkLit : (lt > 0.7 ? mul(bark, 0.7) : bark));
            }
        }
        boolean[][] mask = new boolean[sh][sw];
        int blobs = 6 + r.nextInt(3);
        for (int b = 0; b < blobs; b++) {
            double bx = cx + (r.nextDouble() - 0.5) * rad * 1.5, by = cy + (r.nextDouble() - 0.5) * rad * 0.9;
            double br = rad * (0.55 + r.nextDouble() * 0.3);
            for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) if ((x - bx) * (x - bx) + (y - by) * (y - by) <= br * br) mask[y][x] = true;
        }
        for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) {
            if (!mask[y][x]) continue;
            double light = (-(x - cx) * 0.4 - (y - cy) * 0.8) / rad;
            double clump = vn(x * 0.33, y * 0.33, seed);
            double v = 0.45 + light * 0.32 + (clump - 0.5) * 0.8;
            if (y + 3 < sh && !mask[y + 3][x]) v -= 0.3;
            if (hash(x, y, seed) > 0.93) v += 0.2;
            s.set(x, y, ramp(greens, v, x, y));
        }
        return s.image();
    }

    private BufferedImage deadTree(int seed, int height, int bark, int barkLit) {
        int sw = height + 10, sh = height + 4;
        P s = new P(sw, sh);
        Random r = new Random(seed);
        branch(s, sw / 2.0, sh - 1, Math.PI / 2 + (r.nextDouble() - 0.5) * 0.2, height * 0.5, Math.max(3, height / 9.0), 4, r, bark);
        s.rim(barkLit, mul(bark, 0.6));
        return s.image();
    }

    private void branch(P s, double x, double y, double ang, double len, double th, int depth, Random r, int bark) {
        double ex = x + Math.cos(ang) * len, ey = y - Math.sin(ang) * len;
        s.thickLine((int) x, (int) y, (int) ex, (int) ey, th, bark);
        if (depth <= 0 || len < 3) return;
        int kids = 2 + (r.nextInt(3) == 0 ? 1 : 0);
        for (int k = 0; k < kids; k++) {
            double a2 = ang + (r.nextDouble() - 0.5) * 1.5 + (k == 0 ? -0.25 : 0.25);
            branch(s, ex, ey, a2, len * (0.6 + r.nextDouble() * 0.2), Math.max(1, th * 0.65), depth - 1, r, bark);
        }
    }

    private void drawSway(Graphics2D g, BufferedImage s, int x, int y, double amp, double phase, int t) {
        int sw = s.getWidth(), sh = s.getHeight();
        double gust = Math.sin(t * 0.03 + phase) + 0.5 * Math.sin(t * 0.071 + phase * 2.3);
        for (int r = 0; r < sh; r++) {
            double k = 1 - r / (double) sh;
            int dx = (int) Math.round(amp * k * k * gust);
            g.drawImage(s, x + dx, y + r, x + dx + sw, y + r + 1, 0, r, sw, r + 1, null);
        }
    }

    // ---------------------------------------------------------------- houses

    // palette order: 0 wall, 1 wall shade, 2 timber, 3 roof, 4 roof lit, 5 roof shade, 6 glass, 7 door, 8 wall lit
    private void house(P p, int x, int base, int hw, int hh, int roofH, int[] c, double damage, int seed, boolean lit, int litColor, boolean mark) {
        int top = base - hh;
        Random r = new Random(seed);
        for (int y = top; y < base; y++) for (int xx = x; xx < x + hw; xx++) {   // walls
            double side = (xx - x) / (double) hw;
            double n = hash(xx, y, seed);
            int col = side > 0.82 ? ramp(new int[]{c[1], c[0]}, 0.3 + n * 0.4, xx, y)
                    : ramp(new int[]{c[1], c[0], c[8]}, 0.55 + (n - 0.5) * 0.45 + (side < 0.18 ? 0.2 : 0), xx, y);
            if (y > base - 4 && n > 0.35) col = c[1];
            p.set(xx, y, col);
        }
        int mid = top + hh / 2;   // timber frame
        for (int xx = x; xx < x + hw; xx++) {
            p.set(xx, top, c[2]); p.set(xx, top + 1, c[2]);
            p.set(xx, mid, c[2]); p.set(xx, mid + 1, c[2]);
            p.set(xx, base - 1, c[2]); p.set(xx, base - 2, c[2]);
        }
        int cols = Math.max(2, hw / 14);
        for (int k = 0; k <= cols; k++) {
            int vx = x + (int) Math.round((hw - 2) * k / (double) cols);
            for (int y = top; y < base; y++) { p.set(vx, y, c[2]); p.set(vx + 1, y, c[2]); }
        }
        for (int k = 0; k < cols; k++) {
            int x0 = x + (int) Math.round((hw - 2) * k / (double) cols) + 2, x1 = x + (int) Math.round((hw - 2) * (k + 1) / (double) cols);
            if (r.nextBoolean()) p.line(x0, top + 2, x1, mid, c[2]); else p.line(x1, top + 2, x0, mid, c[2]);
        }
        int doorCol = cols / 2;   // windows and door
        for (int k = 0; k < cols; k++) {
            int cx = x + (int) Math.round((hw - 2) * (k + 0.5) / (double) cols) + 1;
            if (k != doorCol) windowAt(p, cx, mid + 4, c, lit, litColor, r.nextInt(3) == 0 && damage > 0.4);
            windowAt(p, cx, top + 4, c, lit, litColor, damage > 0.5 && r.nextBoolean());
        }
        int dx = x + (int) Math.round((hw - 2) * (doorCol + 0.5) / (double) cols) + 1;
        int dh = Math.min(13, hh / 2 - 1);
        for (int y = base - dh; y < base - 1; y++) for (int xx = dx - 3; xx <= dx + 3; xx++) {
            boolean corner = (y == base - dh) && (xx == dx - 3 || xx == dx + 3);
            if (corner) continue;
            p.set(xx, y, (xx - dx) % 2 == 0 ? c[7] : mul(c[7], 0.8));
        }
        for (int y = base - dh - 1; y < base - 1; y++) { p.set(dx - 4, y, c[8]); p.set(dx + 4, y, c[1]); }
        p.set(dx + 2, base - 6, c[4]);
        if (mark) { p.line(dx - 3, base - dh + 2, dx + 3, base - 4, 0xA01818); p.line(dx + 3, base - dh + 2, dx - 3, base - 4, 0xA01818); }
        int apex = top - roofH, ax = x + hw / 2;   // roof
        for (int y = apex; y < top + 1; y++) {
            double half = (y - apex) / (double) roofH * (hw / 2.0 + 3);
            for (int xx = (int) Math.floor(ax - half); xx <= (int) Math.ceil(ax + half); xx++) {
                int row = (y - apex) / 3, offs = (row & 1) * 2;
                double n = hash((xx + offs) / 4, row, seed);
                boolean left = xx < ax;
                int col = left ? ramp(new int[]{c[3], c[4]}, 0.35 + n * 0.5, xx, y) : ramp(new int[]{c[5], c[3]}, 0.2 + n * 0.6, xx, y);
                if ((y - apex) % 3 == 2) col = mul(col, 0.72);
                else if ((xx + offs) % 4 == 0) col = mul(col, 0.85);
                p.set(xx, y, col);
            }
        }
        for (int xx = x - 3; xx < x + hw + 3; xx++) { p.set(xx, top + 1, c[5]); p.set(xx, top + 2, mul(c[5], 0.6)); }
        p.set(ax, apex, c[4]); p.set(ax, apex + 1, c[4]);
        int chx = x + hw * 3 / 4, chy = apex + roofH / 2;   // chimney
        p.rect(chx, chy - 8, 5, 10, c[1]); p.rect(chx, chy - 8, 1, 10, c[8]); p.rect(chx - 1, chy - 9, 7, 2, c[2]);
        int holes = (int) Math.round(damage * 4);   // damage: holes in the roof showing rafters
        for (int k = 0; k < holes; k++) {
            int hy = apex + roofH / 4 + r.nextInt(Math.max(1, roofH * 2 / 3));
            int hx = ax + (r.nextInt(hw) - hw / 2) * (hy - apex) / Math.max(1, roofH);
            int rad = 3 + r.nextInt(4);
            for (int j = -rad; j <= rad; j++) for (int i = -rad; i <= rad; i++) if (i * i + j * j <= rad * rad) p.erase(hx + i, hy + j);
            for (int j = -rad; j <= rad; j += 3) p.line(hx - rad, hy + j, hx + rad, hy + j - 2, c[2]);
        }
        if (damage > 0.5) {
            int bx = x + r.nextInt(Math.max(1, hw / 2)), by = top + 3 + r.nextInt(Math.max(1, hh / 3));
            for (int j = 0; j < 6; j++) for (int i = 0; i < 7; i++) p.erase(bx + i, by + j);
            p.line(bx, by + 6, bx + 7, by, c[2]);
        }
    }

    private void windowAt(P p, int cx, int ty, int[] c, boolean lit, int litColor, boolean broken) {
        p.rect(cx - 3, ty - 1, 7, 9, c[2]);
        int glass = lit && !broken ? litColor : c[6];
        p.rect(cx - 2, ty, 5, 7, glass);
        if (lit && !broken) { p.set(cx - 2, ty, lerp(litColor, 0xFFFFFF, 0.5)); p.set(cx - 1, ty, lerp(litColor, 0xFFFFFF, 0.35)); }
        else p.set(cx - 2, ty, lerp(glass, 0xFFFFFF, 0.18));
        p.rect(cx, ty, 1, 7, c[2]); p.rect(cx - 2, ty + 3, 5, 1, c[2]);
        p.rect(cx - 4, ty + 8, 9, 1, c[8]);
        if (broken) { p.line(cx - 2, ty, cx + 1, ty + 4, c[2]); p.set(cx + 2, ty + 5, c[2]); }
    }

    // ---------------------------------------------------------------- shared animated bits

    private static Color rgba(int rgb, int a) { return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a); }

    private Anim particles(int n, int seed, double vx, double vy, int rgb, int alpha, int wob, int yMin, int yMax, int size) {
        return (g, t) -> {
            g.setColor(rgba(rgb, alpha));
            int span = yMax - yMin;
            for (int i = 0; i < n; i++) {
                double x0 = hash(i, 0, seed) * lw, y0 = hash(i, 1, seed) * span;
                double x = pmod(x0 + vx * t + Math.sin(t * 0.02 + i * 1.7) * wob, (double) lw);
                double y = pmod(y0 + vy * t, (double) span) + yMin;
                g.fillRect((int) x, (int) y, size, size);
            }
        };
    }

    private void bird(Graphics2D g, double x, double y, int phase, int rgb, int size) {
        g.setColor(new Color(rgb));
        int ix = (int) x, iy = (int) y, up = (phase / 5) % 4;
        int dy = up == 0 ? -size : (up == 1 || up == 3 ? 0 : size);
        for (int i = 1; i <= size + 1; i++) {
            g.fillRect(ix - i, iy + (dy * i) / (size + 1), 1, 1);
            g.fillRect(ix + i, iy + (dy * i) / (size + 1), 1, 1);
        }
        g.fillRect(ix, iy, 1, 2);
    }

    private void flame(Graphics2D g, int x, int y, int hgt, int t, int i, int[] cols) {
        double[] sc = {1.0, 0.68, 0.38};
        for (int k = 0; k < 3; k++) {
            int fh = (int) (hgt * sc[k] * (0.85 + 0.15 * Math.sin(t * 0.4 + i * 2 + k)));
            int fw = (int) (hgt * sc[k] / 2.2) + 1;
            int sway = (int) Math.round(Math.sin(t * 0.3 + i + k * 0.7) * (1.5 - k * 0.4));
            g.setColor(new Color(cols[k]));
            g.fillPolygon(new int[]{x - fw, x + fw, x + sway}, new int[]{y, y, y - fh}, 3);
        }
    }

    private BufferedImage makeVignette() {
        BufferedImage v = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = v.createGraphics();
        float r = (float) Math.hypot(w, h) * 0.6f;
        RadialGradientPaint rp = new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.45f), r, new float[]{0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)});
        g.setPaint(rp);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return v;
    }

    private BufferedImage overlay(BufferedImage base, BufferedImage top) {
        BufferedImage out = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(base, 0, 0, null);
        g.drawImage(top, 0, 0, null);
        g.dispose();
        return out;
    }

    // ================================================================= WORLD 1: The Reckoning (opening)
    // A dying village in famine: dust-brown sky and a pale sun, ruined timber houses, dead trees, cracked and dusty earth, crows and drifting ash.

    private Scene buildWorld1() {
        Scene s = new Scene();
        int[] skyRamp = {0x2b2324, 0x4a3834, 0x7b5b47, 0xb18860, 0xd8ae78};
        P sky = new P(lw, lh);
        for (int y = 0; y < lh; y++) for (int x = 0; x < lw; x++) sky.set(x, y, ramp(skyRamp, y / (double) (HZ + 6), x, y));
        int sx = 292, sy = 54;   // a pale, sick sun behind the dust
        for (int y = 0; y < HZ + 6; y++) for (int x = 0; x < lw; x++) {
            double d = Math.hypot(x - sx, y - sy);
            if (d < 13) sky.set(x, y, d < 10 ? 0xf0dcae : 0xe3c58f);
            else if (d < 40) { double t = 1 - (d - 13) / 27.0; if (t * t * 0.9 > bay(x, y)) sky.set(x, y, lerp(sky.get(x, y), 0xe6c48c, 0.55)); }
        }
        s.sky = sky.image();
        s.clouds = cloudStrip(11, 0.49, new int[]{0x2c2122, 0x4b3a37, 0x72584a, 0x9a7a5c}, 6, 74, 11.0);
        s.cloudSpeed = 0.07;

        P p = new P(lw, lh);
        mountains(p, 5, HZ - 6, 30, 0.012, new int[]{0x2a2020, 0x3e302c, 0x594438}, 0, 0);
        haze(p, HZ - 22, HZ + 2, 0x8c6a4e, 0.45);
        int[] ruin = {0x6d5f55, 0x4c413b, 0x2e2420, 0x3a2c26, 0x55433a, 0x241a17, 0x0d0a0a, 0x3b2a1f, 0x8c7c70};
        int[] far = new int[ruin.length];
        for (int i = 0; i < ruin.length; i++) far[i] = lerp(ruin[i], 0x8c6a4e, 0.45);
        house(p, 150, HZ - 2, 26, 22, 13, far, 0.8, 21, false, 0, false);
        house(p, 190, HZ - 3, 22, 20, 12, far, 1.0, 22, false, 0, false);
        house(p, 226, HZ - 2, 24, 21, 13, far, 0.7, 23, false, 0, false);
        ground(p, 31, 0, new int[]{0x3a2c22, 0x54422f, 0x6e5a40, 0x8a7352}, new int[]{0x2a2018}, null);
        haze(p, HZ - 4, HZ + 14, 0x8c6a4e, 0.5);
        house(p, 96, HZ + 3, 38, 30, 20, ruin, 0.9, 24, false, 0, true);
        house(p, 262, HZ + 4, 44, 32, 22, ruin, 1.0, 25, false, 0, false);
        house(p, -8, HZ + 8, 56, 38, 24, ruin, 0.8, 26, false, 0, true);
        house(p, 348, HZ + 9, 60, 40, 25, ruin, 0.9, 27, false, 0, false);
        for (int[] sh : new int[][]{{96, 38, HZ + 3}, {262, 44, HZ + 4}, {-8, 56, HZ + 8}, {348, 60, HZ + 9}})
            for (int k = 0; k < 3; k++) for (int x = sh[0] - 2; x < sh[0] + sh[1] + 3; x++) p.darken(x, sh[2] + k, 0.62);
        Random fr = new Random(7);   // broken fence posts
        for (int x = 130; x < 250; x += 9) { int hgt = 5 + fr.nextInt(5); p.rect(x, HZ + 6 - hgt, 2, hgt + 1, 0x2a1f19); p.set(x, HZ + 6 - hgt, 0x5a463a); }
        s.scenery = p.image();

        BufferedImage dt1 = deadTree(3, 56, 0x2d221d, 0x66544a), dt2 = deadTree(8, 44, 0x2d221d, 0x66544a), dt3 = deadTree(15, 64, 0x2d221d, 0x66544a);
        s.anims.add((g, t) -> {
            drawSway(g, dt1, 66, HZ + 8 - dt1.getHeight(), 1.2, 0.3, t);
            drawSway(g, dt2, 232, HZ + 10 - dt2.getHeight(), 1.0, 1.9, t);
            drawSway(g, dt3, 330 - dt3.getWidth() / 2, HZ + 12 - dt3.getHeight(), 1.4, 3.1, t);
        });
        s.anims.add((g, t) -> {   // dead grass tufts in the foreground, bending in the wind
            for (int i = 0; i < 46; i++) {
                int x = (int) (hash(i, 0, 90) * lw), y = HZ + 20 + (int) (hash(i, 1, 90) * (lh - HZ - 40));
                int hh = 3 + (y - HZ) / 22;
                double gust = Math.sin(t * 0.05 + x * 0.04) * 1.4;
                g.setColor(new Color(0xa48a52));
                for (int k = 0; k < hh; k++) g.fillRect(x + (int) (gust * k / hh) + (k % 2), y - k, 1, 1);
                g.setColor(new Color(0x7d6738));
                for (int k = 0; k < hh - 1; k++) g.fillRect(x + 2 + (int) (gust * k / hh), y - k, 1, 1);
            }
        });
        s.anims.add((g, t) -> {   // thin smoke from the one house still lived in
            for (int i = 0; i < 9; i++) {
                double age = (t * 0.4 + i * 9) % 80, x = 296 + age * 0.35 + Math.sin(age * 0.2 + i) * 2, y = HZ - 34 - age * 0.7;
                g.setColor(rgba(0x6a5a52, (int) (110 * (1 - age / 80))));
                g.fillRect((int) x, (int) y, 3, 2);
            }
        });
        s.anims.add((g, t) -> {
            for (int i = 0; i < 4; i++) {
                double bx = pmod(t * (0.22 + i * 0.05) + i * 110, lw + 40.0) - 20, by = 20 + i * 8 + Math.sin((t + i * 40) * 0.03) * 5;
                bird(g, bx, by, t + i * 3, 0x120d0d, 3 + (i & 1));
            }
        });
        s.anims.add(particles(70, 5, -0.3, 0.12, 0xb59a74, 130, 6, 0, lh, 1));
        s.anims.add(particles(30, 6, -0.6, 0.05, 0x8a6e52, 90, 5, HZ - 20, lh, 2));
        return s;
    }

    // ================================================================= WORLD 2: The Long Erosion
    // The still-colourful past: snow mountains, a blue lake, forested hills, wheat and a dirt road, leafy trees. One dying tree hints at what is coming.

    private Scene buildWorld2() {
        Scene s = new Scene();
        s.sky = skyImage(new int[]{0x3a78d2, 0x5a9ae0, 0x84bef0, 0xbde2f6, 0xeaf4e4});
        s.clouds = cloudStrip(41, 0.47, new int[]{0x9fb9d9, 0xd2e2f3, 0xf4f9fe, 0xffffff}, 4, 66, 9.0);
        s.cloudSpeed = 0.14;
        P p = new P(lw, lh);
        mountains(p, 2, HZ - 14, 52, 0.011, new int[]{0x435682, 0x6a80a8, 0x9bb2d0}, 0xf1f6fb, 0xb4c6e0);
        haze(p, HZ - 40, HZ - 8, 0xbcd6ec, 0.35);
        mountains(p, 9, HZ - 6, 24, 0.02, new int[]{0x2f5a5e, 0x3f7a62, 0x58996a}, 0, 0);
        forestEdge(p, 4, new int[]{0x1d4a2a, 0x2d6a34, 0x3f8c3e, 0x62b050}, HZ - 40, HZ);
        int lcx = 215, lcy = HZ + 2, lrx = 150, lry = 13;   // the lake
        for (int y = lcy - lry; y <= lcy + lry; y++) for (int x = lcx - lrx; x <= lcx + lrx; x++) {
            double e = ((x - lcx) / (double) lrx) * ((x - lcx) / (double) lrx) + ((y - lcy) / (double) lry) * ((y - lcy) / (double) lry);
            if (e > 1) continue;
            double t = (y - (lcy - lry)) / (2.0 * lry);
            p.set(x, y, ramp(new int[]{0x7fb3ee, 0x4f88d8, 0x2e62b0, 0x244a8a}, t * 0.9 + (vn(x * 0.1, y * 0.5, 3) - 0.5) * 0.2, x, y));
        }
        haze(p, HZ - 8, HZ + 6, 0xdff0f6, 0.35);
        ground(p, 17, 1, new int[]{0x2c6a2c, 0x3d8a30, 0x56a83a, 0x7bc84c}, new int[]{0x6a4e2e, 0x876a3a, 0xa68848, 0xc2a460}, null);
        for (int y = HZ + 3; y < HZ + 70; y++) {   // wheat field on the right, rows following the perspective
            double dy = y - HZ + 5, extent = 30 + dy * 1.3;
            for (int x = 300; x < lw; x++) {
                if (x - 300 > extent * 0.9) continue;
                double row = Math.log(dy / 4.0) * 12;
                double fr = row - Math.floor(row);
                int c = ramp(new int[]{0x8a6a1e, 0xc29a2c, 0xe6c04a, 0xf6dc78}, 0.35 + fr * 0.55 + (hash(x, y, 5) - 0.5) * 0.3, x, y);
                if (fr < 0.12) c = 0x6e5318;
                if (x - 300 > extent * 0.78 && hash(x, y, 6) > 0.5) continue;
                p.set(x, y, c);
            }
        }
        int[] cottage = {0xe6dcc0, 0xb7ab92, 0x5a3c26, 0x9a4a30, 0xc86a40, 0x6e3222, 0x2c3a4a, 0x6a4326, 0xf6efd8};   // a small hamlet in the valley
        int[] fc = new int[cottage.length];
        for (int i = 0; i < cottage.length; i++) fc[i] = lerp(cottage[i], 0xc8dcec, 0.38);
        house(p, 70, HZ + 1, 20, 14, 9, fc, 0, 71, false, 0, false);
        house(p, 98, HZ + 2, 16, 12, 8, fc, 0, 72, false, 0, false);
        haze(p, HZ - 2, HZ + 10, 0xd8ecda, 0.3);
        s.scenery = p.image();

        BufferedImage[] big = {
                leafyTree(1, 44, 30, new int[]{0x16421f, 0x276a2c, 0x3d9440, 0x6dbf55}, 0x4a3320, 0x7a5a38),
                leafyTree(2, 38, 26, new int[]{0x1a4a24, 0x2e7430, 0x45a044, 0x78c85c}, 0x4a3320, 0x7a5a38),
                leafyTree(3, 30, 18, new int[]{0x1d5028, 0x34803a, 0x52ac4a, 0x84d064}, 0x4a3320, 0x7a5a38),
                leafyTree(4, 26, 14, new int[]{0x1d5028, 0x34803a, 0x52ac4a, 0x84d064}, 0x4a3320, 0x7a5a38)};
        BufferedImage dead = deadTree(6, 40, 0x4a3828, 0x7c6448);
        s.anims.add((g, t) -> {
            drawSway(g, big[3], 160, HZ + 14 - big[3].getHeight(), 1.2, 4.0, t);
            drawSway(g, big[2], 262, HZ + 18 - big[2].getHeight(), 1.4, 2.0, t);
            drawSway(g, dead, 190, HZ + 20 - dead.getHeight(), 0.8, 5.0, t);
            drawSway(g, big[1], -12, HZ + 34 - big[1].getHeight(), 1.6, 1.0, t);
            drawSway(g, big[0], 342, HZ + 44 - big[0].getHeight(), 1.8, 0.0, t);
        });
        s.anims.add((g, t) -> {   // sparkle on the lake
            for (int i = 0; i < 26; i++) {
                int life = 40, ph = (t + i * 17) % life;
                if (ph > 12) continue;
                int x = 75 + (int) (hash(i, 3, 12) * 280), y = HZ - 8 + (int) (hash(i, 4, 12) * 20);
                double e = ((x - 215) / 150.0) * ((x - 215) / 150.0) + ((y - (HZ + 2)) / 13.0) * ((y - (HZ + 2)) / 13.0);
                if (e > 0.85) continue;
                g.setColor(new Color(255, 255, 255, 220 - ph * 14));
                g.fillRect(x, y, ph < 6 ? 3 : 2, 1);
            }
        });
        s.anims.add((g, t) -> {   // wheat ripples
            for (int i = 0; i < 40; i++) {
                int x = 306 + (int) (hash(i, 5, 14) * 90), y = HZ + 6 + (int) (hash(i, 6, 14) * 50);
                double ph = (t * 0.06 + x * 0.05 - y * 0.03) % (Math.PI * 2);
                if (Math.sin(ph) > 0.85) { g.setColor(new Color(0xfbe896)); g.fillRect(x, y, 3, 1); }
            }
        });
        s.anims.add((g, t) -> {   // grass tufts and wild flowers near the viewer
            for (int i = 0; i < 70; i++) {
                int x = (int) (hash(i, 0, 40) * lw), y = HZ + 24 + (int) (hash(i, 1, 40) * (lh - HZ - 40));
                int hh = 3 + (y - HZ) / 26;
                double gust = Math.sin(t * 0.06 + x * 0.05) * 1.5;
                g.setColor(new Color(0x92d65a));
                for (int k = 0; k < hh; k++) g.fillRect(x + (int) (gust * k / hh), y - k, 1, 1);
                g.setColor(new Color(0x4a9a36));
                for (int k = 0; k < hh - 1; k++) g.fillRect(x + 2 + (int) (gust * k / hh), y - k, 1, 1);
                if (i % 6 == 0) { g.setColor(new Color(i % 12 == 0 ? 0xffffff : 0xffd84a)); g.fillRect(x + (int) gust + 1, y - hh - 1, 2, 2); }
            }
        });
        s.anims.add((g, t) -> {   // butterflies
            int[] cols = {0xffe04a, 0xffffff, 0xff9ac4, 0x9ad4ff, 0xffb347};
            for (int i = 0; i < 5; i++) {
                double x = 60 + hash(i, 1, 77) * 280 + Math.sin(t * 0.03 + i * 2) * 26, y = HZ + 30 + hash(i, 2, 77) * 90 + Math.sin(t * 0.07 + i * 4) * 8;
                boolean open = (t / 4 + i) % 2 == 0;
                g.setColor(new Color(cols[i]));
                g.fillRect((int) x, (int) y, open ? 3 : 1, 2);
                g.setColor(new Color(0x2a2218));
                g.fillRect((int) x + (open ? 1 : 0), (int) y, 1, 2);
            }
        });
        s.anims.add((g, t) -> {   // far birds
            for (int i = 0; i < 4; i++) bird(g, pmod(t * 0.25 + i * 70, lw + 30.0) - 15, 28 + i * 9 + Math.sin((t + i * 30) * 0.04) * 3, t + i * 4, 0x3a4a62, 2);
        });
        s.anims.add(particles(34, 9, 0.2, -0.04, 0xfff6c0, 150, 8, 40, lh - 40, 1));
        return s;
    }

    // ================================================================= WORLD 3: The Age of Watching
    // The same village square twice: warm festival (bunting, lanterns, lit windows) and plague-village dread (fog, torn rags, marked doors).

    private Scene buildWorld3(boolean plague) {
        Scene s = new Scene();
        int[] skyRamp = plague ? new int[]{0x1f2826, 0x33403a, 0x586654, 0x7d8a6c, 0xa0a98a} : new int[]{0x2f3470, 0x6a4f8a, 0xc9806e, 0xf2b068, 0xffd88a};
        s.sky = skyImage(skyRamp);
        s.clouds = cloudStrip(plague ? 53 : 51, plague ? 0.46 : 0.5,
                plague ? new int[]{0x222b28, 0x3b4840, 0x5a6a58, 0x7c8c72} : new int[]{0x6a4a7c, 0xc4707a, 0xf4a878, 0xffe0a0}, 6, 70, 9.0);
        s.cloudSpeed = plague ? 0.05 : 0.09;
        P p = new P(lw, lh);
        mountains(p, 12, HZ - 6, 28, 0.014, plague ? new int[]{0x1f2a27, 0x2c3a34, 0x435247} : new int[]{0x3c3a6a, 0x5a4a82, 0x8c6a94}, 0, 0);
        forestEdge(p, 14, plague ? new int[]{0x151e1c, 0x1f2c28, 0x2c3c34, 0x3c4e42} : new int[]{0x23304a, 0x344a5a, 0x4a6a66, 0x6a8a76}, HZ - 40, HZ);
        haze(p, HZ - 20, HZ + 2, plague ? 0x7d8a6c : 0xe49a72, 0.45);
        int[] pal = plague ? new int[]{0x8a9486, 0x5c665a, 0x241c18, 0x3c3c34, 0x555a48, 0x24261f, 0x0d1210, 0x3a2c22, 0xa9b3a2}
                : new int[]{0xf0dcb4, 0xc4a888, 0x5a3a26, 0x9a4630, 0xd0663a, 0x6a2a22, 0x2a3048, 0x7a4a28, 0xfff0cc};
        int[] farp = new int[pal.length];
        for (int i = 0; i < pal.length; i++) farp[i] = lerp(pal[i], plague ? 0x7d8a6c : 0xe49a72, 0.45);
        int litC = 0xffcc66;
        boolean lit = !plague;
        house(p, 168, HZ - 3, 26, 22, 14, farp, plague ? 0.4 : 0, 61, lit, litC, plague);
        house(p, 202, HZ - 3, 22, 20, 13, farp, plague ? 0.2 : 0, 62, lit, litC, false);
        house(p, 232, HZ - 3, 24, 22, 14, farp, plague ? 0.5 : 0, 63, lit, litC, plague);
        ground(p, 67, 2, plague ? new int[]{0x2a302a, 0x3c453c, 0x515b4e, 0x6b7666} : new int[]{0x5a4838, 0x7a6048, 0x9a7a58, 0xb89870},
                plague ? new int[]{0x151a16} : new int[]{0x3a2a1c}, null);
        haze(p, HZ - 4, HZ + 12, plague ? 0x7d8a6c : 0xd89870, 0.4);
        house(p, 96, HZ + 3, 40, 32, 21, pal, plague ? 0.35 : 0, 64, lit, litC, plague);
        house(p, 262, HZ + 3, 42, 32, 21, pal, plague ? 0.25 : 0, 65, lit, litC, plague);
        house(p, 40, HZ + 7, 52, 36, 24, pal, plague ? 0.4 : 0, 66, lit, litC, false);
        house(p, 302, HZ + 8, 56, 38, 24, pal, plague ? 0.45 : 0, 67, lit, litC, plague);
        house(p, -16, HZ + 12, 58, 42, 26, pal, plague ? 0.3 : 0, 68, lit, litC, plague);
        house(p, 350, HZ + 13, 64, 44, 27, pal, plague ? 0.3 : 0, 69, lit, litC, false);
        for (int[] sh : new int[][]{{96, 40, HZ + 3}, {262, 42, HZ + 3}, {40, 52, HZ + 7}, {302, 56, HZ + 8}, {-16, 58, HZ + 12}, {350, 64, HZ + 13}})
            for (int k = 0; k < 3; k++) for (int x = sh[0] - 2; x < sh[0] + sh[1] + 3; x++) p.darken(x, sh[2] + k, 0.68);
        int wx = 188, wy = HZ + 14;   // the well in the middle of the square
        p.rect(wx, wy - 8, 24, 9, plague ? 0x59635a : 0x9a8a78);
        for (int x = wx; x < wx + 24; x++) { p.set(x, wy - 8, plague ? 0x7a857a : 0xc8b8a0); p.set(x, wy, plague ? 0x2c332d : 0x5a4a3a); }
        for (int y = wy - 8; y < wy; y += 3) for (int x = wx + (y / 3 % 2) * 4; x < wx + 24; x += 8) p.set(x, y, plague ? 0x333c34 : 0x6f6050);
        p.rect(wx + 1, wy - 18, 2, 10, 0x3a2a1c); p.rect(wx + 21, wy - 18, 2, 10, 0x3a2a1c);
        p.rect(wx, wy - 20, 24, 2, 0x4a3422);
        p.line(wx + 12, wy - 18, wx + 12, wy - 11, 0x2a1f14);
        p.rect(wx + 10, wy - 11, 5, 3, 0x2a1f14);
        for (int x = wx - 1; x < wx + 26; x++) for (int k = 1; k < 4; k++) p.darken(x, wy + k, 0.7);
        s.scenery = p.image();

        final int[][] spans = {{50, 52, 148, 58, 9}, {148, 58, 262, 50, 10}, {262, 50, 346, 56, 9}, {6, 40, 98, 48, 7}, {120, 36, 300, 36, 14}};
        P line = new P(lw, lh);   // bunting lines (the flags are animated)
        for (int[] sp : spans) {
            for (int x = sp[0]; x <= sp[2]; x++) {
                double f = (x - sp[0]) / (double) (sp[2] - sp[0]);
                int y = (int) Math.round(sp[1] + (sp[3] - sp[1]) * f + Math.sin(f * Math.PI) * sp[4]);
                line.set(x, y, plague ? 0x1a1814 : 0x3a2a20);
            }
        }
        s.scenery = overlay(s.scenery, line.image());
        final int[] flagCols = plague ? new int[]{0x4a5048, 0x38403a, 0x5a5a48} : new int[]{0xd23a3a, 0xf0c040, 0x3a7ad2, 0x3aa85a, 0xe27ab4};
        s.anims.add((g, t) -> {
            for (int si = 0; si < spans.length; si++) {
                int[] sp = spans[si];
                int step = plague ? 9 : 6, n = 0;
                for (int x = sp[0] + 4; x < sp[2] - 3; x += step, n++) {
                    if (plague && hash(x, si, 3) > 0.62) continue;
                    double f = (x - sp[0]) / (double) (sp[2] - sp[0]);
                    int y = (int) Math.round(sp[1] + (sp[3] - sp[1]) * f + Math.sin(f * Math.PI) * sp[4]) + 1;
                    double sw = Math.sin(t * (plague ? 0.05 : 0.09) + x * 0.07 + si) * (plague ? 1.8 : 1.1);
                    g.setColor(new Color(flagCols[n % flagCols.length]));
                    int fh = plague ? 5 + (n % 3) : 5;
                    g.fillPolygon(new int[]{x - 2, x + 2, x + (int) Math.round(sw)}, new int[]{y, y, y + fh}, 3);
                }
            }
        });
        if (!plague) {
            s.anims.add((g, t) -> {   // lanterns and warm glow
                int[][] ls = {{104, 48}, {198, 46}, {300, 46}, {60, 46}};
                for (int i = 0; i < ls.length; i++) {
                    double fl = 0.8 + 0.2 * Math.sin(t * 0.21 + i * 2.1) + 0.1 * Math.sin(t * 0.47 + i);
                    g.setColor(new Color(255, 190, 90, (int) (30 * fl)));
                    g.fillOval(ls[i][0] - 14, ls[i][1] - 12, 28, 24);
                    g.setColor(new Color(255, 200, 100, (int) (55 * fl)));
                    g.fillOval(ls[i][0] - 8, ls[i][1] - 7, 16, 14);
                    g.setColor(new Color(0xffd870));
                    g.fillRect(ls[i][0] - 2, ls[i][1] - 3, 5, 6);
                    g.setColor(new Color(0xfff4c0));
                    g.fillRect(ls[i][0] - 1, ls[i][1] - 2, 3, 3);
                    g.setColor(new Color(0x3a2a20));
                    g.fillRect(ls[i][0] - 3, ls[i][1] - 4, 7, 1);
                    g.fillRect(ls[i][0] - 3, ls[i][1] + 3, 7, 1);
                }
            });
            s.anims.add(particles(46, 3, 0.1, -0.35, 0xffc860, 200, 6, 20, lh - 20, 1));
            s.anims.add(particles(24, 4, 0.05, 0.2, 0xff8a6a, 150, 7, 0, lh, 2));
        } else {
            BufferedImage dead = deadTree(18, 50, 0x1c1a16, 0x4a4c40);
            s.anims.add((g, t) -> drawSway(g, dead, 160, HZ + 12 - dead.getHeight(), 0.9, 2.0, t));
            BufferedImage fog = cloudStrip(77, 0.44, new int[]{0x6c7a68, 0x7f8c78, 0x94a08a, 0xa6b19a}, 80, 200, 14.0);   // low creeping fog
            s.anims.add((g, t) -> {
                int cw = fog.getWidth(), off = (int) ((t * 0.16) % cw);
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.38f));
                g.drawImage(fog, -off, HZ + 40 - 80, null);
                g.drawImage(fog, cw - off, HZ + 40 - 80, null);
                g.setComposite(old);
            });
            s.anims.add(particles(26, 12, -0.4, 0.1, 0x0c0e0c, 200, 14, 20, lh - 40, 1));
            s.anims.add((g, t) -> {   // crows
                for (int i = 0; i < 3; i++) bird(g, pmod(t * (0.3 + i * 0.06) + i * 140, lw + 40.0) - 20, 24 + i * 12 + Math.sin((t + i * 50) * 0.03) * 4, t + i * 5, 0x0a0a08, 3);
            });
        }
        return s;
    }

    // ================================================================= WORLD 4: The Reckoning (final)
    // The same land at its end: a blood sky, a black eclipse, burning ruins and the stone gate of Dabbah's Hollow. The ground is split by glowing cracks.

    private Scene buildWorld4() {
        Scene s = new Scene();
        int[] skyRamp = {0x080204, 0x240508, 0x560c10, 0x8c1a12, 0xc2381a};
        P sky = new P(lw, lh);
        for (int y = 0; y < lh; y++) for (int x = 0; x < lw; x++) sky.set(x, y, ramp(skyRamp, y / (double) (HZ + 6), x, y));
        final int ex = 200, ey = 32;
        int[] corona = {0x240508, 0x7a1410, 0xd04a1a, 0xffa84a, 0xfff0b8};
        for (int y = 0; y < HZ + 6; y++) for (int x = 0; x < lw; x++) {
            double d = Math.hypot(x - ex, y - ey), ang = Math.atan2(y - ey, x - ex);
            if (d < 26) { sky.set(x, y, 0x050102); continue; }
            double streak = 0.6 + 0.8 * vn(Math.cos(ang) * 4 + 9, Math.sin(ang) * 4 + 9, 4);
            double inten = Math.pow(clamp01(1 - (d - 26) / (46.0 * streak)), 2);
            if (inten > 0.04) {
                int c = ramp(corona, inten * 1.05, x, y);
                if (inten > bay(x, y) * 0.8) sky.set(x, y, lerp(sky.get(x, y), c, 0.95));
            }
        }
        s.sky = sky.image();
        s.clouds = cloudStrip(81, 0.5, new int[]{0x12040a, 0x2e0a10, 0x5e1814, 0x9a3a20}, 8, 76, 10.0);
        s.cloudSpeed = 0.12;

        P p = new P(lw, lh);
        mountains(p, 33, HZ - 4, 40, 0.012, new int[]{0x0c0305, 0x1a0709, 0x33100f}, 0, 0);
        haze(p, HZ - 24, HZ + 2, 0x8c2a18, 0.4);
        List<Integer> glow = new ArrayList<>();
        ground(p, 91, 3, new int[]{0x0e0707, 0x1a0f0e, 0x2a1b17, 0x3b2822}, new int[]{0xff5a1a}, glow);
        for (int y = HZ + 2; y < lh; y++) {   // a worn paving road leading to the gate
            double dy = y - HZ + 5, half = 2 + dy * 0.2;
            for (int x = (int) (200 - half); x <= 200 + half; x++) {
                double lz = Math.log(dy / 4.0) * 8, fz = lz - Math.floor(lz);
                int c = ramp(new int[]{0x1b1210, 0x2c211d, 0x433630}, hash(x >> 2, (int) lz, 3) * 0.9, x, y);
                if (fz < 0.1) c = 0x0e0807;
                p.set(x, y, c);
            }
        }
        gate(p, 200, HZ + 4);
        haze(p, HZ - 2, HZ + 8, 0x7a1a10, 0.25);
        int[] ruin = {0x4a3834, 0x30231f, 0x140e0c, 0x24161a, 0x3e2622, 0x180c0e, 0x050203, 0x2a1810, 0x6a4a42};   // burning ruins to either side
        house(p, 70, HZ + 6, 44, 34, 22, ruin, 1.0, 91, false, 0, false);
        house(p, 276, HZ + 6, 46, 34, 22, ruin, 1.0, 92, false, 0, false);
        house(p, -14, HZ + 12, 56, 40, 26, ruin, 1.0, 93, false, 0, false);
        house(p, 354, HZ + 12, 60, 42, 26, ruin, 1.0, 94, false, 0, false);
        for (int[] sh : new int[][]{{70, 44, HZ + 6}, {276, 46, HZ + 6}, {-14, 56, HZ + 12}, {354, 60, HZ + 12}})
            for (int k = 0; k < 3; k++) for (int x = sh[0] - 2; x < sh[0] + sh[1] + 3; x++) p.darken(x, sh[2] + k, 0.5);
        for (int y = HZ - 40; y < HZ + 40; y++) for (int x = 0; x < lw; x++) {   // red light from the eclipse on whatever faces it
            if (!p.has(x, y) || (p.get(x, y) & 0xFF) + ((p.get(x, y) >> 8) & 0xFF) < 40) continue;
            double d = Math.hypot(x - ex, (y - ey) * 0.8);
            if (d < 190 && hash(x, y, 8) < (1 - d / 190.0) * 0.28) p.tint(x, y, 0xd84a20, 0.55);
        }
        s.scenery = p.image();
        int[] gp = new int[glow.size()];
        for (int i = 0; i < gp.length; i++) gp[i] = glow.get(i);
        s.glowPixels = gp;
        s.glowColor = 0xff6a22;

        BufferedImage dead = deadTree(35, 54, 0x120a0a, 0x5a2a20), dead2 = deadTree(36, 42, 0x120a0a, 0x5a2a20);
        s.anims.add((g, t) -> {
            drawSway(g, dead, 134 - dead.getWidth() / 2, HZ + 10 - dead.getHeight(), 0.8, 1.0, t);
            drawSway(g, dead2, 268 - dead2.getWidth() / 2, HZ + 10 - dead2.getHeight(), 0.7, 3.0, t);
        });
        s.anims.add((g, t) -> {   // the eclipse pulses with a slow heartbeat
            double b = heartbeat();
            for (int k = 0; k < 4; k++) {
                g.setColor(new Color(255, 120 + k * 20, 40, clamp((int) ((18 - k * 4) * (0.5 + b)), 0, 255)));
                int r = 28 + k * 7 + (int) (b * 3);
                g.drawOval(ex - r, ey - r, r * 2, r * 2);
            }
        });
        s.anims.add((g, t) -> {   // fire on the ruins
            int[][] fires = {{80, HZ - 20}, {96, HZ - 14}, {296, HZ - 22}, {312, HZ - 12}, {18, HZ - 26}, {386, HZ - 28}};
            for (int i = 0; i < fires.length; i++) {
                g.setColor(new Color(255, 100, 30, 34));
                g.fillOval(fires[i][0] - 16, fires[i][1] - 20, 32, 28);
                flame(g, fires[i][0], fires[i][1], 14 + (i % 3) * 3, t, i, new int[]{0xc8300c, 0xff7a18, 0xffd84a});
            }
        });
        s.anims.add(particles(60, 21, 0.1, -0.55, 0xff8a2a, 230, 6, 10, lh - 10, 1));
        s.anims.add(particles(34, 22, -0.2, 0.18, 0x3a2420, 180, 8, 0, lh, 2));
        return s;
    }

    private void gate(P p, int cx, int base) {
        int[] stone = {0x1d1514, 0x33262a, 0x4a3a38, 0x6a5650};
        int tw = 26, th = 52, gap = 40;       // tower width, tower height, opening width
        int lx = cx - gap / 2 - tw, rx = cx + gap / 2;
        for (int side = 0; side < 2; side++) {
            int x0 = side == 0 ? lx : rx;
            for (int y = base - th; y < base; y++) for (int x = x0; x < x0 + tw; x++) {
                int row = (y - (base - th)) / 5, offs = (row & 1) * 4;
                double n = hash((x + offs) / 8, row, 5 + side);
                int c = ramp(stone, 0.25 + n * 0.55 + (x - x0 < 5 ? 0.15 : 0) - (x - x0 > tw - 6 ? 0.2 : 0), x, y);
                if ((y - (base - th)) % 5 == 4) c = stone[0];
                else if ((x + offs) % 8 == 0) c = mul(c, 0.7);
                p.set(x, y, c);
            }
            for (int x = x0 - 1; x < x0 + tw + 1; x += 6) p.rect(x, base - th - 5, 4, 5, stone[side == 0 ? 2 : 1]);   // battlements
            p.rect(x0 - 1, base - th - 1, tw + 2, 2, stone[3]);
            p.rect(x0 + tw / 2 - 1, base - th + 14, 3, 9, 0x050203);   // arrow slit
            p.rect(x0 + tw / 2, base - th + 15, 1, 3, 0x7a1a10);
        }
        int lintelY = base - 36;   // lintel above the opening
        for (int y = lintelY - 9; y < lintelY; y++) for (int x = lx + tw - 1; x <= rx; x++) {
            int c = ramp(stone, 0.35 + hash(x / 6, y / 4, 9) * 0.45, x, y);
            p.set(x, y, y == lintelY - 9 ? stone[3] : c);
        }
        for (int y = lintelY; y < base; y++) for (int x = lx + tw; x < rx; x++) {   // the black mouth with a faint red glow deep inside
            double d = Math.hypot((x - cx) / (gap / 2.0), (y - (base - 14)) / 22.0);
            int c = 0x040102;
            if (d < 1.0 && (1 - d) * 0.8 > bay(x, y)) c = 0x3a0c0c;
            if (d < 0.5 && (0.5 - d) > bay(x, y) * 0.5) c = 0x7a1a10;
            p.set(x, y, c);
        }
        Random r = new Random(5);   // dead vines creeping up the stone
        for (int k = 0; k < 9; k++) {
            int x = lx + r.nextInt(rx + tw - lx), y = base - 1;
            int len = 24 + r.nextInt(20);
            for (int i = 0; i < len; i++) {
                if (p.has(x, y) && p.get(x, y) != 0x040102) p.set(x, y, 0x0e0a08);
                y -= 1;
                x += r.nextInt(3) - 1;
            }
        }
        for (int k = 0; k < 8; k++) {   // fallen blocks at the foot
            int bx = cx - 30 + r.nextInt(60), bw = 5 + r.nextInt(5), bh = 3 + r.nextInt(3), by = base + 1 + r.nextInt(8);
            p.rect(bx, by, bw, bh, stone[1]);
            p.rect(bx, by, bw, 1, stone[2]);
        }
    }
}