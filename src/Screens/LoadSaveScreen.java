package Screens;

import Engine.*;
import Game.GameState;
import Game.ScreenCoordinator;
import Save.Save;
import Save.SaveData;
import SpriteFont.MonoSpriteFont;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// Load screen styled to match the MenuScreen / CreditsScreen: lists every save with its name and when it was saved.
// UP/DOWN (or W/S) to choose, SPACE/ENTER to load, ESC to go back to the menu
public class LoadSaveScreen extends Screen {
    private static final String TITLE = "LOAD SAVE";
    private static final int VISIBLE_ROWS = 5; // how many saves fit on screen before the list scrolls

    private static final Color RED = new Color(255, 0, 0);
    private static final Color DARK_RED = new Color(110, 0, 0);
    private static final Color YELLOW = new Color(255, 210, 0);
    private static final Color GREY = new Color(105, 105, 105);
    private static final Color LIGHT_GREY = new Color(150, 150, 150);
    private static final Color SCANLINE = new Color(0, 0, 0, 70);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MMM dd yyyy  hh:mm a");

    private final ScreenCoordinator screenCoordinator;
    private final KeyLocker keyLocker = new KeyLocker();

    private MonoSpriteFont font;
    private List<Save.SaveInfo> saves;
    private int selected = 0;
    private int scroll = 0;
    private int frame = 0;
    private String message; // shown briefly if a save fails to load
    private int messageTimer = 0;

    private int layoutW = -1;
    private int titleScale, textScale, smallScale;

    public LoadSaveScreen(ScreenCoordinator screenCoordinator) {
        this.screenCoordinator = screenCoordinator;
    }

    @Override
    public void initialize() {
        font = new MonoSpriteFont();
        saves = Save.listSaves();
        selected = 0;
        scroll = 0;
        frame = 0;
        layoutW = -1;
        // lock keys so the key that opened this screen does not instantly trigger something
        keyLocker.lockKey(Key.SPACE);
        keyLocker.lockKey(Key.ENTER);
        keyLocker.lockKey(Key.UP);
        keyLocker.lockKey(Key.DOWN);
        keyLocker.lockKey(Key.ESC);
    }

    // true once per key press (not while held)
    private boolean tap(Key key) {
        if (Keyboard.isKeyDown(key) && !keyLocker.isKeyLocked(key)) {
            keyLocker.lockKey(key);
            return true;
        }
        if (Keyboard.isKeyUp(key)) {
            keyLocker.unlockKey(key);
        }
        return false;
    }

    @Override
    public void update() {
        frame++;
        if (messageTimer > 0)
            messageTimer--;

        boolean up = tap(Key.UP) | tap(Key.W);
        boolean down = tap(Key.DOWN) | tap(Key.S);
        boolean confirm = tap(Key.SPACE) | tap(Key.ENTER);
        boolean back = tap(Key.ESC);

        if (back) {
            screenCoordinator.setGameState(GameState.MENU);
            return;
        }
        if (saves.isEmpty()) {
            if (confirm)
                screenCoordinator.setGameState(GameState.MENU);
            return;
        }

        if (up)
            selected = (selected + saves.size() - 1) % saves.size();
        if (down)
            selected = (selected + 1) % saves.size();
        // keep the selected row inside the visible window
        if (selected < scroll)
            scroll = selected;
        if (selected >= scroll + VISIBLE_ROWS)
            scroll = selected - VISIBLE_ROWS + 1;

        if (confirm) {
            SaveData data = Save.load(saves.get(selected).file);
            if (data == null) {
                message = "SAVE COULD NOT BE READ";
                messageTimer = 120;
            } else {
                screenCoordinator.setPendingLoad(data);
                screenCoordinator.setGameState(GameState.LEVEL);
            }
        }
    }

    @Override
    public void draw(GraphicsHandler gh) {
        int w = ScreenManager.getScreenWidth();
        int h = ScreenManager.getScreenHeight();
        setupLayout(w);

        gh.drawFilledRectangle(0, 0, w, h, Color.BLACK);

        // title with the same red / dark-red shadow as the other screens
        int titleY = (int) (h * 0.10);
        int tx = (w - font.getTextWidth(TITLE, titleScale, titleScale)) / 2;
        int sh = Math.max(2, titleScale / 3);
        font.draw(gh, TITLE, tx + sh, titleY + sh, titleScale, titleScale, DARK_RED);
        font.draw(gh, TITLE, tx, titleY, titleScale, titleScale, RED);

        int tracking = textScale * 2;
        if (saves.isEmpty()) {
            drawCentered(gh, "NO SAVES FOUND", w, (int) (h * 0.45), textScale, tracking, LIGHT_GREY);
            drawCentered(gh, "PRESS K IN GAME TO SAVE", w, (int) (h * 0.45) + 30, smallScale, smallScale, GREY);
        } else {
            drawList(gh, w, h, tracking);
        }

        if (messageTimer > 0 && message != null)
            drawCentered(gh, message, w, h - 100, textScale, tracking, RED);

        // controls hint, pulsing like the credits screen
        String hint = saves.isEmpty() ? "PRESS SPACE OR ESC TO RETURN" : "SPACE TO LOAD    ESC TO RETURN";
        int alpha = Math.max(0, Math.min(255, (int) (150 + 105 * Math.sin(frame * 0.05))));
        drawCentered(gh, hint, w, h - 50, smallScale, smallScale, new Color(150, 150, 150, alpha));

        for (int y = 0; y < h; y += 3)
            gh.drawFilledRectangle(0, y, w, 1, SCANLINE);
    }

    // each save is a name line with its save time underneath; the selected one is white with the eye cursor
    private void drawList(GraphicsHandler gh, int w, int h, int tracking) {
        int rowH = font.getCapHeight(textScale) + font.getCapHeight(smallScale) + 26;
        int listW = (int) (w * 0.60);
        int x0 = (w - listW) / 2 + 28;
        int y0 = (int) (h * 0.30);

        int end = Math.min(saves.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            Save.SaveInfo save = saves.get(i);
            int y = y0 + (i - scroll) * rowH;
            boolean on = i == selected;
            int shake = (on && frame % 47 < 2) ? (frame % 2 == 0 ? 1 : -1) : 0; // the selected name trembles

            font.draw(gh, save.name.toUpperCase(), x0 + shake, y, textScale, tracking, on ? Color.WHITE : LIGHT_GREY);
            String time = "SAVED " + TIME_FORMAT.format(new Date(save.savedAt)).toUpperCase();
            font.draw(gh, time, x0, y + font.getCapHeight(textScale) + 10, smallScale, smallScale, on ? RED : GREY);

            if (on) {
                int px = Math.max(2, textScale * 7 / 5);
                drawCursor(gh, x0 - px * 7 - 16, y + (font.getCapHeight(textScale) - 5 * px) / 2, px);
            }
        }

        // little arrows when there are more saves above / below
        if (scroll > 0)
            drawCentered(gh, "^", w, y0 - 24, smallScale, smallScale, GREY);
        if (end < saves.size())
            drawCentered(gh, "v", w, y0 + VISIBLE_ROWS * rowH - 8, smallScale, smallScale, GREY);
    }

    // same blinking slit-pupil eye as the MenuScreen cursor
    private static final String[] CURSOR = { "..###..", ".##.##.", "###.###", ".##.##.", "..###.." };
    private static final String[] CURSOR_BLINK = { ".......", ".......", "#######", ".......", "......." };

    private void drawCursor(GraphicsHandler gh, int x, int y, int px) {
        String[] pat = frame % 220 < 6 ? CURSOR_BLINK : CURSOR;
        for (int r = 0; r < pat.length; r++)
            for (int c = 0; c < pat[r].length(); c++)
                if (pat[r].charAt(c) == '#')
                    gh.drawFilledRectangle(x + c * px, y + r * px, px, px, YELLOW);
    }

    private void drawCentered(GraphicsHandler gh, String text, int w, int y, int scale, int tracking, Color color) {
        font.draw(gh, text, (w - font.getTextWidth(text, scale, tracking)) / 2, y, scale, tracking, color);
    }

    // whole-number pixel scales relative to the window, like the other screens
    private void setupLayout(int w) {
        if (layoutW == w)
            return;
        layoutW = w;
        titleScale = 1;
        while (font.getTextWidth(TITLE, titleScale + 1, titleScale + 1) <= w * 0.50)
            titleScale++;
        textScale = Math.max(1, titleScale / 3);
        smallScale = Math.max(1, textScale / 2 + 1);
    }
}
