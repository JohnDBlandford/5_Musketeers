package GameObject;

import Engine.GraphicsHandler;

// A door that moves the player to another map. Invisible trigger; the tile under it shows through.
public class Door extends Portal {
    public Door(float x, float y, String targetMap, float targetX, float targetY, int width, int height) {
        super(x, y, targetMap, targetX, targetY, width, height);
    }

    @Override
    public void drawBottomLayer(GraphicsHandler graphicsHandler) {
        // draw nothing
    }
}