package GameObject;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class PlaceHolderSpriteFactory {
    private PlaceHolderSpriteFactory() {}

    public static SpriteSheet drawSolidColor(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0,0, width, height);
        graphics.dispose();
        return new SpriteSheet(img, width, height);
    }
}
