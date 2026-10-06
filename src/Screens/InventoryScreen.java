package Screens;

import Engine.*;
import SpriteFont.SpriteFont;

import java.awt.*;

public class InventoryScreen extends Screen {
    private KeyLocker keyLocker = new KeyLocker();
    private PlayLevelScreen playLevelScreen;
    private SpriteFont inventory;
    private SpriteFont instructions;

    public InventoryScreen(PlayLevelScreen playLevelScreen) {
        this.playLevelScreen = playLevelScreen;
        initialize();
    }

    @Override
    public void initialize() {
        inventory = new SpriteFont("Inventory", 200, 200, "Arial", 28, Color.white);
        inventory.setOutlineColor(Color.black);
        inventory.setOutlineThickness(2);
        instructions = new SpriteFont("Press I to enter and ESC exit the inventory", 150, 100, "Arial", 25, Color.white);
        instructions.setOutlineColor(Color.black);
        instructions.setOutlineThickness(2);
        keyLocker.lockKey(Key.ESC);
    }

    @Override
    public void update() {
        if (Keyboard.isKeyUp(Key.ESC)) keyLocker.unlockKey(Key.ESC);
        if (Keyboard.isKeyDown(Key.ESC) && !keyLocker.isKeyLocked(Key.ESC)) {
            playLevelScreen.closeInventory();
        }
    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        int screenWidth = ScreenManager.getScreenWidth();
        int screenHeight = ScreenManager.getScreenHeight();
        // dims the world/level background
        graphicsHandler.drawFilledRectangle(0, 0, screenWidth, screenHeight, new Color(0, 0, 0, 128));
        // draws the actual inventory panel
        graphicsHandler.drawFilledRectangleWithBorder(screenWidth / 8, screenHeight / 8, screenWidth * 3 / 4,
                screenHeight * 3 / 4, new Color(60,60,60), Color.black, 3);
        inventory.draw(graphicsHandler);
        instructions.draw(graphicsHandler);
    }
}
