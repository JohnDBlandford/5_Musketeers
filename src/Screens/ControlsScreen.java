package Screens;

import Engine.*;
import Game.GameState;
import Game.ScreenCoordinator;
import SpriteFont.MonoSpriteFont;

import java.awt.*;

// Controls screen (from the menu's CONTROLS item), styled like CreditsScreen.
// The keys listed here are the real ones from Player, PlayLevelScreen, WorldMapScreen, InventoryScreen and BattleInput;
// if one of those changes, change its line here too.
public class ControlsScreen extends Screen {
    protected ScreenCoordinator screenCoordinator;
    protected KeyLocker keyLocker = new KeyLocker();

    private MonoSpriteFont font;
    private int frame = 0;

    private static final Color RED = new Color(255, 0, 0);
    private static final Color DARK_RED = new Color(110, 0, 0);
    private static final Color HEADER = new Color(105, 105, 105);
    private static final Color KEY = new Color(255, 230, 0);
    private static final Color SCANLINE = new Color(0, 0, 0, 70);

    // { section title, key, what it does, key, what it does, ... }
    private static final String[][] LEFT = {
            { "EXPLORING", "WASD", "MOVE", "ENTER", "TALK / INTERACT", "I", "INVENTORY", "M", "WORLD MAP", "CLICK", "JUMP BUTTONS" },
            { "INVENTORY", "ARROWS", "MOVE CURSOR", "ENTER", "EQUIP / UNEQUIP", "ESC", "CLOSE" },
    };
    private static final String[][] RIGHT = {
            { "WORLD MAP", "ARROWS", "PICK A PLACE", "ENTER", "TRAVEL THERE", "CLICK", "TRAVEL TO A PIN", "Q / E", "SWITCH WORLD", "M / ESC", "CLOSE MAP" },
            { "BATTLE", "ARROWS", "CHOOSE", "ENTER", "CONFIRM", "1 - 4", "USE ABILITY", "B / X", "BACK", "Y", "ABILITY INFO" },
    };

    private static final String[][] BOTTOM = {
            { "SHOP (MERCHANT & BLACKSMITH)", "ESC", "QUIT", "ENTER", "CONFIRM", "ARROWS", "NAVIGATE" }
    };

    private static final int TITLE_SCALE = 5, HEAD_SCALE = 2, TEXT_SCALE = 2;
    private static final int ROW_H = 22, HEAD_H = 30, SECTION_GAP = 18, KEY_COL = 112;

    public ControlsScreen(ScreenCoordinator screenCoordinator) {
        this.screenCoordinator = screenCoordinator;
    }

    @Override
    public void initialize() {
        font = new MonoSpriteFont();
        frame = 0;
        // the ENTER / SPACE that picked this item must not send us straight back
        keyLocker.lockKey(Key.SPACE);
        keyLocker.lockKey(Key.ENTER);
        keyLocker.lockKey(Key.ESC);
    }

    // true once per key press (not while held)
    private boolean tap(Key key) {
        if (Keyboard.isKeyUp(key)) keyLocker.unlockKey(key);
        if (Keyboard.isKeyDown(key) && !keyLocker.isKeyLocked(key)) {
            keyLocker.lockKey(key);
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        frame++;
        if (tap(Key.SPACE) | tap(Key.ESC)) screenCoordinator.setGameState(GameState.MENU);
    }

    @Override
    public void draw(GraphicsHandler gh) {
        int w = ScreenManager.getScreenWidth();
        int h = ScreenManager.getScreenHeight();
        gh.drawFilledRectangle(0, 0, w, h, Color.BLACK);

        String title = "CONTROLS";
        int tx = (w - font.getTextWidth(title, TITLE_SCALE, TITLE_SCALE)) / 2;
        int sh = Math.max(2, TITLE_SCALE / 3);
        font.draw(gh, title, tx + sh, 40 + sh, TITLE_SCALE, TITLE_SCALE, DARK_RED);
        font.draw(gh, title, tx, 40, TITLE_SCALE, TITLE_SCALE, RED);

        int colW = (w - 100) / 2;
        int leftEnd = drawColumn(gh, LEFT, 50 + 20, 115, 300);
        int rightEnd = drawColumn(gh, RIGHT, 50 + colW + 20, 115, 300);

        // shop block: centered under both columns
        int blockW = 0;
        for (String[] s : BOTTOM) {
            blockW = Math.max(blockW, font.getTextWidth(s[0], HEAD_SCALE, HEAD_SCALE * 2));
            for (int i = 1; i + 1 < s.length; i += 2) {
                blockW = Math.max(blockW, KEY_COL + font.getTextWidth(s[i + 1], TEXT_SCALE, 0));
            }
        }
        drawColumn(gh, BOTTOM, (w - blockW) / 2, Math.max(leftEnd, rightEnd), blockW);

        String back = "PRESS SPACE TO RETURN";
        int alpha = Math.max(0, Math.min(255, (int) (150 + 105 * Math.sin(frame * 0.05))));
        font.draw(gh, back, (w - font.getTextWidth(back, TEXT_SCALE, TEXT_SCALE * 2)) / 2, h - 30, TEXT_SCALE, TEXT_SCALE * 2,
                new Color(150, 150, 150, alpha));

        for (int y = 0; y < h; y += 3) gh.drawFilledRectangle(0, y, w, 1, SCANLINE);
    }

    // draws the sections and returns the y just below the last one
    private int drawColumn(GraphicsHandler gh, String[][] sections, int x, int y, int width) {
        for (String[] s : sections) {
            font.draw(gh, s[0], x, y, HEAD_SCALE, HEAD_SCALE * 2, HEADER);
            gh.drawFilledRectangle(x, y + font.getCapHeight(HEAD_SCALE) + 6, width, 2, DARK_RED);
            y += HEAD_H;
            for (int i = 1; i + 1 < s.length; i += 2) {
                font.draw(gh, s[i], x, y, TEXT_SCALE, 0, KEY);
                font.draw(gh, s[i + 1], x + KEY_COL, y, TEXT_SCALE, 0, Color.WHITE);
                y += ROW_H;
            }
            y += SECTION_GAP;
        }
        return y;
    }
}