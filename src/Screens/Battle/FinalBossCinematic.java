package Screens.Battle;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

// FINAL BOSS: the anime-style cinematic that plays every time one of Dabbah's forms falls (and after the last one).
// FinalBossPresentation starts it, the battle's text waits for it, and when it ends the 3D scene takes over with the old
// form breaking apart and the next form rising: the transformation is the last part of the cinematic, then the fight goes on.
//
// The beats (frames at 60 fps):
//   1 IMPACT    (~1 s)    IMPACT FRAMES: the killing blow freezes as white shapes on black, then black on white, with cracks
//                         running out from the hit; then the frozen scene slowly zooms in on the hit, tinted red, speed lines
//   2 VERSUS    (~1.5 s)  the screen splits on a diagonal: the knight's face above, Dabbah's eye below; Dabbah speaks
//   3 RUSH      (~0.8 s)  both charge at each other across streaming speed lines and meet: another impact frame
//   4 CLASH     (3 s +)   a BEAM STRUGGLE: the knight's beam against Dabbah's (five beams for the hydra) with a ball of
//                         energy between them. Dabbah keeps pushing it toward the knight; MASH ENTER (or SPACE) to push it
//                         back. Push it all the way into Dabbah to win. Let it reach the knight and he dies (6).
//   5 BREAK     (~2 s)    the ball drives into Dabbah: impact frames, a blinding explosion and a shockwave, white-out,
//                         Dabbah's last line
// then FinalBossPresentation plays the transformation (or, after the last form, the final collapse).
//   6 SLAIN     (~4 s)    the struggle was lost: Dabbah's beam swallows the knight's and the ball drives into him (impact
//                         frames), three claw slashes cut him apart under Dabbah's eye, he falls, the screen goes dark:
//                         "THE KNIGHT HAS FALLEN". The battle then ends in a DEFEAT (game over), even in test mode.
//
// CINEMATIC CHANGES (this round):
//   - the BEAMS have detail: a haze, a flickering jagged edge, two energy strands twisting round the body, pulse rings racing
//     toward the clash, sparks and crackling bolts flying off, a white-hot core, and a star flare where each beam leaves its
//     caster. The ball where they meet has spinning rays, shockwave rings, and debris kicked up off the ground under it.
//   - DABBAH'S EYE is pixel art now (drawn small, blown up with hard pixels) and meaner: a slanted, glaring almond under a
//     heavy brow ridge, horned scales, a blood-red bloodshot sclera, a burning ringed iris, a jagged slit pupil with a hot rim,
//     black ichor dripping from the corner. The Judgement cut-in uses the same eye.
//   - the struggle CAN BE LOST (6) and gets HARDER EACH FORM (CLASH_* below): Dabbah pushes harder, each tap pushes back less,
//     and from form 2 Dabbah SURGES every few seconds (warned by a red "DABBAH SURGES!") and pushes much harder for a moment.
//
// PHOTOSENSITIVITY: impact frames are hard black/white flashes. Each burst is 2 flashes and bursts are at least a second
// apart (under 3 flashes a second). REDUCED_FLASH = true turns every impact frame into a dark red still instead.
final class FinalBossCinematic {
    static boolean REDUCED_FLASH = false;

    enum Kind { FIRST_FORM_FALLS, SECOND_FORM_FALLS, FINAL_FORM_FALLS }

    // when each beat starts (frames)
    private static final int VERSUS = 56, RUSH = 150, CLASH = 196;
    private static final int BREAK_LEN = 122;   // from the end of the struggle to the end of the cinematic
    private static final int SLAIN_LEN = 250;   // from losing the struggle to the end of the cinematic

    // CINEMATIC: how hard the struggle is, per form (index 0 = after form 1 falls, 1 = form 2, 2 = the final form).
    // The ball goes from -1 (it reaches the knight: he dies) to +1 (it reaches Dabbah: you win), starting at 0.
    // Holding it still takes about 2.3 / 3.0 / 3.0 taps a second; winning in ~4 s takes about 6 / 7 / 6.5 taps a second.
    // CINEMATIC: the final form's struggle was reined back in (it needed ~8 taps a second): now just above form 2's.
    private static final float[] CLASH_TAP = {0.080f, 0.070f, 0.075f};          // each ENTER pushes the ball back this far
    private static final float[] CLASH_DRAIN = {0.0030f, 0.0035f, 0.0034f};     // Dabbah pushes this far every frame
    private static final int[] CLASH_SURGE_EVERY = {0, 150, 160};               // frames between surges (0 = none)
    private static final float[] CLASH_SURGE_DRAIN = {0f, 0.0060f, 0.0050f};    // extra push per frame while surging
    private static final int SURGE_WARN = 30, SURGE_LEN = 24, SURGE_FIRST = 60; // warning, surge, first surge after start

    private static final String[][] LINES = {
            { "...SO THIS IS WHAT THEY SENT AGAINST ME.", "THE JUDGEMENT IS NOT FINISHED." },
            { "EVERY BLOW YOU STRIKE IS MORE EVIDENCE.", "THEN SEE WHAT I WAS MADE TO BECOME." },
            { "FEN SHIELDED ME. WHO WILL SHIELD YOU?", "...THE TALLY... IS... COMPLETE." } };
    private static final String SLAIN_LINE = "JUDGED... AND FOUND WANTING.";

    private static final int GOLD = 0xffc840, GOLD_DARK = 0x2a1a06;

    private final Kind kind;
    private final int W, H;
    private final BufferedImage scene;                     // the battle at the moment of the killing blow
    private final BufferedImage subjWhite, subjBlack;      // the fighters (and effects) as flat white / black shapes
    private final BufferedImage knight, knightWhite, knightBlack, knightDark;
    private final BufferedImage boss, bossWhite, bossBlack;
    private final int hitX, hitY;
    private final int[] beam;                              // Dabbah's beam colour(s): one for the dragon, five for the hydra

    private int t, clashEnd = -1, lostAt = -1;
    private float push, shake;

    FinalBossCinematic(Kind kind, int w, int h, BufferedImage scene, BufferedImage subjects, BufferedImage knight,
                       BufferedImage boss, int hitX, int hitY, int[] beamColors) {
        this.kind = kind;
        this.W = w;
        this.H = h;
        this.scene = scene;
        this.subjWhite = flat(subjects, 0xffffff);
        this.subjBlack = flat(subjects, 0x000000);
        this.knight = knight;
        this.knightWhite = flat(knight, 0xffffff);
        this.knightBlack = flat(knight, 0x000000);
        this.knightDark = darkened(knight);
        this.boss = boss;
        this.bossWhite = flat(boss, 0xffffff);
        this.bossBlack = flat(boss, 0x000000);
        this.hitX = hitX;
        this.hitY = hitY;
        this.beam = beamColors;
    }

    boolean done() {
        return (clashEnd >= 0 && t >= clashEnd + BREAK_LEN) || (lostAt >= 0 && t >= lostAt + SLAIN_LEN);
    }

    // CINEMATIC: true when the struggle was lost (the knight died): the battle ends in a defeat
    boolean lost() { return lostAt >= 0; }

    // 0 = no surge, 1 = the warning, 2 = Dabbah surging
    private int surgeStage() {
        int f = kind.ordinal(), c = t - CLASH;
        if (CLASH_SURGE_EVERY[f] == 0 || c < SURGE_FIRST || clashEnd >= 0 || lostAt >= 0) return 0;
        int sp = (c - SURGE_FIRST) % CLASH_SURGE_EVERY[f];
        return sp < SURGE_WARN ? 1 : (sp < SURGE_WARN + SURGE_LEN ? 2 : 0);
    }

    // mash = ENTER / SPACE was tapped this frame
    void update(boolean mash) {
        t++;
        if (t >= CLASH && clashEnd < 0 && lostAt < 0) {
            int c = t - CLASH, f = kind.ordinal();
            float drain = CLASH_DRAIN[f] * (1f + Math.max(0, c - 600) / 600f);   // after 10 s it slowly gets harder (no stalemates)
            if (surgeStage() == 2) { drain += CLASH_SURGE_DRAIN[f]; shake = Math.max(shake, 5); }
            push -= drain;
            if (mash) { push += CLASH_TAP[f]; shake = Math.max(shake, 7); }
            if (push >= 1f) { push = 1f; clashEnd = t; shake = 14; }
            else if (push <= -1f) { push = -1f; lostAt = t; shake = 20; }
        }
        if (lostAt >= 0 && t - lostAt == 32) shake = 16;   // the first claw slash
        shake *= 0.86f;
        if (t == 1 || t == RUSH + 40) shake = 18;
    }

    // ================================================================= drawing

    void draw(GraphicsHandler gh) {
        Graphics2D g = (Graphics2D) gh.getGraphics().create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        float sx = shake * (float) Math.sin(t * 2.3), sy = shake * 0.7f * (float) Math.cos(t * 3.1);
        g.translate(sx, sy);
        if (t < VERSUS) drawImpact(g);
        else if (t < RUSH) drawVersus(g);
        else if (t < CLASH) drawRush(g);
        else if (lostAt >= 0) drawSlain(g);
        else if (clashEnd < 0 || t < clashEnd + 20) drawClash(g, true);
        else drawBreak(g);
        g.translate(-sx, -sy);
        // cinematic bars
        int bar = Math.round(H * 0.07f);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, bar);
        g.fillRect(0, H - bar, W, bar);
        drawText(gh);
        g.dispose();
    }

    // ---- 1 IMPACT: the killing blow
    private void drawImpact(Graphics2D g) {
        if (t < 4) { inkFrame(g, subjWhite, false); cracks(g, hitX, hitY, 0xff2a20, t); return; }
        if (t < 8) { inkFrame(g, subjBlack, true); cracks(g, hitX, hitY, 0x000000, t); return; }
        float k = ease((t - 8) / 48f), z = 1f + 0.22f * k;
        g.drawImage(scene, Math.round(hitX - hitX * z), Math.round(hitY - hitY * z), Math.round(W * z), Math.round(H * z), null);
        g.setColor(new Color(120, 0, 10, 90));
        g.fillRect(-20, -20, W + 40, H + 40);
        radialLines(g, hitX, hitY, 70, new Color(255, 255, 255, 150), 140, W, H, t / 2);
    }

    // ---- 2 VERSUS: the split screen
    private void drawVersus(Graphics2D g) {
        int v = t - VERSUS;
        float inA = ease(v / 14f), inB = ease((v - 4) / 14f);
        // the knight, above the diagonal
        Shape top = poly(0, 0, W, 0, W, H * 0.38f, 0, H * 0.62f);
        Graphics2D a = (Graphics2D) g.create();
        a.translate(-W * (1 - inA), 0);
        a.clip(top);
        a.setColor(rgb(GOLD_DARK));
        a.fill(top);
        horizLines(a, new Color(255, 200, 80, 120), 50, 11, 34);
        float kh = H * 1.7f, kw = kh * knight.getWidth() / Math.max(1, knight.getHeight());
        float zoom = 1f + 0.04f * v / 94f;
        a.drawImage(knight, Math.round(W * 0.12f), Math.round(-H * 0.06f), Math.round(kw * zoom), Math.round(kh * zoom), null);
        a.dispose();
        // Dabbah's eye, below it, with a red glow behind it
        Shape bottom = poly(0, H * 0.62f + 10, W, H * 0.38f + 10, W, H, 0, H);
        Graphics2D b = (Graphics2D) g.create();
        b.translate(W * (1 - inB), 0);
        b.clip(bottom);
        b.setColor(new Color(20, 1, 4));
        b.fill(bottom);
        float glow = 0.6f + 0.4f * (float) Math.sin(t * 0.12);
        b.setPaint(new RadialGradientPaint(W * 0.64f, H * 0.71f, H * 0.45f, new float[]{0f, 1f},
                new Color[]{new Color(150, 0, 12, Math.round(150 * glow)), new Color(20, 1, 4, 0)}));
        b.fill(bottom);
        horizLines(b, rgba(beam[0], 110), 50, 23, -34);
        drawEye(b, W * 0.64f, H * 0.71f, H * 0.2f, beam[0], 1f - 0.85f * ease((v - 30) / 40f), t);
        b.dispose();
        // the white stripe between them
        g.setStroke(new BasicStroke(8));
        g.setColor(Color.WHITE);
        g.draw(new Line2D.Float(-10, H * 0.62f + 5, W + 10, H * 0.38f + 5));
        g.setStroke(new BasicStroke(1));
    }

    // ---- 3 RUSH: they charge and meet
    private void drawRush(Graphics2D g) {
        int r = t - RUSH;
        if (r >= 40 && r < 44) { meetFrame(g, false); return; }
        if (r >= 44) { meetFrame(g, true); return; }
        g.setColor(new Color(8, 6, 16));
        g.fillRect(-20, -20, W + 40, H + 40);
        horizLines(g, new Color(255, 255, 255, 90), 70, 31, 60);
        float k = ease(r / 40f);
        float kh = H * 0.46f, kw = kh * knight.getWidth() / Math.max(1, knight.getHeight());
        float kx = -W * 0.3f + (W * 0.38f + W * 0.3f) * k, ky = H * 0.88f - kh;
        float bh = H * 0.72f, bw = bh * boss.getWidth() / Math.max(1, boss.getHeight());
        float bx = W * 1.1f + (W * 0.5f - W * 1.1f) * k, by = H * 0.9f - bh;
        Composite old = g.getComposite();
        for (int i = 3; i >= 1; i--) {   // motion trails
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f * (4 - i)));
            g.drawImage(knight, Math.round(kx - i * 40), Math.round(ky), Math.round(kw), Math.round(kh), null);
            g.drawImage(boss, Math.round(bx + i * 50), Math.round(by), Math.round(bw), Math.round(bh), null);
        }
        g.setComposite(old);
        g.drawImage(boss, Math.round(bx), Math.round(by), Math.round(bw), Math.round(bh), null);
        g.drawImage(knight, Math.round(kx), Math.round(ky), Math.round(kw), Math.round(kh), null);
    }

    private void meetFrame(Graphics2D g, boolean inverted) {
        float kh = H * 0.46f, kw = kh * knight.getWidth() / Math.max(1, knight.getHeight());
        float bh = H * 0.72f, bw = bh * boss.getWidth() / Math.max(1, boss.getHeight());
        if (REDUCED_FLASH) {
            g.setColor(new Color(60, 0, 6));
            g.fillRect(-20, -20, W + 40, H + 40);
            g.drawImage(bossBlack, Math.round(W * 0.5f), Math.round(H * 0.9f - bh), Math.round(bw), Math.round(bh), null);
            g.drawImage(knightBlack, Math.round(W * 0.38f), Math.round(H * 0.88f - kh), Math.round(kw), Math.round(kh), null);
            return;
        }
        g.setColor(inverted ? Color.WHITE : Color.BLACK);
        g.fillRect(-20, -20, W + 40, H + 40);
        g.drawImage(inverted ? bossBlack : bossWhite, Math.round(W * 0.5f), Math.round(H * 0.9f - bh), Math.round(bw), Math.round(bh), null);
        g.drawImage(inverted ? knightBlack : knightWhite, Math.round(W * 0.38f), Math.round(H * 0.88f - kh), Math.round(kw), Math.round(kh), null);
        cracks(g, Math.round(W * 0.52f), Math.round(H * 0.55f), inverted ? 0x000000 : 0xff2a20, t);
    }

    // ---- 4 CLASH: the beam struggle

    // 0 normally; 0..1 over the first frames after the struggle is lost (the ball races into the knight)
    private float losing() { return lostAt >= 0 ? ease((t - lostAt) / 22f) : 0f; }

    private float clashX() {
        float x = W * 0.3f + W * 0.42f * (push + 1) / 2f;
        return x + (W * 0.15f - x) * losing();
    }

    private void drawClash(Graphics2D g, boolean ui) {
        int c = t - CLASH;
        boolean hydra = beam.length > 1;
        float lose = losing();
        boolean surging = surgeStage() == 2;
        // sky, ground, debris
        g.setPaint(new GradientPaint(0, 0, new Color(6, 2, 10), 0, H * 0.8f, hydra ? new Color(44, 8, 70) : new Color(60, 8, 10)));
        g.fillRect(-20, -20, W + 40, H + 40);
        g.setColor(new Color(14, 10, 12));
        g.fillRect(-20, Math.round(H * 0.8f), W + 40, H);
        Random d = new Random(77);
        for (int i = 0; i < 40; i++) {
            float x = d.nextFloat() * W, sp = 0.6f + d.nextFloat() * 2.2f, s = 3 + d.nextInt(9);
            float y = H * 0.82f - ((c * sp + d.nextFloat() * H) % (H * 0.8f));
            g.setColor(new Color(60, 46, 40));
            g.fillRect(Math.round(x), Math.round(y), Math.round(s), Math.round(s));
        }
        float cx = clashX(), cy = H * 0.46f + 6 * (float) Math.sin(t * 0.3);
        radialLines(g, cx, cy, 46, new Color(255, 255, 255, 50), 90, W, H, t / 2);
        // a trench of glowing ground and debris kicked up under the ball
        g.setColor(rgba(beam[0], 60));
        g.fillOval(Math.round(cx - 90), Math.round(H * 0.8f - 10), 180, 22);
        Random kick = new Random(t / 2 * 53L);
        for (int i = 0; i < 12; i++) {
            float age = ((t + i * 7) % 28) / 28f;
            float x = cx + (kick.nextFloat() - 0.5f) * 160, y = H * 0.8f - age * (60 + i * 6);
            int s = 3 + (i % 4) * 2;
            g.setColor(i % 3 == 0 ? rgba(beam[0], Math.round(220 * (1 - age))) : new Color(70, 54, 46, Math.round(255 * (1 - age))));
            g.fillRect(Math.round(x), Math.round(y), s, s);
        }
        // the fighters
        float kh = H * 0.42f, kw = kh * knight.getWidth() / Math.max(1, knight.getHeight());
        g.drawImage(knight, Math.round(W * 0.14f - kw / 2), Math.round(H * 0.8f - kh), Math.round(kw), Math.round(kh), null);
        float bh = H * 0.72f, bw = bh * boss.getWidth() / Math.max(1, boss.getHeight());
        g.drawImage(boss, Math.round(W * 0.8f - bw / 2), Math.round(H * 0.8f - bh), Math.round(bw), Math.round(bh), null);
        // the beams (Dabbah's grow while it surges and when it wins; the knight's dies out when he loses)
        float wob = (float) Math.sin(t * 0.7);
        float bossW = (surging ? 1.25f : 1f) * (1f + 0.6f * lose);
        richBeam(g, W * 0.22f, H * 0.5f, cx, cy, (34 + 3 * wob) * (1f - lose), GOLD, 0xffffff, 1);
        if (hydra) {
            for (int i = 0; i < beam.length; i++)
                richBeam(g, W * 0.66f + (i % 2) * W * 0.04f, H * 0.18f + i * H * 0.08f, cx, cy, (18 + 2 * wob) * bossW, beam[i], 0xffffff, 10 + i);
        } else {
            richBeam(g, W * 0.68f, H * 0.38f, cx, cy, (40 + 3 * wob) * bossW, beam[0], 0xfff0e0, 10);
        }
        drawBall(g, cx, cy);
        if (!ui || clashEnd >= 0 || lostAt >= 0) return;
        // the tug-of-war bar (gold = the knight's side, Dabbah's colour = its side); it flashes red while Dabbah surges
        int bw2 = Math.round(W * 0.56f), bx = (W - bw2) / 2, by = Math.round(H * 0.11f);
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(bx - 4, by - 4, bw2 + 8, 22);
        g.setColor(rgb(beam[0]));
        g.fillRect(bx, by, bw2, 14);
        g.setColor(rgb(GOLD));
        g.fillRect(bx, by, Math.round(bw2 * (push + 1) / 2f), 14);
        g.setColor(Color.WHITE);
        g.fillRect(bx + bw2 / 2 - 1, by - 2, 2, 18);   // the middle
        g.setColor(surgeStage() > 0 && (t / 4) % 2 == 0 ? new Color(230, 20, 30) : Color.WHITE);
        g.drawRect(bx - 4, by - 4, bw2 + 8, 22);
        // the knight's skull end and Dabbah's end of the bar
        g.setColor(new Color(230, 20, 30));
        g.fillRect(bx - 16, by, 8, 14);
    }

    // the ball of energy where the beams meet: gold on the knight's side, Dabbah's colour on its side, spinning rays,
    // shockwave rings, crackling bolts
    private void drawBall(Graphics2D g, float cx, float cy) {
        float r = 44 + 8 * (float) Math.sin(t * 0.9) + 10 * Math.abs(push);
        // shockwave rings, one every 18 frames
        g.setStroke(new BasicStroke(3));
        for (int k = 0; k < 3; k++) {
            float p = ((t + k * 6) % 18) / 18f;
            g.setColor(new Color(255, 255, 255, Math.round(160 * (1 - p))));
            float rr = r * (1.2f + p * 2.4f);
            g.drawOval(Math.round(cx - rr), Math.round(cy - rr * 0.8f), Math.round(rr * 2), Math.round(rr * 1.6f));
        }
        // spinning rays
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14 + t * 0.06 * (i % 2 == 0 ? 1 : -1);
            float len = r * (1.9f + 0.5f * (float) Math.sin(t * 0.4 + i));
            int[] xs = { Math.round(cx + (float) Math.cos(a - 0.06) * r * 0.8f), Math.round(cx + (float) Math.cos(a) * len), Math.round(cx + (float) Math.cos(a + 0.06) * r * 0.8f) };
            int[] ys = { Math.round(cy + (float) Math.sin(a - 0.06) * r * 0.8f), Math.round(cy + (float) Math.sin(a) * len), Math.round(cy + (float) Math.sin(a + 0.06) * r * 0.8f) };
            g.setColor(i % 2 == 0 ? new Color(255, 230, 150, 170) : rgba(beam[0], 170));
            g.fillPolygon(xs, ys, 3);
        }
        // the layered ball: two halves of colour, then hotter towards the middle
        float rr = r * 1.25f;
        Graphics2D h = (Graphics2D) g.create();
        h.clipRect(Math.round(cx - rr - 2), Math.round(cy - rr - 2), Math.round(rr + 2), Math.round(rr * 2 + 4));
        h.setColor(new Color(255, 190, 60, 150));
        h.fillOval(Math.round(cx - rr), Math.round(cy - rr), Math.round(rr * 2), Math.round(rr * 2));
        h.dispose();
        h = (Graphics2D) g.create();
        h.clipRect(Math.round(cx), Math.round(cy - rr - 2), Math.round(rr + 2), Math.round(rr * 2 + 4));
        h.setColor(rgba(beam[0], 160));
        h.fillOval(Math.round(cx - rr), Math.round(cy - rr), Math.round(rr * 2), Math.round(rr * 2));
        h.dispose();
        for (int i = 3; i >= 1; i--) {
            g.setColor(i == 3 ? new Color(255, 220, 140, 150) : (i == 2 ? new Color(255, 245, 210, 220) : Color.WHITE));
            float q = r * i / 3.2f;
            g.fillOval(Math.round(cx - q), Math.round(cy - q), Math.round(q * 2), Math.round(q * 2));
        }
        Random l = new Random(t / 3 * 41L);
        g.setStroke(new BasicStroke(2.5f));
        for (int i = 0; i < 6; i++) bolt(g, cx, cy, l.nextDouble() * Math.PI * 2, r * 1.6f + l.nextInt(90), i % 2 == 0 ? Color.WHITE : rgba(beam[0], 230), l);
        g.setStroke(new BasicStroke(1));
    }

    // ---- 5 BREAK: the ball drives into Dabbah, impact frames, explosion, white-out
    private void drawBreak(Graphics2D g) {
        int b = t - clashEnd - 20;
        float bh = H * 0.72f, bw = bh * boss.getWidth() / Math.max(1, boss.getHeight());
        int bx = Math.round(W * 0.8f - bw / 2), by = Math.round(H * 0.8f - bh);
        int ex = Math.round(W * 0.78f), ey = Math.round(H * 0.5f);
        if (b < 8 && !REDUCED_FLASH) {
            boolean inv = b >= 4;
            g.setColor(inv ? Color.WHITE : Color.BLACK);
            g.fillRect(-20, -20, W + 40, H + 40);
            g.drawImage(inv ? bossBlack : bossWhite, bx, by, Math.round(bw), Math.round(bh), null);
            cracks(g, ex, ey, inv ? 0x000000 : 0xff2a20, t);
            return;
        }
        drawClash(g, false);
        if (b < 8) { g.setColor(new Color(90, 0, 8, 200)); g.fillRect(-20, -20, W + 40, H + 40); return; }   // REDUCED_FLASH
        float k = ease((b - 8) / 46f), maxR = (float) Math.hypot(W, H);
        float r = k * maxR, ring = Math.min(maxR * 1.2f, r * 1.25f + 30);
        radialLines(g, ex, ey, 60, new Color(255, 250, 230, 200), Math.max(10, r * 0.5f), W, H, t / 2);
        g.setColor(new Color(255, 255, 255, 230));
        g.setStroke(new BasicStroke(10));
        g.drawOval(Math.round(ex - ring), Math.round(ey - ring), Math.round(ring * 2), Math.round(ring * 2));
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 250, 235));
        g.fillOval(Math.round(ex - r), Math.round(ey - r), Math.round(r * 2), Math.round(r * 2));
        if (b >= 54) { g.setColor(new Color(255, 252, 245)); g.fillRect(-20, -20, W + 40, H + 40); }
    }

    // ---- 6 SLAIN: the struggle was lost. Dabbah's beam swallows the knight, then it tears him apart.
    private void drawSlain(Graphics2D g) {
        int e = t - lostAt;
        if (e < 24) {   // the ball races back into the knight, Dabbah's colour floods the screen
            drawClash(g, false);
            g.setColor(rgba(beam[0], Math.round(160 * e / 24f)));
            g.fillRect(-20, -20, W + 40, H + 40);
            return;
        }
        float kh = H * 0.62f, kw = kh * knight.getWidth() / Math.max(1, knight.getHeight());
        float kx = W * 0.36f - kw / 2, ky = H * 0.84f - kh;
        if (e < 32) {   // impact frames: the knight as a flat shape, cracks out from his chest (2 flashes)
            if (REDUCED_FLASH) {
                g.setColor(new Color(60, 0, 6));
                g.fillRect(-20, -20, W + 40, H + 40);
                g.drawImage(knightBlack, Math.round(kx), Math.round(ky), Math.round(kw), Math.round(kh), null);
                return;
            }
            boolean inv = e >= 28;
            g.setColor(inv ? Color.WHITE : Color.BLACK);
            g.fillRect(-20, -20, W + 40, H + 40);
            g.drawImage(inv ? knightBlack : knightWhite, Math.round(kx), Math.round(ky), Math.round(kw), Math.round(kh), null);
            cracks(g, Math.round(kx + kw / 2), Math.round(ky + kh * 0.42f), inv ? 0x000000 : 0xff2a20, t);
            return;
        }
        int s = e - 32;
        float groundY = H * 0.84f;
        // a blood-dark sky, Dabbah looming as a black shape, its eye watching
        g.setPaint(new GradientPaint(0, 0, new Color(34, 0, 5), 0, H, new Color(6, 0, 2)));
        g.fillRect(-20, -20, W + 40, H + 40);
        float bh = H * 1.05f, bw = bh * boss.getWidth() / Math.max(1, boss.getHeight());
        g.drawImage(bossBlack, Math.round(W * 0.86f - bw / 2), Math.round(groundY - bh), Math.round(bw), Math.round(bh), null);
        g.setColor(new Color(10, 4, 6));
        g.fillRect(-20, Math.round(groundY), W + 40, H);
        drawEye(g, W * 0.76f, H * 0.27f, H * 0.11f, beam[0], 0.1f, t);

        // the cut runs through his chest; after the slashes the two halves come apart
        float cx0 = kx - kw * 0.15f, cy0 = ky + kh * 0.32f, cx1 = kx + kw * 1.15f, cy1 = ky + kh * 0.52f;
        float dx = cx1 - cx0, dy = cy1 - cy0, dl = (float) Math.hypot(dx, dy);
        dx /= dl;
        dy /= dl;
        float lx = cx0 - dx * W, ly = cy0 - dy * W, rx = cx1 + dx * W, ry = cy1 + dy * W;
        Shape above = poly(lx, ly, rx, ry, rx, -H * 2, lx, -H * 2);
        Shape below = poly(lx, ly, rx, ry, rx, H * 3, lx, H * 3);
        float apart = s < 26 ? 0 : ease((s - 26) / 60f);
        // the blood pool, spreading under him
        float pool = ease((s - 40) / 90f) * kw * 1.6f;
        if (pool > 1) {
            g.setColor(new Color(90, 0, 8));
            g.fillOval(Math.round(kx + kw / 2 - pool / 2), Math.round(groundY - 6), Math.round(pool), 14);
        }
        // the lower half slumps, the upper half slides off along the cut and topples to the ground
        Graphics2D lo = (Graphics2D) g.create();
        lo.rotate(apart * 0.12, kx + kw / 2, ky + kh);
        lo.translate(0, apart * kh * 0.06f);
        lo.clip(below);
        lo.drawImage(knightDark, Math.round(kx), Math.round(ky), Math.round(kw), Math.round(kh), null);
        lo.dispose();
        Graphics2D up = (Graphics2D) g.create();
        up.translate(-apart * kw * 0.45f, apart * apart * (groundY - cy0) * 0.75f);
        up.rotate(-apart * 1.1, (cx0 + cx1) / 2, (cy0 + cy1) / 2);
        up.clip(above);
        up.drawImage(knightDark, Math.round(kx), Math.round(ky), Math.round(kw), Math.round(kh), null);
        up.dispose();
        // blood spraying from the cut, falling and staying where it lands
        for (int i = 0; i < 110; i++) {
            Random rr = new Random(i * 977L + 13);
            int birth = 4 + rr.nextInt(40);
            if (s < birth) continue;
            float dt = s - birth, along = rr.nextFloat();
            float x0 = cx0 + (cx1 - cx0) * along, y0 = cy0 + (cy1 - cy0) * along;
            float vx = (rr.nextFloat() - 0.4f) * 7f, vy = -2f - rr.nextFloat() * 8f;
            float x = x0 + vx * dt, y = y0 + vy * dt + 0.2f * dt * dt;
            if (y > groundY) { y = groundY + rr.nextInt(8); x = Math.min(x, x0 + vx * (dt * 0.6f)); }
            int sz = 3 + rr.nextInt(5);
            g.setColor(i % 3 == 0 ? new Color(190, 10, 22) : new Color(110, 0, 10));
            g.fillRect(Math.round(x), Math.round(y), sz, sz);
        }
        // three claw slashes: each streaks across in 5 frames, then stays as a fading red gash
        for (int k = 0; k < 3; k++) {
            int st = k * 6;
            if (s < st) continue;
            float grow = ease((s - st) / 5f), fadeOut = 1f - Math.min(1f, Math.max(0, s - st - 10) / 50f);
            if (fadeOut <= 0) continue;
            float off = (k - 1) * kh * 0.13f;
            float ax = cx0 - kw * 0.5f, ay = cy0 + off - kh * 0.25f, bx2 = cx1 + kw * 0.5f, by2 = cy1 + off + kh * 0.25f;
            float ex2 = ax + (bx2 - ax) * grow, ey2 = ay + (by2 - ay) * grow;
            Line2D.Float slash = new Line2D.Float(ax, ay, ex2, ey2);
            stroke(g, slash, 26, new Color(160, 0, 14, Math.round(160 * fadeOut)));
            stroke(g, slash, 12, new Color(235, 30, 40, Math.round(230 * fadeOut)));
            if (s - st < 10) stroke(g, slash, 4, Color.WHITE);
        }
        // the dark closes in
        if (e >= 150) {
            g.setColor(new Color(0, 0, 0, Math.round(255 * ease((e - 150) / 50f))));
            g.fillRect(-20, -20, W + 40, H + 40);
        }
    }

    // subtitles: Dabbah's lines, the struggle prompts, and the death
    private void drawText(GraphicsHandler gh) {
        int capY = H - Math.round(H * 0.07f) - 40;
        if (lostAt >= 0) {
            int e = t - lostAt;
            if (e >= 60 && e < 160) caption(gh, SLAIN_LINE, capY, false);
            if (e >= 165) {
                int a = Math.round(255 * ease((e - 165) / 30f));
                BattleUI.textCenter(gh, "THE KNIGHT HAS FALLEN", W / 2 + 3, H / 2 - 14 + 3, 4, new Color(60, 0, 0, a));
                BattleUI.textCenter(gh, "THE KNIGHT HAS FALLEN", W / 2, H / 2 - 14, 4, new Color(200, 16, 28, a));
            }
            return;
        }
        String[] lines = LINES[kind.ordinal()];
        if (t >= VERSUS + 20 && t < RUSH) caption(gh, lines[0], capY, false);
        if (clashEnd >= 0 && t >= clashEnd + 20 + 60) caption(gh, lines[1], capY, true);
        if (t >= CLASH && clashEnd < 0) {
            int stage = surgeStage();
            if (stage > 0) {   // the warning blinks, the surge itself is solid
                if (stage == 2 || (t / 5) % 2 == 0) {
                    BattleUI.textCenter(gh, "DABBAH SURGES!", W / 2 + 3, Math.round(H * 0.16f) + 3, 3, new Color(60, 0, 0));
                    BattleUI.textCenter(gh, "DABBAH SURGES!", W / 2, Math.round(H * 0.16f), 3, new Color(235, 30, 40));
                }
            } else if ((t / 12) % 2 == 0) {
                BattleUI.textCenter(gh, "MASH ENTER!", W / 2 + 3, Math.round(H * 0.16f) + 3, 3, new Color(80, 40, 0));
                BattleUI.textCenter(gh, "MASH ENTER!", W / 2, Math.round(H * 0.16f), 3, rgb(GOLD));
            }
        }
    }

    private void caption(GraphicsHandler gh, String text, int y, boolean onWhite) {
        BattleUI.textCenter(gh, text, W / 2 + 2, y + 2, 2, onWhite ? new Color(200, 180, 170) : new Color(90, 0, 0));
        BattleUI.textCenter(gh, text, W / 2, y, 2, onWhite ? new Color(40, 0, 6) : Color.WHITE);
    }

    // ================================================================= shared drawing helpers (FinalBossPresentation uses them too)

    // an impact frame: the whole screen black (or white), the fighters as flat shapes
    private void inkFrame(Graphics2D g, BufferedImage shapes, boolean inverted) {
        if (REDUCED_FLASH) {
            g.drawImage(scene, 0, 0, null);
            g.setColor(new Color(70, 0, 8, 190));
            g.fillRect(-20, -20, W + 40, H + 40);
            return;
        }
        g.setColor(inverted ? Color.WHITE : Color.BLACK);
        g.fillRect(-20, -20, W + 40, H + 40);
        g.drawImage(shapes, 0, 0, null);
    }

    // jagged cracks running out from a point to the edges of the screen
    private void cracks(Graphics2D g, int x, int y, int color, int seed) {
        Random r = new Random(seed / 4 * 13L + 5);
        g.setColor(rgb(color));
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI * 2 / 12 + r.nextDouble() * 0.4;
            float px = x, py = y;
            g.setStroke(new BasicStroke(5));
            for (int s = 0; s < 9; s++) {
                ang += (r.nextDouble() - 0.5) * 0.6;
                float nx = px + (float) Math.cos(ang) * (40 + r.nextInt(60)), ny = py + (float) Math.sin(ang) * (40 + r.nextInt(60));
                g.draw(new Line2D.Float(px, py, nx, ny));
                g.setStroke(new BasicStroke(Math.max(1, 5 - s / 2)));
                px = nx;
                py = ny;
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    // thin wedges pointing in at (cx, cy) from all around: the anime "speed lines"
    static void radialLines(Graphics2D g, float cx, float cy, int n, Color c, float inner, int w, int h, long seed) {
        Random r = new Random(seed * 7919L + 3);
        float far = (float) Math.hypot(w, h);
        g.setColor(c);
        for (int i = 0; i < n; i++) {
            double ang = r.nextDouble() * Math.PI * 2, wd = 0.004 + r.nextDouble() * 0.012;
            float r0 = inner * (1f + r.nextFloat() * 0.9f);
            int[] xs = { Math.round(cx + (float) Math.cos(ang) * r0), Math.round(cx + (float) Math.cos(ang - wd) * far), Math.round(cx + (float) Math.cos(ang + wd) * far) };
            int[] ys = { Math.round(cy + (float) Math.sin(ang) * r0), Math.round(cy + (float) Math.sin(ang - wd) * far), Math.round(cy + (float) Math.sin(ang + wd) * far) };
            g.fillPolygon(xs, ys, 3);
        }
    }

    // streaks racing sideways across the screen (negative speed = right to left)
    private void horizLines(Graphics2D g, Color c, int n, long seed, float speed) {
        Random r = new Random(seed);
        g.setColor(c);
        for (int i = 0; i < n; i++) {
            int y = r.nextInt(H), len = 60 + r.nextInt(320), th = 1 + r.nextInt(3);
            float span = W + len, x = ((r.nextFloat() * span + t * speed * (0.7f + r.nextFloat())) % span + span) % span - len;
            g.fillRect(Math.round(x), y, len, th);
        }
    }

    // CINEMATIC: a detailed beam from its caster (x0, y0) to the clash (x1, y1):
    //   haze -> flickering jagged body -> two energy strands twisting round it -> pulse rings racing toward the clash ->
    //   white-hot core -> sparks and little bolts flying off -> a star flare where it leaves the caster
    private void richBeam(Graphics2D g, float x0, float y0, float x1, float y1, float w, int color, int core, int seed) {
        if (w < 1.5f) return;
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.hypot(dx, dy);
        if (len < 2) return;
        float ux = dx / len, uy = dy / len, nx = -uy, ny = ux;   // along and across the beam
        Line2D.Float l = new Line2D.Float(x0, y0, x1, y1);
        // haze
        stroke(g, l, w * 2.4f, rgba(color, 30));
        stroke(g, l, w * 1.6f, rgba(color, 60));
        // the body: a polygon whose edges flicker in and out
        Random r = new Random(t / 2 * 131L + seed * 977L);
        int segs = Math.max(4, Math.round(len / 16));
        int[] xs = new int[(segs + 1) * 2], ys = new int[(segs + 1) * 2];
        for (int i = 0; i <= segs; i++) {
            float p = i / (float) segs, bx = x0 + dx * p, by = y0 + dy * p;
            float taper = Math.min(1f, 0.55f + p * 3f);   // a little narrower where it leaves the caster
            float up = w * 0.55f * taper * (0.82f + 0.36f * r.nextFloat()), dn = w * 0.55f * taper * (0.82f + 0.36f * r.nextFloat());
            xs[i] = Math.round(bx + nx * up);
            ys[i] = Math.round(by + ny * up);
            xs[xs.length - 1 - i] = Math.round(bx - nx * dn);
            ys[ys.length - 1 - i] = Math.round(by - ny * dn);
        }
        g.setColor(rgba(color, 215));
        g.fillPolygon(xs, ys, xs.length);
        // the brighter inner body
        stroke(g, l, w * 0.7f, rgba(mix(color, 0xffffff, 0.45f), 230));
        // two energy strands twisting round it (the half in front is brighter)
        g.setStroke(new BasicStroke(Math.max(2f, w * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int strand = 0; strand < 2; strand++) {
            double phase = strand * Math.PI;
            float px = x0, py = y0;
            int steps = Math.max(8, Math.round(len / 7));
            for (int i = 1; i <= steps; i++) {
                float p = i / (float) steps;
                double a = p * len * 0.045 - t * 0.35 + phase;
                float off = (float) Math.sin(a) * w * 0.5f;
                float qx = x0 + dx * p + nx * off, qy = y0 + dy * p + ny * off;
                boolean front = Math.cos(a) > 0;
                g.setColor(front ? rgba(mix(color, 0xffffff, 0.75f), 235) : rgba(mix(color, 0x000000, 0.35f), 160));
                g.draw(new Line2D.Float(px, py, qx, qy));
                px = qx;
                py = qy;
            }
        }
        // pulse rings racing from the caster to the clash
        g.setStroke(new BasicStroke(Math.max(2f, w * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float spacing = Math.max(40f, w * 2.2f);
        for (float pos = (t * 9f) % spacing; pos < len; pos += spacing) {
            float bx = x0 + ux * pos, by = y0 + uy * pos, half = w * 0.68f;
            g.setColor(new Color(255, 255, 255, 140));
            g.draw(new Line2D.Float(bx + nx * half, by + ny * half, bx - nx * half, by - ny * half));
        }
        // the white-hot core
        stroke(g, l, w * 0.32f, rgb(core));
        stroke(g, l, Math.max(1f, w * 0.12f), Color.WHITE);
        // sparks thrown off the sides, and small bolts crackling along it
        for (int i = 0; i < 16; i++) {
            float pos = r.nextFloat() * len, side = r.nextBoolean() ? 1 : -1;
            float out = w * (0.6f + r.nextFloat() * 1.3f);
            int sz = 2 + r.nextInt(4);
            g.setColor(i % 3 == 0 ? Color.WHITE : rgba(mix(color, 0xffffff, 0.3f), 230));
            g.fillRect(Math.round(x0 + ux * pos + nx * out * side), Math.round(y0 + uy * pos + ny * out * side), sz, sz);
        }
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 2; i++) {
            float pos = r.nextFloat() * len;
            double ang = Math.atan2(ny, nx) + (r.nextBoolean() ? 0 : Math.PI);
            bolt(g, x0 + ux * pos, y0 + uy * pos, ang, w * 1.8f, rgba(mix(color, 0xffffff, 0.6f), 220), r);
        }
        g.setStroke(new BasicStroke(1));
        // the flare where it leaves the caster
        float fr = w * 1.2f * (0.9f + 0.2f * (float) Math.sin(t * 0.8 + seed));
        g.setColor(rgba(color, 120));
        g.fillOval(Math.round(x0 - fr), Math.round(y0 - fr), Math.round(fr * 2), Math.round(fr * 2));
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + t * 0.03, spike = fr * (i % 2 == 0 ? 2.2f : 1.4f);
            int[] sx = { Math.round(x0 + (float) Math.cos(a - 0.12) * fr * 0.4f), Math.round(x0 + (float) (Math.cos(a) * spike)), Math.round(x0 + (float) Math.cos(a + 0.12) * fr * 0.4f) };
            int[] sy = { Math.round(y0 + (float) Math.sin(a - 0.12) * fr * 0.4f), Math.round(y0 + (float) (Math.sin(a) * spike)), Math.round(y0 + (float) Math.sin(a + 0.12) * fr * 0.4f) };
            g.setColor(new Color(255, 255, 255, 200));
            g.fillPolygon(sx, sy, 3);
        }
        g.setColor(Color.WHITE);
        g.fillOval(Math.round(x0 - fr * 0.45f), Math.round(y0 - fr * 0.45f), Math.round(fr * 0.9f), Math.round(fr * 0.9f));
    }

    private static void stroke(Graphics2D g, Shape s, float w, Color c) {
        g.setStroke(new BasicStroke(Math.max(1f, w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.draw(s);
        g.setStroke(new BasicStroke(1));
    }

    private static void bolt(Graphics2D g, float x, float y, double ang, float len, Color c, Random r) {
        g.setColor(c);
        float px = x, py = y;
        for (int i = 0; i < 6; i++) {
            double a = ang + (r.nextDouble() - 0.5) * 1.2;
            float nx = px + (float) Math.cos(a) * len / 6, ny = py + (float) Math.sin(a) * len / 6;
            g.draw(new Line2D.Float(px, py, nx, ny));
            px = nx;
            py = ny;
        }
    }

    // CINEMATIC: Dabbah's eye in close-up, as PIXEL ART: drawn on a tiny canvas with hard pixels, then blown up.
    // A glaring slanted almond under a heavy brow ridge, horned scales, a bloodshot dark-red sclera, a burning ringed iris in
    // its form's colour, a jagged slit pupil with a hot rim, black ichor dripping from the inner corner.
    // (cx, cy) = the eye's centre, size = about the eye's height, open = 0..1 how wide the pupil is.
    static void drawEye(Graphics2D g, float cx, float cy, float size, int color, float open, int t) {
        int px = Math.max(3, Math.round(size / 10f));                       // one "pixel" of the art, in screen pixels
        int lw = Math.max(24, Math.round(size * 4.2f / px)), lh = Math.max(14, Math.round(size * 2.6f / px));
        BufferedImage img = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D e = img.createGraphics();   // no antialiasing: every edge is a hard pixel
        float ox = lw * 0.5f, oy = lh * 0.55f;       // the eye's centre on the little canvas
        float ew = lw * 0.36f, eh = lh * 0.29f;      // half-width / half-height of the eye

        // the head around it: an oval of dark staggered scales whose edge breaks up into loose pixels (no hard box)
        Random hr = new Random(3);
        for (int y = 0; y < lh; y++)
            for (int x = 0; x < lw; x++) {
                float nx = (x - ox) / (lw * 0.5f), ny = (y - oy) / (lh * 0.58f), d = nx * nx + ny * ny;
                float roll = hr.nextFloat();
                if (d > 1f || (d > 0.6f && roll < (d - 0.6f) * 2.5f)) continue;
                int c = 0x130810;
                boolean scaleRow = y % 3 == 2;
                int scaleCol = (x + (y / 3) % 2 * 2) % 4;
                if (scaleRow && scaleCol < 2) c = 0x2a141e;
                if (y % 3 == 1 && scaleCol == 0) c = 0x3e1a24;
                img.setRGB(x, y, 0xFF000000 | c);
            }
        // horns along the brow
        for (int i = 0; i < 5; i++) {
            float bx = ox - ew * 0.6f + i * ew * 0.42f, by = oy - eh * 1.7f - i * eh * 0.18f;
            float hh = lh * (0.12f + 0.04f * (i % 3));
            e.setColor(rgb(0x07030a));
            e.fill(poly(bx - 2, by + 2, bx + 2.5f, by - hh, bx + 3, by + 2));
            e.setColor(rgb(0x6a0610));
            e.fillRect(Math.round(bx + 2), Math.round(by - hh + 1), 1, 1);
        }
        // the slanted almond: the inner corner (left) low, the outer corner high: a glare
        Path2D.Float lid = new Path2D.Float();
        float ix = ox - ew, iy = oy + eh * 0.35f, qx = ox + ew, qy = oy - eh * 0.6f;
        lid.moveTo(ix, iy);
        lid.quadTo(ox, oy - eh * 1.55f, qx, qy);
        lid.quadTo(ox + ew * 0.1f, oy + eh * 1.3f, ix, iy);
        lid.closePath();
        // a red burning rim round the socket
        float pulse = 0.6f + 0.4f * (float) Math.sin(t * 0.15);
        e.setColor(rgba(0xa00010, Math.round(120 + 100 * pulse)));
        e.setStroke(new BasicStroke(3));
        e.draw(lid);
        e.setStroke(new BasicStroke(1));
        Graphics2D in = (Graphics2D) e.create();
        in.clip(lid);
        // bloodshot sclera
        in.setColor(rgb(0x2c0205));
        in.fill(lid.getBounds2D());
        Random vr = new Random(7);
        for (int v = 0; v < 9; v++) {
            float vx = v % 2 == 0 ? ix + 1 : qx - 1, vy = (v % 2 == 0 ? iy : qy) + vr.nextInt(5) - 2;
            in.setColor(rgba(0xb00a14, Math.round(140 + 110 * pulse)));
            for (int s = 0; s < 6; s++) {
                in.fillRect(Math.round(vx), Math.round(vy), 1, 1);
                vx += v % 2 == 0 ? 1 : -1;
                vy += vr.nextInt(3) - 1;
            }
        }
        // the iris: rings of its colour, darker outside, hotter inside, with dark streaks
        float icx = ox - ew * 0.04f, icy = oy - eh * 0.1f, ir = eh * 1.12f;
        int dark = mix(color, 0x000000, 0.55f), hot = mix(color, 0xffe070, 0.55f);
        fillCircle(in, icx, icy, ir, dark);
        fillCircle(in, icx, icy, ir * 0.82f, color);
        fillCircle(in, icx, icy, ir * 0.55f, mix(color, 0xffe070, 0.25f + 0.2f * pulse));
        Random sr = new Random(11);
        in.setColor(rgb(dark));
        for (int k = 0; k < 14; k++) {
            double a = sr.nextDouble() * Math.PI * 2;
            for (float d = ir * 0.4f; d < ir * 0.95f; d += 1f)
                if (sr.nextInt(3) > 0) in.fillRect(Math.round(icx + (float) Math.cos(a) * d), Math.round(icy + (float) Math.sin(a) * d), 1, 1);
        }
        // the slit pupil: jagged, with a hot rim
        float pw = Math.max(1f, ir * (0.05f + 0.22f * open)), ph = ir * 1.05f;
        in.setColor(rgb(hot));
        in.fill(poly(icx, icy - ph - 1, icx + pw + 1, icy - ph * 0.3f, icx + pw * 0.7f + 1, icy + ph * 0.3f, icx, icy + ph + 1,
                icx - pw - 1, icy + ph * 0.35f, icx - pw * 0.7f - 1, icy - ph * 0.3f));
        in.setColor(Color.BLACK);
        in.fill(poly(icx, icy - ph, icx + pw, icy - ph * 0.3f, icx + pw * 0.7f, icy + ph * 0.3f, icx, icy + ph,
                icx - pw, icy + ph * 0.35f, icx - pw * 0.7f, icy - ph * 0.3f));
        // a shadow under the upper lid, and a glint
        in.setColor(new Color(0, 0, 0, 150));
        in.fill(poly(ix, iy - 1, ox, oy - eh * 1.6f, qx, qy - 1, qx, qy + 1, ox, oy - eh * 1.2f, ix, iy + 1));
        in.setColor(Color.WHITE);
        in.fillRect(Math.round(icx - ir * 0.5f), Math.round(icy - ir * 0.35f), 2, 1);
        in.fillRect(Math.round(icx - ir * 0.5f) + 3, Math.round(icy - ir * 0.35f) + 1, 1, 1);
        in.dispose();
        e.setColor(Color.BLACK);
        e.draw(lid);
        // the heavy brow ridge, slanting down over the inner corner
        e.setColor(rgb(0x050207));
        e.fill(poly(ix - ew * 0.25f, iy - eh * 0.4f, qx + ew * 0.2f, qy - eh * 1.9f, qx + ew * 0.3f, qy - eh * 1.0f,
                ox, oy - eh * 1.05f, ix - ew * 0.15f, iy + eh * 0.2f));
        e.setColor(rgb(0x3a0a12));
        e.draw(new Line2D.Float(ix - ew * 0.2f, iy - eh * 0.4f, qx + ew * 0.2f, qy - eh * 1.9f));
        // black ichor dripping from the inner corner
        int drip = (t / 3) % Math.max(1, Math.round(lh * 0.55f));
        int dxp = Math.round(ix + 1), dyp = Math.round(iy + 1);
        e.setColor(rgb(0x120006));
        e.fillRect(dxp, dyp, 1, drip);
        e.fillRect(dxp - 1, dyp + drip, 3, 2);
        e.setColor(rgb(0x5a0010));
        e.fillRect(dxp, dyp + drip, 1, 1);
        e.dispose();

        Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(img, Math.round(cx - ox * px), Math.round(cy - oy * px), lw * px, lh * px, null);
        if (hint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
    }

    private static void fillCircle(Graphics2D g, float cx, float cy, float r, int c) {
        g.setColor(rgb(c));
        g.fillOval(Math.round(cx - r), Math.round(cy - r), Math.max(1, Math.round(r * 2)), Math.max(1, Math.round(r * 2)));
    }

    // ================================================================= small helpers

    // a copy of an image with every visible pixel set to one flat colour
    private static BufferedImage flat(BufferedImage src, int rgb) {
        BufferedImage out = new BufferedImage(Math.max(1, src.getWidth()), Math.max(1, src.getHeight()), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++)
                if ((src.getRGB(x, y) >>> 24) > 40) out.setRGB(x, y, 0xFF000000 | rgb);
        return out;
    }

    // a copy darkened and pushed toward red (the knight as he dies)
    private static BufferedImage darkened(BufferedImage src) {
        BufferedImage out = new BufferedImage(Math.max(1, src.getWidth()), Math.max(1, src.getHeight()), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                if ((p >>> 24) <= 40) continue;
                int r = (p >> 16) & 255, gr = (p >> 8) & 255, b = p & 255;
                out.setRGB(x, y, 0xFF000000 | (Math.min(255, r * 6 / 10 + 30) << 16) | ((gr * 4 / 10) << 8) | (b * 4 / 10));
            }
        return out;
    }

    private static Shape poly(float... p) {
        Path2D.Float s = new Path2D.Float();
        s.moveTo(p[0], p[1]);
        for (int i = 2; i + 1 < p.length; i += 2) s.lineTo(p[i], p[i + 1]);
        s.closePath();
        return s;
    }

    private static float ease(float p) { p = Math.max(0, Math.min(1, p)); return 1 - (1 - p) * (1 - p) * (1 - p); }

    // blends two colours (k = 0: a, k = 1: b)
    private static int mix(int a, int b, float k) {
        k = Math.max(0, Math.min(1, k));
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int gr = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (gr << 8) | bl;
    }

    static Color rgb(int c) { return new Color((c >> 16) & 255, (c >> 8) & 255, c & 255); }

    static Color rgba(int c, int a) { return new Color((c >> 16) & 255, (c >> 8) & 255, c & 255, Math.max(0, Math.min(255, a))); }
}