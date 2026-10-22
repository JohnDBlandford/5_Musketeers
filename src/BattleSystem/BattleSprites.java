package BattleSystem;

import Engine.GraphicsHandler;
import Engine.ImageLoader;
import SpriteFont.MonoSpriteFont;

import java.awt.*;
import java.awt.image.BufferedImage;

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
            BufferedImage[] melee = {cell(attacks, 0, 0), cell(attacks, 1, 0), cell(attacks, 2, 0), cell(attacks, 0, 1)};
            BufferedImage[] ranged = {cell(attacks, 1, 1), cell(attacks, 2, 1), cell(attacks, 0, 2), cell(attacks, 1, 2)};
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

    private static BufferedImage cell(BufferedImage sheet, int col, int row) {
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
        java.util.List<String> lines = wrapName(f, name.toUpperCase(), w - 16, scale);
        if (lines.size() * 12 * scale > h - 12) { scale = 1; lines = wrapName(f, name.toUpperCase(), w - 16, scale); }
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

    private static java.util.List<String> wrapName(MonoSpriteFont f, String text, int maxW, int scale) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String cur = "";
        for (String word : text.split(" ")) {
            String t = cur.isEmpty() ? word : cur + " " + word;
            if (f.getTextWidth(t, scale, 0) <= maxW || cur.isEmpty()) cur = t;
            else { out.add(cur); cur = word; }
        }
        if (!cur.isEmpty()) out.add(cur);
        return out;
    }
}