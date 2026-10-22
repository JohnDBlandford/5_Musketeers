package BattleSystem;

import Engine.GraphicsHandler;
import Engine.ImageLoader;
import GameObject.ImageEffect;
import GameObject.ImageEffect;
import SpriteFont.MonoSpriteFont;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static BattleSystem.BattleSprites.BattleSprite.cell;
import static BattleSystem.BattleSprites.BattleSprite.placeholder;

// Builds the sprites for a fight.
//  - The knight is cut straight out of Spritesheet.png using the same cells Knight.java uses (3 columns x 4 rows of 32x32).
//  - Enemies look for  Resources/Battle/<EnemyName without spaces>.png  (e.g. Battle/StarvingWretch.png).
//    That file is where your partner drops the real art. Until it exists a colored placeholder monster is drawn instead.
//    A single picture of any size works (it is scaled up to about 128 px tall).
public final class BattleSprites {
    private BattleSprites() {}

    public static final int KNIGHT_HEIGHT = 192;   // the knight is the big sprite in front (bottom left)
    public static final int ENEMY_HEIGHT = 128;    // the enemy is further away (upper right), so it is drawn smaller

    public static BattleSprite knight() {
        try {
            BufferedImage sheet = ImageLoader.load("Spritesheet.png");
            BufferedImage attacks = ImageLoader.load("Attacks.png");
            BufferedImage[] attack = {cell(attacks, 0, 0), cell(attacks, 1, 0), cell(attacks, 2, 0), cell(attacks, 0, 1)};
            BufferedImage[] ranged = {cell(attacks, 1, 1), cell(attacks, 2, 1), cell(attacks, 0, 2), cell(attacks, 1, 2)};
            BufferedImage idle = cell(sheet, 0, 0);
            BufferedImage[] hurt = {cell(sheet, 1, 0), cell(sheet, 2, 0), cell(sheet, 0, 1)};
            //BufferedImage[] attack = {cell(sheet, 1, 1), cell(sheet, 2, 1), cell(sheet, 0, 2), cell(sheet, 1, 2)};
            return new BattleSprite(idle, attack, hurt, false, KNIGHT_HEIGHT / 32);
        } catch (RuntimeException e) {
            return placeholder("KNIGHT", false);
        }
    }
    public static final class BattleSprite {
        public enum Pose {IDLE, ATTACK, HURT, GUARD, HEAL, RUN, FLEE, FAINT}

        private final BufferedImage idle;
        private final BufferedImage[] attack;
        private final BufferedImage[] hurt;
        private final boolean facesLeft;   // true for the enemy on the right (the picture is flipped to look at the player)
        private final int scale;

        // the Pokemon-style stat change animation: coloured arrows streaming up (stat rose) or down (stat fell) over the fighter
        private static final int FX_FRAMES = 62;
        private int fxT = -1;
        private boolean fxUp;
        private Color fxColor = Color.WHITE;

        private BattleSystem.BattleSprites.BattleSprite.Pose pose = BattleSystem.BattleSprites.BattleSprite.Pose.IDLE;
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

        public void setPose(BattleSystem.BattleSprites.BattleSprite.Pose p) {
            if (p == null || p == pose && p == BattleSystem.BattleSprites.BattleSprite.Pose.IDLE) return;
            pose = p;
            t = 0;
        }

        public BattleSystem.BattleSprites.BattleSprite.Pose getPose() {
            return pose;
        }

        public boolean isBusy() {
            return pose != BattleSystem.BattleSprites.BattleSprite.Pose.IDLE && pose != BattleSystem.BattleSprites.BattleSprite.Pose.FAINT
                    && pose != BattleSystem.BattleSprites.BattleSprite.Pose.FLEE;
        }

        private int duration() {
            switch (pose) {
                case ATTACK:
                    return 32;
                case HURT:
                    return 30;
                case GUARD:
                    return 40;
                case HEAL:
                    return 50;
                case RUN:
                    return 24;
                default:
                    return Integer.MAX_VALUE;
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
            if (t > duration()) {
                pose = BattleSystem.BattleSprites.BattleSprite.Pose.IDLE;
                t = 0;
            }
        }

        public int getDrawWidth() {
            return idle.getWidth() * scale;
        }

        public int getDrawHeight() {
            return idle.getHeight() * scale;
        }

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
            if (pose == BattleSystem.BattleSprites.BattleSprite.Pose.FAINT)
                g.clipRect(cx - w, footY - h - 10, w * 2, h + 10);   // sinks "into" the ground
            if (alpha < 1f) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            gh.drawImage(img, cx - w / 2 + dx, footY - h + dy, w, h, flipImage ? ImageEffect.FLIP_HORIZONTAL : ImageEffect.NONE);
            g.setComposite(old);

            if (pose == BattleSystem.BattleSprites.BattleSprite.Pose.GUARD || pose == BattleSystem.BattleSprites.BattleSprite.Pose.HEAL) {
                float p = t / (float) duration();
                float a = (float) Math.sin(Math.PI * p) * 0.45f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, a)));
                g.setColor(pose == BattleSystem.BattleSprites.BattleSprite.Pose.GUARD ? BattleUI.BLUE : BattleUI.GREEN);
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


        public static BattleSprite knight() {
            try {
                BufferedImage sheet = ImageLoader.load("Spritesheet.png");
                BufferedImage idle = cell(sheet, 0, 0);
                BufferedImage[] hurt = {cell(sheet, 1, 0), cell(sheet, 2, 0), cell(sheet, 0, 1)};
                BufferedImage[] attack = {cell(sheet, 1, 1), cell(sheet, 2, 1), cell(sheet, 0, 2), cell(sheet, 1, 2)};
                return new BattleSprite(idle, attack, hurt, false, KNIGHT_HEIGHT / 32);
            } catch (RuntimeException e) {
                return placeholder("KNIGHT", false);
            }
        }

    public static BattleSprite enemy(String enemyName) {
        String file = "Battle/" + enemyName.replace(" ", "") + ".png";
        try {
            BufferedImage img = ImageLoader.load(file);
            BufferedImage[] attack = null;
            BufferedImage[] hurt = null;
            BufferedImage idleBase = null;
            int scale = Math.max(1, KNIGHT_HEIGHT / 32);
            if (enemyName.equals("HighChorusZealot")) {
                attack = new BufferedImage[] {cell(img, 0, 0), cell(img, 1, 0), cell(img, 2, 0), cell(img, 0, 1)};
                hurt = new BufferedImage[] {cell(img, 0, 0), cell(img, 1, 1), cell(img, 2, 1), cell(img, 0, 2), cell(img, 1, 2)};
                idleBase = cell(img, 0, 0);
                //idle = new BufferedImage[] {cell(img, 0, 0), cell(img, 2, 2), cell(img, 0, 3), cell(img, 1, 3), cell(img, 2, 3)};
            } else if (enemyName.equals("FamineWarden")) {
                attack = new BufferedImage[] {superCell(img, 0, 0), superCell(img, 1, 0), superCell(img, 2, 0), superCell(img, 3, 0), superCell(img, 0, 1), superCell(img, 1, 1), superCell(img, 2, 1), superCell(img, 3, 1)};
                hurt = new BufferedImage[] {superCell(img, 0, 0), superCell(img, 0, 2), superCell(img, 1, 2), superCell(img, 2, 2), superCell(img, 3, 2), superCell(img, 0, 3)};
                idleBase = superCell(img, 0, 0);
                scale = Math.max(1, KNIGHT_HEIGHT / 16);
                //idle = new BufferedImage[] {superCell(img, 0, 0), superCell(img, 1, 3), superCell(img, 2, 3), superCell(img, 3, 3), superCell(img, 0, 4), superCell(img, 1, 4)};
            } else {
                attack = new BufferedImage[] {cell(img, 0, 0), cell(img, 1, 0), cell(img, 2, 0), cell(img, 0, 1)};
                hurt = new BufferedImage[] {cell(img, 0, 0), cell(img, 1, 1), cell(img, 2, 1), cell(img, 0, 2)};
                idleBase = cell(img, 0, 0);
                //idle = new BufferedImage[] {cell(img, 0, 0), cell(img, 1, 2), cell(img, 2, 2), cell(img, 0, 3)};
            }

            return new BattleSprite(idleBase, attack, hurt, true, scale);
        } catch (RuntimeException e) {
            return placeholder(enemyName, true);
        }
    }
    static BufferedImage cell(BufferedImage sheet, int col, int row) {
        return sheet.getSubimage(col * 32, row * 32, 32, 32);
    }
     private static BufferedImage superCell(BufferedImage sheet, int col, int row) {
        return sheet.getSubimage(col * 48, row * 48, 48, 48);
    }

        // PLACEHOLDER ART: a black box with a white border and the name inside it. It acts as the "alt text" of the sprite.
        public static BattleSprite placeholder(String name, boolean enemy) {
            int w = enemy ? 160 : 120, h = enemy ? 112 : 160;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.WHITE);
            g2.drawRect(0, 0, w - 1, h - 1);
            GraphicsHandler gh = new GraphicsHandler();
            gh.setGraphics(g2);
            MonoSpriteFont f = BattleUI.font();
            int scale = 2;
            List<String> lines = wrapName(f, name.toUpperCase(), w - 16, scale);
            if (lines.size() * 12 * scale > h - 12) {
                scale = 1;
                lines = wrapName(f, name.toUpperCase(), w - 16, scale);
            }
            int lineH = 6 * scale + 7 * scale;
            int total = lines.size() * lineH - 6 * scale;
            int y = (h - total) / 2;
            for (String line : lines) {
                f.draw(gh, line, (w - f.getTextWidth(line, scale, 0)) / 2, y, scale, 0, Color.WHITE);
                y += lineH;
            }
            g2.dispose();
            return new BattleSprite(img, null, null, enemy, 1, false);
        }

        private static List<String> wrapName(MonoSpriteFont f, String text, int maxW, int scale) {
            List<String> out = new ArrayList<>();
            String cur = "";
            for (String word : text.split(" ")) {
                String t = cur.isEmpty() ? word : cur + " " + word;
                if (f.getTextWidth(t, scale, 0) <= maxW || cur.isEmpty()) cur = t;
                else {
                    out.add(cur);
                    cur = word;
                }
            }
            if (!cur.isEmpty()) out.add(cur);
            return out;
        }
    }
}