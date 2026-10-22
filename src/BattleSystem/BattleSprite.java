package BattleSystem;

import Engine.GraphicsHandler;
import GameObject.ImageEffect;

import java.awt.*;
import java.awt.image.BufferedImage;

// One fighter on the battle field. Holds the picture(s) and plays the little animations that happen on interact:
// ATTACK lunges forward, HURT shakes and flickers, GUARD/HEAL flash a colored aura, RUN is a quick hop backwards (when you ask to run),
// FLEE slides off screen (when you really got away), FAINT sinks and fades.
public class BattleSprite {
    public enum Pose { IDLE, ATTACK, HURT, GUARD, HEAL, RUN, FLEE, FAINT }

    private final BufferedImage idle;
    private final BufferedImage[] attack;
    //private final BufferedImage[] rangedAttack;
    //private final BufferedImage[] swordAttack;
    private final BufferedImage[] hurt;
    private final boolean facesLeft;   // true for the enemy on the right (the picture is flipped to look at the player)
    private final int scale;

    // the Pokemon-style stat change animation: coloured arrows streaming up (stat rose) or down (stat fell) over the fighter
    private static final int FX_FRAMES = 62;
    private int fxT = -1;
    private boolean fxUp;
    private Color fxColor = Color.WHITE;

    private Pose pose = Pose.IDLE;
    private int t;

    private final boolean flipImage;  // false for text placeholders so the name never mirrors

    public BattleSprite(BufferedImage idle, BufferedImage[] attack, BufferedImage[] hurt, boolean facesLeft, int scale) {
        this(idle, attack, hurt, facesLeft, scale, facesLeft);
    }

    public BattleSprite(BufferedImage idle, BufferedImage[] attack, BufferedImage[] hurt, boolean facesLeft, int scale, boolean flipImage) {
        this.flipImage = flipImage;
        this.idle = idle;
        this.attack = (attack == null || attack.length == 0) ? new BufferedImage[]{idle} : attack;
        this.hurt = (hurt == null || hurt.length == 0) ? new BufferedImage[]{idle} : hurt;
        this.facesLeft = facesLeft;
        this.scale = scale;
    }

    public void setPose(Pose p) {
        if (p == null || p == pose && p == Pose.IDLE) return;
        pose = p;
        t = 0;
    }

    public Pose getPose() { return pose; }

    public boolean isBusy() { return pose != Pose.IDLE && pose != Pose.FAINT && pose != Pose.FLEE; }

    private int duration() {
        switch (pose) {
            case ATTACK: return 32;
            case HURT: return 30;
            case GUARD: return 40;
            case HEAL: return 50;
            case RUN: return 24;
            default: return Integer.MAX_VALUE;
        }
    }

    // starts the stat animation (up = true for a rise, false for a fall) in the colour of that stat
    public void playStatFx(boolean up, Color color) {
        fxT = 0;
        fxUp = up;
        fxColor = color;
    }

    public void update() {
        if (fxT >= 0 && ++fxT > FX_FRAMES) fxT = -1;
        t++;
        if (t > duration()) { pose = Pose.IDLE; t = 0; }
    }

    public int getDrawWidth() { return idle.getWidth() * scale; }
    public int getDrawHeight() { return idle.getHeight() * scale; }

    // cx = horizontal center, footY = the ground line the fighter stands on
    public void draw(GraphicsHandler gh, int cx, int footY, int tick) {
        Graphics2D g = gh.getGraphics();
        int w = getDrawWidth(), h = getDrawHeight();
        int dir = facesLeft ? -1 : 1;     // direction toward the opponent
        int dx = 0, dy = (int) Math.round(Math.sin(tick * 0.08) * 2);
        float alpha = 1f;
        BufferedImage img = idle;

        switch (pose) {
            case ATTACK: {
                float p = t / (float) duration();
                dx = (int) Math.round(dir * Math.sin(Math.PI * p) * 56);
                dy = (int) Math.round(-dir * Math.abs(dx) * 0.35);   // lunges diagonally, up toward the enemy / down toward the knight
                img = attack[Math.min(attack.length - 1, (int) (p * attack.length))];
                break;
            }
            case HURT: {
                dx = (t % 6 < 3 ? -7 : 7) * (t < 22 ? 1 : 0) * -dir;
                dy = 0;
                img = hurt[Math.min(hurt.length - 1, t / 10)];
                if (t % 6 < 2) alpha = 0.35f;
                break;
            }
            case RUN:
                dx = (int) Math.round(-dir * Math.sin(Math.PI * t / 24.0) * 26);
                dy = 0;
                break;
            case FLEE:
                dx = -dir * t * 7;
                alpha = Math.max(0f, 1f - t / 40f);
                break;
            case FAINT:
                dy = Math.min(h / 2, t * 2);
                alpha = Math.max(0f, 1f - t / 45f);
                break;
            default:
                break;
        }
        if (alpha <= 0f) return;

        Composite old = g.getComposite();
        Shape oldClip = g.getClip();
        if (pose == Pose.FAINT) g.clipRect(cx - w, footY - h - 10, w * 2, h + 10);   // sinks "into" the ground
        if (alpha < 1f) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        gh.drawImage(img, cx - w / 2 + dx, footY - h + dy, w, h, flipImage ? ImageEffect.FLIP_HORIZONTAL : ImageEffect.NONE);
        g.setComposite(old);

        if (pose == Pose.GUARD || pose == Pose.HEAL) {
            float p = t / (float) duration();
            float a = (float) Math.sin(Math.PI * p) * 0.45f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, a)));
            g.setColor(pose == Pose.GUARD ? BattleUI.BLUE : BattleUI.GREEN);
            int r = (int) (Math.max(w, h) * 0.62);
            g.fillOval(cx - r, footY - h / 2 - r, r * 2, r * 2);
            g.setComposite(old);
        }
        g.setClip(oldClip);
        if (fxT >= 0) drawStatFx(g, cx, footY, w, h);
    }

    // a tinted wash, a soft band sweeping through the sprite and columns of chevrons scrolling the same way
    private void drawStatFx(Graphics2D g, int cx, int footY, int w, int h) {
        float p = fxT / (float) FX_FRAMES;
        float env = (float) Math.sin(Math.PI * Math.min(1f, p));          // fades in and out
        int x0 = cx - w / 2, y0 = footY - h;
        Shape oldClip = g.getClip();
        Composite old = g.getComposite();
        Stroke oldStroke = g.getStroke();
        g.clipRect(x0 - 6, y0 - 6, w + 12, h + 12);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f * env));
        g.setColor(fxColor);
        g.fillRect(x0, y0, w, h);

        int bandH = Math.max(24, h / 3);
        int bandY = fxUp ? footY - (int) (p * (h + bandH)) : y0 - bandH + (int) (p * (h + bandH));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f * env));
        g.setPaint(new GradientPaint(0, bandY, new Color(255, 255, 255, 0), 0, bandY + bandH / 2, Color.WHITE));
        g.fillRect(x0, bandY, w, bandH / 2);
        g.setPaint(new GradientPaint(0, bandY + bandH / 2, Color.WHITE, 0, bandY + bandH, new Color(255, 255, 255, 0)));
        g.fillRect(x0, bandY + bandH / 2, w, bandH / 2);

        int cw = Math.max(10, w / 7), gapY = Math.max(34, h / 3);
        double scroll = (fxT * 3.4) % gapY;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, env * 1.4f)));
        for (int col = 0; col < 3; col++) {
            int ax = x0 + w * (2 * col + 1) / 6;
            for (int row = -1; row <= h / gapY + 1; row++) {
                double off = fxUp ? -scroll : scroll;
                int ay = (int) (fxUp ? footY - 10 + off - row * gapY : y0 + 10 + off + row * gapY) - (col == 1 ? gapY / 2 : 0);
                if (ay < y0 - 12 || ay > footY + 12) continue;
                int s = fxUp ? -1 : 1;     // chevron points up for a rise, down for a fall
                int[] xs = {ax - cw, ax, ax + cw}, ys = {ay - s * cw / 2, ay + s * cw / 2, ay - s * cw / 2};
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(Color.WHITE);
                g.drawPolyline(xs, ys, 3);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(fxColor.brighter());
                g.drawPolyline(xs, ys, 3);
            }
        }
        g.setStroke(oldStroke);
        g.setComposite(old);
        g.setClip(oldClip);
    }
}