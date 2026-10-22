package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.Screen;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Random;

// The encounter cut-scene that plays between the map and the battle:
//   the world's animated battle backdrop fades in under cinematic bars, the knight and the enemy slide in from opposite sides,
//   glare at each other (the camera creeps in), pull back, then CHARGE and CLASH in the middle: a freeze-frame, a huge spark,
//   shockwaves, flying sparks and a screen shake, and the whole thing blows out to white, which is where the battle begins
//   (the battle opens with its own white flash, so the join is seamless).
// ENTER skips the slide-in and goes straight to the charge.
// PlayLevelScreen runs this first, and when isDone() turns true it starts the battle.
public final class EncounterScreen extends Screen {
    // ---- the timeline, in frames (60 per second). Tweak freely.
    private static final int BARS_IN = 16;            // cinematic bars slide in and the backdrop fades up from black
    private static final int SLIDE_END = 54;          // both fighters have slid in
    private static final int PULLBACK_START = 78;     // they lean back, gathering force
    private static final int CHARGE_START = 92;       // and rush at each other
    private static final int IMPACT = 108;            // the clash
    private static final int HITSTOP = 6;             // frames everything freezes at the moment of impact
    private static final int END = IMPACT + 54;       // done: fully white
    private static final int GROUND_Y = 442;          // the line both fighters stand on

    private final BattleWorld world;
    private final String enemyName;

    private BattleBackgroundScreen backdrop;
    private BattleSprite knight, foe;
    private int t;
    private boolean done;
    private final KeyLocker keyLocker = new KeyLocker();

    private static final class Spark { double x, y, vx, vy; int life, max; Color color; double len; }
    private Spark[] sparks;
    private Random rng;

    public EncounterScreen(BattleWorld world, String enemyName) {
        this.world = world;
        this.enemyName = enemyName;
    }

    @Override
    public void initialize() {
        t = 0;
        done = false;
        rng = new Random(enemyName.hashCode() * 31L + 7);
        backdrop = new BattleBackgroundScreen(world);
        backdrop.initialize();
        knight = BattleSprites.knight();
        foe = BattleSprites.enemy(enemyName);
        sparks = null;
        keyLocker.lockKey(Key.ENTER);
        keyLocker.lockKey(Key.SPACE);
    }

    public boolean isDone() { return done; }

    private boolean tap(Key k) {
        if (Keyboard.isKeyDown(k) && !keyLocker.isKeyLocked(k)) { keyLocker.lockKey(k); return true; }
        if (Keyboard.isKeyUp(k)) keyLocker.unlockKey(k);
        return false;
    }

    @Override
    public void update() {
        boolean skip = tap(Key.ENTER) | tap(Key.SPACE);
        if (skip && t > 10 && t < CHARGE_START) t = CHARGE_START;
        if (t >= IMPACT && t < IMPACT + HITSTOP) {
            if (t == IMPACT) startImpact();
            t++;
            return;                                  // freeze-frame: the backdrop and sprites hold still
        }
        t++;
        backdrop.update();
        knight.update();
        foe.update();
        if (sparks != null) {
            for (Spark s : sparks) {
                if (s.life <= 0) continue;
                s.x += s.vx;
                s.y += s.vy;
                s.vy += 0.32;
                s.vx *= 0.985;
                s.life--;
            }
        }
        if (t >= END) done = true;
    }

    private void startImpact() {
        knight.setPose(BattleSprite.Pose.HURT);
        foe.setPose(BattleSprite.Pose.HURT);
        sparks = new Spark[90];
        int cx = 400, cy = contactY();
        for (int i = 0; i < sparks.length; i++) {
            Spark s = new Spark();
            double a = rng.nextDouble() * Math.PI * 2, sp = 3 + rng.nextDouble() * 15;
            s.x = cx + Math.cos(a) * 10;
            s.y = cy + Math.sin(a) * 10;
            s.vx = Math.cos(a) * sp;
            s.vy = Math.sin(a) * sp - 2;
            s.max = s.life = 26 + rng.nextInt(34);
            double c = rng.nextDouble();
            s.color = c < 0.3 ? new Color(255, 255, 255) : (c < 0.65 ? new Color(255, 225, 90) : (c < 0.9 ? new Color(255, 140, 30) : new Color(230, 60, 20)));
            s.len = 4 + rng.nextDouble() * 10;
            sparks[i] = s;
        }
    }

    // ---------------------------------------------------------------- positions

    private static double easeOut(double p) { p = Math.max(0, Math.min(1, p)); return 1 - Math.pow(1 - p, 3); }
    private static double easeIn(double p) { p = Math.max(0, Math.min(1, p)); return p * p * p; }

    private int contactY() { return GROUND_Y - (int) (Math.min(knight.getDrawHeight(), foe.getDrawHeight()) * 0.55); }

    // where a fighter's centre is at this frame. side = -1 for the knight (left), +1 for the enemy (right)
    private double fighterX(int side) {
        int half = (side < 0 ? knight.getDrawWidth() : foe.getDrawWidth()) / 2;
        double offscreen = side < 0 ? -half - 30 : 800 + half + 30;
        double rest = 400 + side * (half + 190);
        double contact = 400 + side * (half + 1);
        double x;
        if (t < SLIDE_END) {
            x = offscreen + (rest - offscreen) * easeOut((t - BARS_IN / 2) / (double) (SLIDE_END - BARS_IN / 2));
        } else {
            x = rest;
            if (t >= PULLBACK_START) x += side * 16 * easeOut((t - PULLBACK_START) / 12.0);               // lean back
            if (t >= CHARGE_START) {
                double back = rest + side * 16;
                x = back + (contact - back) * easeIn((t - CHARGE_START) / (double) (IMPACT - CHARGE_START));
            }
        }
        if (t >= IMPACT) {                                                                                 // knocked back by the clash
            double k = easeOut((t - IMPACT) / 22.0);
            x = contact + side * (70 + half * 0.15) * k;
        }
        return x;
    }

    // ---------------------------------------------------------------- draw

    @Override
    public void draw(GraphicsHandler gh) {
        Graphics2D g = gh.getGraphics();
        int w = 800, h = 605;
        AffineTransform base = g.getTransform();

        // camera: creeps in during the stand-off, then shakes after the clash
        double tension = easeOut((t - SLIDE_END) / (double) (CHARGE_START - SLIDE_END));
        double zoom = 1.06 + 0.05 * tension;
        double shake = 0;
        if (t >= IMPACT) shake = 18 * Math.pow(Math.max(0, 1 - (t - IMPACT) / 20.0), 2);
        double sx = shake * (Math.sin(t * 2.7) + 0.6 * Math.sin(t * 5.1)), sy = shake * (Math.cos(t * 3.1) + 0.6 * Math.cos(t * 4.3)) * 0.7;
        g.translate(w / 2.0 + sx, h / 2.0 + sy);
        g.scale(zoom, zoom);
        g.translate(-w / 2.0, -h / 2.0);

        backdrop.draw(gh);
        drawDarkening(g, w, h, tension);
        if (t >= CHARGE_START - 4 && t < IMPACT + 4) drawSpeedLines(g, w, h);

        drawFighters(gh, g);
        if (t >= IMPACT) drawImpact(g, w, h);

        g.setTransform(base);

        // cinematic bars slide in, then out when the clash happens
        int bar = (int) (64 * easeOut(t / (double) BARS_IN));
        if (t >= IMPACT) bar = (int) (bar * Math.max(0, 1 - (t - IMPACT) / 10.0));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, bar);
        g.fillRect(0, h - bar, w, bar);

        // fade up from black at the start
        if (t < BARS_IN) {
            g.setColor(new Color(0, 0, 0, 255 - 255 * t / BARS_IN));
            g.fillRect(0, 0, w, h);
        }
        // the flash of the clash, then the long blow-out to white that hands over to the battle
        float flash = 0;
        if (t >= IMPACT) flash = (float) Math.max(0, 1 - (t - IMPACT) / 12.0);
        if (t > END - 20) flash = Math.max(flash, (float) easeOut((t - (END - 20)) / 18.0));
        if (flash > 0) {
            g.setColor(new Color(255, 255, 255, Math.min(255, (int) (flash * 255))));
            g.fillRect(0, 0, w, h);
        }
        g.setStroke(new BasicStroke(1));
    }

    private void drawDarkening(Graphics2D g, int w, int h, double tension) {
        // the world dims while the two glare at each other
        g.setColor(new Color(0, 0, 0, (int) (110 * tension) + (t >= CHARGE_START && t < IMPACT ? 30 : 0)));
        g.fillRect(-40, -40, w + 80, h + 80);
    }

    private void drawSpeedLines(Graphics2D g, int w, int h) {
        Random r = new Random(t / 2);
        double intensity = t < IMPACT ? Math.min(1.0, (t - (CHARGE_START - 4)) / 8.0) : Math.max(0, 1 - (t - IMPACT) / 4.0);
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 26; i++) {
            int y = 120 + r.nextInt(h - 240), len = 90 + r.nextInt(240), x = r.nextInt(w);
            int a = (int) (150 * intensity * (0.4 + r.nextDouble() * 0.6));
            g.setColor(new Color(255, 255, 255, a));
            g.drawLine(x, y, x + len, y);
        }
    }

    private void drawFighters(GraphicsHandler gh, Graphics2D g) {
        int kcx = (int) Math.round(fighterX(-1)), fcx = (int) Math.round(fighterX(+1));
        // fighters bob a little while they size each other up
        int tick = t * 2;
        // motion trails while charging
        if (t >= CHARGE_START + 2 && t < IMPACT) {
            Composite old = g.getComposite();
            for (int i = 3; i >= 1; i--) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f * (4 - i)));
                knight.draw(gh, kcx - i * 18, GROUND_Y, tick);
                foe.draw(gh, fcx + i * 18, GROUND_Y, tick);
            }
            g.setComposite(old);
        }
        // contact shadows
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(kcx - knight.getDrawWidth() / 2, GROUND_Y - 14, knight.getDrawWidth(), 28);
        g.fillOval(fcx - foe.getDrawWidth() / 2, GROUND_Y - 12, foe.getDrawWidth(), 24);
        knight.draw(gh, kcx, GROUND_Y, tick);
        foe.draw(gh, fcx, GROUND_Y, tick + 40);
    }

    // the big spark and everything that flies off it
    private void drawImpact(Graphics2D g, int w, int h) {
        int local = t - IMPACT;
        int cx = 400, cy = contactY();
        Composite old = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.SrcOver);

        // shockwave rings
        for (int i = 0; i < 3; i++) {
            int lt = local - i * 4;
            if (lt < 0 || lt > 26) continue;
            double p = lt / 26.0, r = 14 + easeOut(p) * (240 + i * 60);
            g.setStroke(new BasicStroke((float) (14 * (1 - p) + 2)));
            g.setColor(new Color(255, 235 - i * 30, 160 - i * 40, (int) (230 * (1 - p))));
            g.drawOval((int) (cx - r), (int) (cy - r * 0.8), (int) (r * 2), (int) (r * 1.6));
        }

        // radiating streaks
        Random r = new Random(11);
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 28; i++) {
            double a = r.nextDouble() * Math.PI * 2, r0 = 30 + r.nextDouble() * 40, r1 = r0 + 60 + r.nextDouble() * 220;
            double p = Math.min(1, local / 14.0), fade = Math.max(0, 1 - Math.max(0, local - 6) / 18.0);
            if (fade <= 0) break;
            g.setColor(new Color(255, 250, 210, (int) (220 * fade)));
            g.drawLine((int) (cx + Math.cos(a) * (r0 + (r1 - r0) * 0.4 * p)), (int) (cy + Math.sin(a) * (r0 + (r1 - r0) * 0.4 * p)),
                    (int) (cx + Math.cos(a) * (r0 + (r1 - r0) * p)), (int) (cy + Math.sin(a) * (r0 + (r1 - r0) * p)));
        }

        // the starburst itself: a spiky white star with a hot yellow heart that grows, holds, then shrinks away
        double grow = easeOut(local / 7.0), shrink = Math.max(0, 1 - Math.max(0, local - 12) / 14.0);
        double R = 190 * grow * shrink;
        if (R > 2) {
            drawStar(g, cx, cy, R, R * 0.30, 14, 0.2, new Color(255, 255, 255, 250));
            drawStar(g, cx, cy, R * 0.62, R * 0.2, 14, 0.2 + 0.12, new Color(255, 214, 80, 255));
            drawStar(g, cx, cy, R * 0.28, R * 0.12, 8, 0.5, new Color(255, 255, 255, 255));
        }

        // flying sparks
        if (sparks != null) {
            g.setStroke(new BasicStroke(3f));
            for (Spark s : sparks) {
                if (s.life <= 0) continue;
                float a = Math.min(1f, s.life / (float) s.max * 1.6f);
                g.setColor(new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), (int) (255 * a)));
                double len = s.len * (0.5 + 0.5 * s.life / s.max);
                double mag = Math.max(0.001, Math.hypot(s.vx, s.vy));
                g.drawLine((int) s.x, (int) s.y, (int) (s.x - s.vx / mag * len), (int) (s.y - s.vy / mag * len));
            }
        }
        g.setStroke(oldStroke);
        g.setComposite(old);
    }

    private void drawStar(Graphics2D g, int cx, int cy, double outer, double inner, int points, double rot, Color color) {
        int n = points * 2;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            double a = rot + i * Math.PI / points;
            double rad = (i % 2 == 0) ? outer * (0.75 + 0.25 * ((i / 2) % 3 == 0 ? 1 : 0.55)) : inner;
            xs[i] = (int) Math.round(cx + Math.cos(a) * rad);
            ys[i] = (int) Math.round(cy + Math.sin(a) * rad * 0.9);
        }
        g.setColor(color);
        g.fillPolygon(xs, ys, n);
    }
}