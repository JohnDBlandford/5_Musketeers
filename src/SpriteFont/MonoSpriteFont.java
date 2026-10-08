package SpriteFont;

import Engine.GraphicsHandler;
import Engine.ImageLoader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class MonoSpriteFont {
    public static final String DEFAULT_IMAGE = "VCROSDMono.png";
    public static final int CELL_W = 6, CELL_H = 10, COLUMNS = 16;
    public static final int CAP_TOP = 1, CAP_HEIGHT = 7;

    private static final String CHARS = buildChars();

    private final BufferedImage sheet;
    private final HashMap<Integer, BufferedImage[]> tintedGlyphs = new HashMap<>();

    public MonoSpriteFont() {
        this(DEFAULT_IMAGE);
    }

    public MonoSpriteFont(String imageFileName) {
        this.sheet = ImageLoader.load(imageFileName);
    }

    private static String buildChars() {
        StringBuilder sb = new StringBuilder();
        for (char c = 32; c <= 126; c++) sb.append(c);
        sb.append('\u00A9');
        return sb.toString();
    }

    public int getTextWidth(String text, int scale, int tracking) {
        if (text.isEmpty()) return 0;
        return text.length() * CELL_W * scale + tracking * (text.length() - 1) - scale;
    }

    public int getCapHeight(int scale) {
        return CAP_HEIGHT * scale;
    }

    public void draw(GraphicsHandler gh, String text, int x, int capTopY, int scale, int tracking, Color color) {
        Graphics2D g = gh.getGraphics();
        Composite old = g.getComposite();
        if (color.getAlpha() < 255) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, color.getAlpha() / 255f));
        }
        BufferedImage[] glyphs = getGlyphs(color);
        int cx = x, cellY = capTopY - CAP_TOP * scale;
        for (char ch : text.toCharArray()) {
            if (ch != ' ') {
                int i = CHARS.indexOf(ch);
                if (i < 0) i = CHARS.indexOf('?');
                gh.drawImage(glyphs[i], cx, cellY, CELL_W * scale, CELL_H * scale);
            }
            cx += CELL_W * scale + tracking;
        }
        g.setComposite(old);
    }

    private BufferedImage[] getGlyphs(Color color) {
        int key = color.getRGB() & 0xFFFFFF;
        BufferedImage[] glyphs = tintedGlyphs.get(key);
        if (glyphs == null) {
            BufferedImage tinted = new BufferedImage(sheet.getWidth(), sheet.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < sheet.getHeight(); y++) {
                for (int x = 0; x < sheet.getWidth(); x++) {
                    if ((sheet.getRGB(x, y) >>> 24) > 0) tinted.setRGB(x, y, 0xFF000000 | key);
                }
            }
            glyphs = new BufferedImage[CHARS.length()];
            for (int i = 0; i < glyphs.length; i++) {
                glyphs[i] = tinted.getSubimage((i % COLUMNS) * CELL_W, (i / COLUMNS) * CELL_H, CELL_W, CELL_H);
            }
            tintedGlyphs.put(key, glyphs);
        }
        return glyphs;
    }
}