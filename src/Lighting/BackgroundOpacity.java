package Lighting;

import Engine.GraphicsHandler;
import Utils.Colors;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class BackgroundOpacity {
    public void draw(GraphicsHandler graphicsHandler, float centerX, float centerY) {
        float radius = 50;

        Rectangle2D box = new Rectangle2D.Float(centerX - 1000, centerY - 1000, 2000, 2000);

        Ellipse2D ellipse = new Ellipse2D.Float(centerX - radius, centerY - radius, radius * 2, radius * 2);

        Area areaBox = new Area(box);
        areaBox.subtract(new Area(ellipse));

        graphicsHandler.getGraphics().setColor(Colors.transparentBlack);
        graphicsHandler.getGraphics().fill(areaBox);
    }
}
