package Screens;

import Engine.*;
import SpriteFont.MonoSpriteFont;

import java.awt.*;
import java.util.Random;

// Shown when the knight is defeated. The world drains to dark red, embers drift upward, "GAME OVER" fades in with a slow
// pulse, and a line says where the knight will wake up. Press ENTER (once the text has appeared) -> isDone() becomes true
// and PlayLevelScreen puts the knight back at the last named location.
public final class GameOverScreen extends Screen {
    private static final int FADE_IN = 60, TITLE_AT = 50, SUB_AT = 130, PROMPT_AT = 190;
    private final MonoSpriteFont font = new MonoSpriteFont();
    private final KeyLocker keyLocker = new KeyLocker();
    private final String placeName;
    private int t, leaving;
    private boolean done;
    private double[] ex, ey, es, ev;
    private final Random rng = new Random(42);

    public GameOverScreen(String placeName) { this.placeName = placeName; }

    @Override
    public void initialize() {
        t = 0; leaving = 0; done = false;
        int n = 70;
        ex = new double[n]; ey = new double[n]; es = new double[n]; ev = new double[n];
        for (int i = 0; i < n; i++) { ex[i] = rng.nextDouble() * 800; ey[i] = rng.nextDouble() * 605; es[i] = 1 + rng.nextInt(3); ev[i] = 0.3 + rng.nextDouble() * 1.1; }
        keyLocker.lockKey(Key.ENTER);
        keyLocker.lockKey(Key.SPACE);
    }

    public boolean isDone() { return done; }

    @Override
    public void update() {
        t++;
        for (int i = 0; i < ex.length; i++) {
            ey[i] -= ev[i];
            ex[i] += Math.sin((t + i * 13) * 0.03) * 0.4;
            if (ey[i] < -4) { ey[i] = 610; ex[i] = rng.nextDouble() * 800; }
        }
        boolean down = Keyboard.isKeyDown(Key.ENTER) || Keyboard.isKeyDown(Key.SPACE);
        if (!down) { keyLocker.unlockKey(Key.ENTER); keyLocker.unlockKey(Key.SPACE); }
        if (leaving > 0) { if (++leaving > 30) done = true; return; }
        if (t > PROMPT_AT - 20 && down && !keyLocker.isKeyLocked(Key.ENTER)) leaving = 1;
    }

    @Override
    public void draw(GraphicsHandler gh) {
        Graphics2D g = gh.getGraphics();
        double f = Math.min(1, t / (double) FADE_IN);
        g.setColor(new Color(10, 2, 4));
        g.fillRect(0, 0, 800, 605);
        // a slow red glow rising from the bottom
        for (int i = 0; i < 40; i++) {
            float a = (float) (f * 0.035 * (1 + 0.2 * Math.sin(t * 0.04)));
            g.setColor(new Color(0.65f, 0.05f, 0.05f, Math.min(1f, a)));
            int h = 40 + i * 12;
            g.fillRect(0, 605 - h, 800, h);
        }
        // embers
        for (int i = 0; i < ex.length; i++) {
            int al = (int) (f * (90 + 100 * Math.abs(Math.sin((t + i * 7) * 0.05))));
            g.setColor(new Color(255, 120 + (i * 9) % 90, 40, Math.min(255, al)));
            g.fillRect((int) ex[i], (int) ey[i], (int) es[i], (int) es[i]);
        }
        // vignette
        for (int i = 0; i < 14; i++) {
            g.setColor(new Color(0, 0, 0, 14));
            g.fillRect(0, 0, 800, 24 + i * 14);
            g.fillRect(0, 605 - 24 - i * 14, 800, 24 + i * 14);
        }
        // title
        if (t > TITLE_AT) {
            double p = Math.min(1, (t - TITLE_AT) / 40.0);
            int scale = 9, track = 3;
            String title = "GAME OVER";
            int w = font.getTextWidth(title, scale, track);
            int y = 210 + (int) ((1 - p) * -18);
            font.draw(gh, title, 400 - w / 2 + 4, y + 4, scale, track, new Color(0, 0, 0, (int) (200 * p)));
            double pulse = 0.85 + 0.15 * Math.sin(t * 0.06);
            font.draw(gh, title, 400 - w / 2, y, scale, track, new Color((int) (225 * pulse), (int) (40 * pulse), (int) (40 * pulse), (int) (255 * p)));
        }
        if (t > SUB_AT) {
            double p = Math.min(1, (t - SUB_AT) / 30.0);
            String s1 = "The knight falls...";
            String s2 = "...and wakes at " + placeName;
            int scale = 2, track = 1;
            font.draw(gh, s1, 400 - font.getTextWidth(s1, scale, track) / 2, 330, scale, track, new Color(200, 170, 170, (int) (255 * p)));
            font.draw(gh, s2, 400 - font.getTextWidth(s2, scale, track) / 2, 360, scale, track, new Color(255, 215, 150, (int) (255 * p)));
        }
        if (t > PROMPT_AT && (t / 30) % 2 == 0) {
            String s = "Press ENTER";
            font.draw(gh, s, 400 - font.getTextWidth(s, 2, 1) / 2, 470, 2, 1, new Color(190, 190, 190));
        }
        // fade out
        if (leaving > 0) {
            g.setColor(new Color(0, 0, 0, Math.min(255, leaving * 9)));
            g.fillRect(0, 0, 800, 605);
        }
        // fade in from the battle
        if (t < 20) {
            g.setColor(new Color(0, 0, 0, 255 - t * 12));
            g.fillRect(0, 0, 800, 605);
        }
    }
}