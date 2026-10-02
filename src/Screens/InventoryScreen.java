package Screens;

import Engine.GraphicsHandler;
import Engine.KeyLocker;
import Engine.Screen;
import Game.ScreenCoordinator;
import Level.Map;
import SpriteFont.SpriteFont;

public class InventoryScreen extends Screen {
    // KeyLocker instance
    // ScreenCoordinator instance
    // Map instance - not a HashMap
    // keyPress timer
    // currentMenuItemHovered
    // menuItemSelected
    // SpriteFont for naming
    private KeyLocker keyLocker = new KeyLocker();
    private ScreenCoordinator screenCoordinator;
    private int currMenuItemHovered;
    private int currMenuItemSelected;
    private SpriteFont inventoryScreen;
    private Map background;
    private int keyPressTimer;

    public InventoryScreen(ScreenCoordinator screenCoordinator) { this.screenCoordinator = screenCoordinator; }

    @Override
    public void initialize() {

    }

    @Override
    public void update() {

    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        background.draw(graphicsHandler);
    }
}
