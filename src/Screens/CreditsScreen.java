package Screens;

import Engine.*;
import Game.GameState;
import Game.ScreenCoordinator;
import SpriteFont.MonoSpriteFont;

import java.awt.*;

// Credits screen styled to match the dread-inducing theming of the MenuScreen
public class CreditsScreen extends Screen {
    protected ScreenCoordinator screenCoordinator;
    protected KeyLocker keyLocker = new KeyLocker();

    private MonoSpriteFont font;
    private int frame = 0;
    private int layoutW = -1;
    private int titleScale, textScale;

    private static final Color RED = new Color(255, 0, 0);
    private static final Color DARK_RED = new Color(110, 0, 0);
    private static final Color SCANLINE = new Color(0, 0, 0, 70);

    public CreditsScreen(ScreenCoordinator screenCoordinator) {
        this.screenCoordinator = screenCoordinator;
    }

    @Override
    public void initialize() {
        font = new MonoSpriteFont();
        frame = 0;
        layoutW = -1;
        keyLocker.lockKey(Key.SPACE);
    }

    public void update() {
        frame++;

        if (Keyboard.isKeyUp(Key.SPACE)) {
            keyLocker.unlockKey(Key.SPACE);
        }

        // if space is pressed, go back to main menu
        if (!keyLocker.isKeyLocked(Key.SPACE) && Keyboard.isKeyDown(Key.SPACE)) {
            screenCoordinator.setGameState(GameState.MENU);
        }
    }

    public void draw(GraphicsHandler graphicsHandler) {
        int w = ScreenManager.getScreenWidth();
        int h = ScreenManager.getScreenHeight();
        setupLayout(w, h);

        // 1. Pitch black background
        graphicsHandler.drawFilledRectangle(0, 0, w, h, Color.BLACK);

        // 2. Title Text (CREDITS) with the same red/dark-red guttering shadow effect
        String title = "CREDITS";
        int titleY = (int) (h * 0.15);
        int tx = (w - font.getTextWidth(title, titleScale, titleScale)) / 2;
        int sh = Math.max(2, titleScale / 3);

        font.draw(graphicsHandler, title, tx + sh, titleY + sh, titleScale, titleScale, DARK_RED);
        font.draw(graphicsHandler, title, tx, titleY, titleScale, titleScale, RED);

        // 3. Body Text (Credits Information)
        int tracking = textScale * 2;

        String creatorHeader = "ORIGINAL FRAMEWORK BY";
        String creatorName = "ALEX THIMINEUR";

        String teamHeader = "WHEN DABBAH RISES BY";
        String teamName = "THE 5 MUSKETEERS";

        int cy = (int) (h * 0.40);
        int cx = (w - font.getTextWidth(creatorHeader, textScale, tracking)) / 2;
        font.draw(graphicsHandler, creatorHeader, cx, cy, textScale, tracking, new Color(105, 105, 105));

        cx = (w - font.getTextWidth(creatorName, textScale, tracking)) / 2;
        font.draw(graphicsHandler, creatorName, cx, cy + 30, textScale, tracking, Color.WHITE);

        int ty = (int) (h * 0.60);
        int txTeam = (w - font.getTextWidth(teamHeader, textScale, tracking)) / 2;
        font.draw(graphicsHandler, teamHeader, txTeam, ty, textScale, tracking, new Color(105, 105, 105));

        txTeam = (w - font.getTextWidth(teamName, textScale, tracking)) / 2;
        font.draw(graphicsHandler, teamName, txTeam, ty + 30, textScale, tracking, Color.WHITE);

        // 4. Return Instructions (with a subtle pulsing fade)
        String returnInst = "PRESS SPACE TO RETURN";
        int rx = (w - font.getTextWidth(returnInst, textScale, tracking)) / 2;
        int alpha = (int)(150 + 105 * Math.sin(frame * 0.05)); // Pulsing transparency
        alpha = Math.max(0, Math.min(255, alpha));
        font.draw(graphicsHandler, returnInst, rx, h - 60, textScale, tracking, new Color(150, 150, 150, alpha));

        // 5. Scanlines overlay to match MenuScreen
        for (int y = 0; y < h; y += 3) {
            graphicsHandler.drawFilledRectangle(0, y, w, 1, SCANLINE);
        }
    }

    // Calculates scaling dynamically to ensure it fits regardless of resolution
    private void setupLayout(int w, int h) {
        if (layoutW == w) return;
        layoutW = w;

        titleScale = 1;
        while (font.getTextWidth("CREDITS", titleScale + 1, titleScale + 1) <= w * 0.50) {
            titleScale++;
        }
        textScale = Math.max(1, titleScale / 3);
    }
}