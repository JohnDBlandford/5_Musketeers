package Screens;

import BattleSystem.BattleUI;
import Engine.*;
import Game.GameState;
import Game.ScreenCoordinator;
import SpriteFont.MonoSpriteFont;

import java.awt.*;

// About Us screen (from the menu's ABOUT US item), styled like CreditsScreen.
// Edit ABOUT for the text and TEAM for the member names (an empty TEAM hides that block). Long lines wrap by themselves.
public class AboutScreen extends Screen {
    protected ScreenCoordinator screenCoordinator;
    protected KeyLocker keyLocker = new KeyLocker();

    private MonoSpriteFont font;
    private int frame = 0;

    private static final Color RED = new Color(255, 0, 0);
    private static final Color DARK_RED = new Color(110, 0, 0);
    private static final Color HEADER = new Color(105, 105, 105);
    private static final Color BODY = new Color(220, 220, 220);
    private static final Color SCANLINE = new Color(0, 0, 0, 70);

    private static final String[] ABOUT = {
            "WHEN DABBAH RISES IS A 2D PIXEL RPG MADE IN JAVA BY THE 5 MUSKETEERS.",
            "ROAM THE WORLDS AS A LONE KNIGHT, DISCOVER NEW PLACES, AND SURVIVE TURN-BASED BATTLES AS YOUR GEAR AND STRENGTH GROW.",
            "EVERY WORLD LEADS CLOSER TO THE LAST ONE, WHERE DABBAH WAITS.",
    };
    private static final String[] TEAM = { }; // e.g. { "NAME ONE", "NAME TWO", ... }

    private static final int TITLE_SCALE = 5, TEXT_SCALE = 2, LINE_H = 24, PARA_GAP = 14;

    public AboutScreen(ScreenCoordinator screenCoordinator) {
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

        String title = "ABOUT US";
        int tx = (w - font.getTextWidth(title, TITLE_SCALE, TITLE_SCALE)) / 2;
        int sh = Math.max(2, TITLE_SCALE / 3);
        font.draw(gh, title, tx + sh, 40 + sh, TITLE_SCALE, TITLE_SCALE, DARK_RED);
        font.draw(gh, title, tx, 40, TITLE_SCALE, TITLE_SCALE, RED);

        int maxChars = (w - 120) / (6 * TEXT_SCALE); // every letter is 6 px wide at scale 1
        int y = 125;
        for (String para : ABOUT) {
            for (String line : BattleUI.wrap(para, maxChars)) {
                drawCentered(gh, line, y, BODY);
                y += LINE_H;
            }
            y += PARA_GAP;
        }

        if (TEAM.length > 0) {
            y += 10;
            drawCentered(gh, "THE TEAM", y, HEADER);
            y += LINE_H + 6;
            for (String name : TEAM) {
                drawCentered(gh, name.toUpperCase(), y, Color.WHITE);
                y += LINE_H;
            }
        }

        String back = "PRESS SPACE TO RETURN";
        int alpha = Math.max(0, Math.min(255, (int) (150 + 105 * Math.sin(frame * 0.05))));
        font.draw(gh, back, (w - font.getTextWidth(back, TEXT_SCALE, TEXT_SCALE * 2)) / 2, h - 60, TEXT_SCALE, TEXT_SCALE * 2,
                new Color(150, 150, 150, alpha));

        for (int yy = 0; yy < h; yy += 3) gh.drawFilledRectangle(0, yy, w, 1, SCANLINE);
    }

    private void drawCentered(GraphicsHandler gh, String s, int y, Color c) {
        font.draw(gh, s, (w() - font.getTextWidth(s, TEXT_SCALE, 0)) / 2, y, TEXT_SCALE, 0, c);
    }

    private static int w() { return ScreenManager.getScreenWidth(); }
}