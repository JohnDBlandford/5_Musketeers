package Lighting;

import Engine.GraphicsHandler;
import Utils.Colors;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;


public class BackgroundOpacity {
    public void draw(GraphicsHandler graphicsHandler,float x, float y) {

        Rectangle2D box = new Rectangle2D.Float(x-1000,y-500,2000,2000);
        Ellipse2D ellipse = new Ellipse2D.Float(x-15,y-15,100,100);
        Area areaBox = new Area(box);
        Area areaEllipse = new Area(ellipse);

        areaBox.subtract(areaEllipse);
        graphicsHandler.getGraphics().setColor(Colors.transparentBlack);
        graphicsHandler.getGraphics().fill(areaBox);

        //TESTING
        // graphicsHandler.getGraphics().fill(box);
        //graphicsHandler.getGraphics().fill(ellipse);
        //graphicsHandler.drawFilledRectangle(x, y, 100, 100, Colors.transparentBlack);
        // graphicsHandler.drawFilledRectangle(x-500,y-100,1000,500,Colors.MAGENTA);
        //graphicsHandler.drawCircle(x, y, 50, Colors.transparentBlack);



    }
}
