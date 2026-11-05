package Story;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

// STORY: every World 1 story character and object, drawn in code as small pixel sprites (16x24 for people, scaled x3 in
// the world like the other NPCs). No image files: each Look is a set of colours, a body shape and a prop, painted with
// rectangles, then given a dark 1-pixel outline. Frame 2 is the "breathing" frame (the upper body dips by 1 pixel).
// The dialogue box uses the same pictures, enlarged, as portraits.
public final class NPCArt {
    private NPCArt() {}

    public enum Look {
        KNIGHT, WREN, HESTER, IDA, BESS, JOSS, VALE, ANSEL, MIREL, MAUD, ELSA, HALE, WARDEN,
        GRAVE, GRAVE_BREAD, WALL, MARK, MOUND, CACHE, STONE
    }

    private static final Map<Look, BufferedImage[]> CACHE_IMG = new EnumMap<>(Look.class);

    private static final Color OUTLINE = new Color(22, 16, 20);
    private static final Color SKIN = new Color(214, 170, 132), SKIN_PALE = new Color(206, 190, 176), SKIN_DARK = new Color(160, 112, 80);

    // both frames of a look (index 0 = still, 1 = breathing)
    public static BufferedImage[] frames(Look look) {
        return CACHE_IMG.computeIfAbsent(look, NPCArt::paint);
    }

    public static BufferedImage portrait(Look look) { return frames(look)[0]; }

    // ---------------------------------------------------------------- the people

    // a person's recipe
    private static final class P {
        Color skin = SKIN, hair = new Color(70, 46, 30), cloth = new Color(110, 90, 70), cloth2 = new Color(80, 64, 50);
        Color hood, belt, eye = new Color(30, 22, 20);
        int hairStyle = 1;      // 0 none, 1 short, 2 long, 3 bun, 4 white long, 5 kerchief
        boolean robe, child, helmet, mask, tall, patches, bandage;
        int prop;               // 0 none, 1 staff, 2 shovel, 3 basket, 4 spear, 5 bread, 6 book, 7 scythe+basket, 8 sword
        Color propColor = new Color(120, 86, 50);
    }

    private static P recipe(Look l) {
        P p = new P();
        switch (l) {
            case KNIGHT: p.cloth = new Color(150, 156, 170); p.cloth2 = new Color(96, 102, 120); p.helmet = true; p.prop = 8;
                p.belt = new Color(120, 30, 30); break;
            case WREN: p.child = true; p.hair = new Color(120, 70, 36); p.hairStyle = 2; p.cloth = new Color(140, 112, 84);
                p.cloth2 = new Color(110, 84, 62); p.patches = true; p.prop = 5; p.robe = true; break;
            case HESTER: p.skin = SKIN_PALE; p.hair = new Color(150, 146, 140); p.hairStyle = 5; p.cloth = new Color(70, 70, 76);
                p.cloth2 = new Color(50, 50, 56); p.robe = true; p.hood = new Color(96, 96, 104); break;
            case IDA: p.hairStyle = 5; p.hair = new Color(150, 120, 80); p.cloth = new Color(120, 80, 60); p.cloth2 = new Color(92, 60, 44);
                p.robe = true; break;
            case BESS: p.hairStyle = 3; p.hair = new Color(60, 40, 28); p.cloth = new Color(96, 110, 80); p.cloth2 = new Color(70, 84, 58);
                p.robe = true; break;
            case JOSS: p.skin = SKIN_DARK; p.hair = new Color(40, 30, 26); p.cloth = new Color(100, 86, 70); p.cloth2 = new Color(74, 62, 50);
                p.patches = true; break;
            case VALE: p.cloth = new Color(200, 196, 182); p.cloth2 = new Color(160, 40, 40); p.helmet = true; p.prop = 4;
                p.belt = new Color(90, 70, 40); p.propColor = new Color(170, 170, 180); break;
            case ANSEL: p.skin = SKIN_PALE; p.robe = true; p.hood = new Color(30, 26, 30); p.cloth = new Color(34, 30, 36);
                p.cloth2 = new Color(150, 24, 30); p.hairStyle = 0; p.prop = 6; p.propColor = new Color(110, 30, 30); break;
            case MIREL: p.skin = SKIN_PALE; p.robe = true; p.hood = new Color(90, 86, 92); p.cloth = new Color(100, 96, 104);
                p.cloth2 = new Color(150, 24, 30); p.hairStyle = 0; p.prop = 3; break;
            case MAUD: p.skin = SKIN_PALE; p.hair = new Color(226, 224, 216); p.hairStyle = 4; p.robe = true;
                p.cloth = new Color(60, 94, 60); p.cloth2 = new Color(44, 70, 44); p.hood = new Color(70, 106, 66); p.prop = 1; break;
            case ELSA: p.hairStyle = 2; p.hair = new Color(176, 120, 60); p.cloth = new Color(96, 76, 52); p.cloth2 = new Color(66, 90, 60);
                p.robe = true; p.prop = 2; p.propColor = new Color(140, 140, 150); break;
            case HALE: p.skin = SKIN_DARK; p.hair = new Color(30, 26, 24); p.cloth = new Color(92, 70, 50); p.cloth2 = new Color(60, 46, 36);
                p.bandage = true; p.belt = new Color(50, 40, 30); break;
            case WARDEN: p.tall = true; p.robe = true; p.hood = new Color(46, 34, 26); p.cloth = new Color(58, 44, 32);
                p.cloth2 = new Color(110, 90, 50); p.mask = true; p.hairStyle = 0; p.prop = 7; p.skin = new Color(220, 210, 190);
                p.eye = new Color(200, 40, 30); break;
            default: break;
        }
        return p;
    }

    private static BufferedImage[] paint(Look l) {
        switch (l) {
            case GRAVE: return still(grave(false));
            case GRAVE_BREAD: return still(grave(true));
            case WALL: return still(wall());
            case MARK: return new BufferedImage[]{ mark(0), mark(1) };
            case MOUND: return new BufferedImage[]{ mound(false), mound(true) };
            case CACHE: return new BufferedImage[]{ cache(false), cache(true) };
            case STONE: return new BufferedImage[]{ stone(false), stone(true) };
            default: break;
        }
        P p = recipe(l);
        int w = p.tall ? 20 : 16, h = p.tall ? 30 : 24;
        BufferedImage a = person(p, w, h, false), b = person(p, w, h, true);
        return new BufferedImage[]{ outline(a), outline(b) };
    }

    private static BufferedImage[] still(BufferedImage img) { return new BufferedImage[]{ img, img }; }

    // paints one person. breathe = the upper body sits 1 pixel lower
    private static BufferedImage person(P p, int w, int h, boolean breathe) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int cx = w / 2;                               // centre column
        int top = p.child ? 7 : (p.tall ? 1 : 2);     // where the head starts
        int headW = p.child ? 5 : 6, headH = p.child ? 5 : 6;
        int bodyTop = top + headH, bodyBot = h - 3;
        int bodyW = p.child ? 7 : (p.tall ? 12 : 9);
        int b = breathe ? 1 : 0;

        // legs / robe hem
        if (p.robe) {
            rect(g, cx - bodyW / 2 - 1, bodyTop + 4, bodyW + 2, bodyBot - bodyTop - 3, p.cloth2);
            rect(g, cx - bodyW / 2, bodyTop + 4, bodyW, bodyBot - bodyTop - 4, p.cloth);
            rect(g, cx - bodyW / 2 - 1, bodyBot, bodyW + 2, 1, p.cloth2);
        } else {
            rect(g, cx - 3, bodyTop + 6, 2, bodyBot - bodyTop - 5, p.cloth2);
            rect(g, cx + 1, bodyTop + 6, 2, bodyBot - bodyTop - 5, p.cloth2);
        }
        rect(g, cx - 3, bodyBot + 1, 3, 2, new Color(40, 30, 24));     // shoes
        rect(g, cx + 1, bodyBot + 1, 3, 2, new Color(40, 30, 24));

        // torso
        int ty = bodyTop + b;
        rect(g, cx - bodyW / 2, ty, bodyW, 7, p.cloth);
        rect(g, cx - bodyW / 2, ty, 1, 7, brighter(p.cloth));          // light from the left
        rect(g, cx + bodyW / 2 - 1, ty, 1, 7, p.cloth2);                // shadow on the right
        if (p.belt != null) rect(g, cx - bodyW / 2, ty + 5, bodyW, 1, p.belt);
        if (p == null) return img;
        // the accent: Concord's red cross, the Chorus's red hollow eye, a Kept leaf
        if (p.cloth2.getRed() > 140 && p.cloth2.getGreen() < 60) {
            if (p.helmet) { rect(g, cx - 1, ty + 1, 2, 4, p.cloth2); rect(g, cx - 2, ty + 2, 4, 1, p.cloth2); }
            else { rect(g, cx - 1, ty + 2, 3, 2, p.cloth2); px(g, cx, ty + 2, OUTLINE); }
        }
        if (p.patches) { px(g, cx - 2, ty + 3, p.cloth2); px(g, cx + 1, ty + 5, brighter(p.cloth)); }
        if (p.bandage) { rect(g, cx - bodyW / 2, ty + 2, bodyW, 1, new Color(220, 210, 200)); px(g, cx + 1, ty + 2, new Color(160, 30, 30)); }
        // arms
        rect(g, cx - bodyW / 2 - 1, ty + 1, 1, 5, p.cloth2);
        rect(g, cx + bodyW / 2, ty + 1, 1, 5, p.cloth2);
        px(g, cx - bodyW / 2 - 1, ty + 6, p.skin);
        px(g, cx + bodyW / 2, ty + 6, p.skin);

        // head
        int hx = cx - headW / 2, hy = top + b;
        if (p.hood != null) {
            rect(g, hx - 1, hy - 1, headW + 2, headH + 2, p.hood);
            rect(g, hx - 2, hy + 3, headW + 4, headH, p.hood);
        }
        rect(g, hx, hy, headW, headH, p.mask ? p.skin : p.skin);
        if (p.mask) { rect(g, hx, hy, headW, headH, p.skin); rect(g, hx + 1, hy + headH - 2, headW - 2, 1, new Color(160, 150, 130)); }
        // hair
        switch (p.hairStyle) {
            case 1: rect(g, hx, hy, headW, 2, p.hair); px(g, hx, hy + 2, p.hair); break;
            case 2: rect(g, hx, hy, headW, 2, p.hair); rect(g, hx - 1, hy + 1, 1, headH + 2, p.hair);
                rect(g, hx + headW, hy + 1, 1, headH + 2, p.hair); break;
            case 3: rect(g, hx, hy, headW, 2, p.hair); rect(g, hx + 1, hy - 2, 3, 2, p.hair); break;
            case 4: rect(g, hx, hy, headW, 1, p.hair); rect(g, hx - 1, hy + 1, 1, headH + 4, p.hair);
                rect(g, hx + headW, hy + 1, 1, headH + 4, p.hair); break;
            case 5: rect(g, hx - 1, hy - 1, headW + 2, 3, p.cloth2); rect(g, hx - 1, hy + 2, 1, 3, p.cloth2); break;
            default: break;
        }
        if (p.helmet) {
            rect(g, hx - 1, hy - 1, headW + 2, 3, new Color(150, 154, 168));
            rect(g, hx - 1, hy - 1, headW + 2, 1, new Color(200, 204, 214));
            rect(g, hx + headW / 2 - 1, hy + 2, 2, 2, new Color(120, 124, 136));   // nose guard
        }
        // eyes
        int ey = hy + (p.hood != null ? 3 : 3);
        px(g, hx + 1, ey, p.eye);
        px(g, hx + headW - 2, ey, p.eye);
        if (p.mask) { px(g, hx + 1, ey, p.eye); px(g, hx + headW - 2, ey, p.eye); }

        // props
        Color pc = p.propColor;
        int px0 = cx + bodyW / 2 + 1;
        switch (p.prop) {
            case 1: rect(g, px0, hy - 1, 1, h - hy - 2, pc); px(g, px0, hy - 2, new Color(140, 200, 120)); break;             // staff
            case 2: rect(g, px0, ty - 3, 1, 14, new Color(120, 86, 50)); rect(g, px0 - 1, ty + 10, 3, 3, pc); break;         // shovel
            case 3: rect(g, px0 - 1, ty + 4, 4, 3, new Color(150, 110, 60)); rect(g, px0 - 1, ty + 3, 4, 1, new Color(200, 170, 100)); break;
            case 4: rect(g, px0, hy - 4, 1, h - hy + 1, new Color(120, 86, 50)); rect(g, px0 - 1, hy - 6, 3, 2, pc); px(g, px0, hy - 7, pc); break;
            case 5: rect(g, cx - 2, ty + 3, 4, 2, new Color(196, 150, 90)); px(g, cx - 2, ty + 3, new Color(230, 190, 130)); break;
            case 6: rect(g, cx - 2, ty + 3, 4, 3, pc); rect(g, cx - 2, ty + 3, 4, 1, new Color(200, 180, 140)); break;
            case 7: // the Warden: a scythe on the right, a tithe basket on the left
                rect(g, px0, hy - 1, 1, h - hy - 2, new Color(90, 70, 46));
                rect(g, px0 - 5, hy - 2, 6, 1, new Color(170, 170, 176)); px(g, px0 - 6, hy - 1, new Color(170, 170, 176));
                rect(g, cx - bodyW / 2 - 4, ty + 5, 5, 4, new Color(140, 104, 56)); rect(g, cx - bodyW / 2 - 4, ty + 4, 5, 1, new Color(196, 160, 90));
                break;
            case 8: rect(g, px0, ty + 2, 1, 9, new Color(200, 204, 214)); rect(g, px0 - 1, ty + 2, 3, 1, new Color(160, 120, 40)); break;
            default: break;
        }
        g.dispose();
        return img;
    }

    // ---------------------------------------------------------------- the objects

    // Wren's grave: a mound with a stick cross (and, if the knight gave Hester the whole ration, the bread Wren saved)
    private static BufferedImage grave(boolean bread) {
        BufferedImage img = new BufferedImage(16, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        rect(g, 2, 14, 12, 5, new Color(92, 70, 50));
        rect(g, 3, 13, 10, 1, new Color(110, 86, 60));
        rect(g, 7, 2, 2, 12, new Color(120, 90, 56));
        rect(g, 4, 5, 8, 2, new Color(120, 90, 56));
        px(g, 7, 2, new Color(150, 116, 74));
        if (bread) { rect(g, 9, 15, 4, 2, new Color(196, 150, 90)); px(g, 9, 15, new Color(230, 190, 130)); }
        rect(g, 4, 16, 1, 1, new Color(120, 120, 70));   // a weed
        g.dispose();
        return outline(img);
    }

    // the Wall of Names: a stretch of stone wall covered in scratched names
    private static BufferedImage wall() {
        BufferedImage img = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        for (int row = 0; row < 5; row++)
            for (int col = 0; col < 5; col++) {
                int off = row % 2 == 0 ? 0 : 3, x = col * 7 - off, y = 2 + row * 4;
                Color c = new Color(120 - (row * 7 + col * 13) % 20, 116 - (row * 7 + col * 13) % 20, 112 - (row * 7 + col * 13) % 20);
                rect(g, Math.max(0, x), y, Math.min(6, 32 - Math.max(0, x)), 3, c);
            }
        // scratches (the names)
        Color s = new Color(60, 54, 52);
        for (int i = 0; i < 9; i++) rect(g, 2 + (i * 11) % 26, 4 + (i * 5) % 16, 3 + i % 3, 1, s);
        rect(g, 0, 22, 32, 2, new Color(70, 62, 56));
        g.dispose();
        return outline(img);
    }

    // Dabbah's mark in the riverbed: cracked white ground torn open by three claw gouges that glow
    private static BufferedImage mark(int frame) {
        BufferedImage img = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        rect(g, 1, 3, 30, 12, new Color(214, 206, 186));
        Color crack = new Color(120, 110, 96);
        for (int i = 0; i < 6; i++) { rect(g, 2 + i * 5, 5 + (i % 3) * 3, 4, 1, crack); px(g, 4 + i * 5, 6 + (i % 3) * 3, crack); }
        Color glow = frame == 0 ? new Color(150, 20, 20) : new Color(220, 50, 30);
        for (int k = 0; k < 3; k++) {
            int x = 8 + k * 6;
            for (int y = 2; y < 15; y++) px(g, x + (y - 2) / 3, y, y % 4 == 0 ? glow : new Color(40, 16, 16));
        }
        g.dispose();
        return outline(img);
    }

    // the oldest burial mound, with a stone door; a glint where the tablet piece is
    private static BufferedImage mound(boolean glint) {
        BufferedImage img = new BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        rect(g, 1, 8, 22, 7, new Color(70, 90, 56));
        rect(g, 4, 4, 16, 4, new Color(80, 102, 62));
        rect(g, 8, 2, 8, 2, new Color(90, 112, 70));
        rect(g, 9, 8, 6, 7, new Color(96, 92, 88));
        rect(g, 10, 9, 4, 6, new Color(30, 26, 28));
        if (glint) { px(g, 12, 12, new Color(255, 230, 140)); px(g, 11, 12, new Color(200, 170, 90)); }
        g.dispose();
        return outline(img);
    }

    // a small stone cache with a carved eye: holds the Kept Grove piece
    private static BufferedImage cache(boolean glint) {
        BufferedImage img = new BufferedImage(16, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        rect(g, 1, 3, 14, 8, new Color(120, 114, 104));
        rect(g, 0, 2, 16, 2, new Color(146, 140, 128));
        rect(g, 6, 6, 4, 2, new Color(60, 54, 50));
        px(g, 7, 6, glint ? new Color(140, 220, 120) : new Color(80, 130, 70));
        g.dispose();
        return outline(img);
    }

    // a standing stone with a sigil (where the tablet pieces lie in the open)
    private static BufferedImage stone(boolean glint) {
        BufferedImage img = new BufferedImage(12, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        rect(g, 2, 2, 8, 17, new Color(110, 106, 100));
        rect(g, 3, 1, 6, 1, new Color(130, 126, 118));
        rect(g, 5, 6, 2, 6, glint ? new Color(220, 180, 90) : new Color(70, 64, 60));
        g.dispose();
        return outline(img);
    }

    // ---------------------------------------------------------------- pixel helpers

    private static void rect(Graphics2D g, int x, int y, int w, int h, Color c) {
        if (w <= 0 || h <= 0) return;
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    private static void px(Graphics2D g, int x, int y, Color c) { rect(g, x, y, 1, 1, c); }

    private static Color brighter(Color c) {
        return new Color(Math.min(255, c.getRed() + 30), Math.min(255, c.getGreen() + 30), Math.min(255, c.getBlue() + 30));
    }

    // a dark 1-pixel outline around everything drawn
    private static BufferedImage outline(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int p = src.getRGB(x, y);
                if ((p >>> 24) != 0) { out.setRGB(x, y, p); continue; }
                boolean edge = false;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h && (src.getRGB(nx, ny) >>> 24) != 0) edge = true;
                }
                if (edge) out.setRGB(x, y, OUTLINE.getRGB());
            }
        return out;
    }
}