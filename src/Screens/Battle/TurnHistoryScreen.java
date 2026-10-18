package Screens.Battle;

import BattleSystem.BattleInput;
import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.Keyboard;
import Engine.Screen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class TurnHistoryScreen extends Screen {
    enum Actor { PLAYER, ENEMY }

    static final float N_TRGT = -1f; // trgrt hp for a move w/o a trgt logged as "-"

    // this inner class represents a single entry in the list
    static final class Entry {
        private final int turnNum;
        private final Actor actor;
        private final String move;
        private int dmgTaken;
        private int hpLeft;

        private Entry(int turnNum, Actor actor, String move, float trgtHp) {
            this.turnNum = turnNum;
            this.actor = actor;
            this.move = move;
             this.hpLeft = trgtHp == N_TRGT ? -1 : toHp(trgtHp);
        }

        void hit(int dmg, float hpAfter) {
            dmgTaken += dmg;
            hpLeft = toHp(hpAfter);
        }
    }

    private static final int MAX_ENTRIES = 200; // max num of entries allowed in the list. no battle should come close to this
    private static final int VISIBLE_ENTRIES = 3; // max num of entries visible w/o expansion
    private static final int MAX_EXPANDED_ENTRIES = 18;

    // layout (pixels). Text is drawn at scale 1.
    private static final int PANEL_W = 290, MARGIN = 8, PAD = 12, HEADER_H = 14, ROW_H = 14;

    // column positions, measured from the left edge of the text area (PANEL_W - 2 * PAD = 266 wide)
    private static final int COL_WHO = 26, COL_MOVE = 40, COL_DMG_RIGHT = 216, COL_HP_RIGHT = 266;
    private static final int DMG_WIDTH = 24;  // room kept for a 4 digit damage number

    // account for the fact that the expanded panel is bigger than the compact one, obviously
    private static final int EXPANDED_W = PANEL_W + 16, FOOTER_H = 16, SCROLLBAR_W = 4;

    // stuff for scroll support
    private static final int HOLD_DELAY = 12, HOLD_STEP = 2;

    // colors using the battle UI
    private static final Color PLAYER_COLOR = BattleUI.BOX_FRAME;
    private static final Color ENEMY_COLOR = BattleUI.RED.darker();

    // background dims on pause
    private static final Color DIM = new Color(0, 0, 0, 140);

    private final List<Entry> entries = new ArrayList<>();
    private int turn;

    // expansion state tracking
    private boolean isExpanded; // is full log open?
    private boolean canExpand; // set each frame, init as false
    private int scrollBackEntries; // how many entries the expanded view is scrolled up from the newest one
    private int scrollHoldFrames; // how long scroll key held for

    protected TurnHistoryScreen() {}

    @Override
    public void initialize() {
        entries.removeAll(entries); // each battle starts with an empty screen
        turn = 0;
        isExpanded = false;
        scrollBackEntries = 0;
        scrollHoldFrames = 0;
    }

    @Override
    public void update() { updateScreen(true); }

    void updateScreen(boolean available) {
        canExpand = available;
        if (!isExpanded) {
            if (available && BattleInput.tap(Key.T)) open();
            return;
        }
        if (!available || BattleInput.tap(Key.T) || BattleInput.back()) {
            close();
            return;
        }
        scroll();
    }

    boolean isExpanded() {
        return isExpanded;
    }

    private void open() {
        isExpanded = true;
        scrollBackEntries = 0;
        scrollHoldFrames = 0;
    }

    private void close() {
        isExpanded = false;
        scrollHoldFrames = 0;
        //  clear key so it cannot also back out of the menu underneath
        BattleInput.reset();
    }

    // one row per tap, and while a key is held it keeps going
    private void scroll() {
        int step = 0;
        if (BattleInput.up()) step = 1;             // toward older entries
        else if (BattleInput.down()) step = -1;     // toward newer entries
        boolean holdUp = Keyboard.isKeyDown(Key.UP) || Keyboard.isKeyDown(Key.W);
        boolean holdDown = Keyboard.isKeyDown(Key.DOWN) || Keyboard.isKeyDown(Key.S);

        if (holdUp == holdDown) scrollHoldFrames = 0;     // nothing held (or both, which cancel out)
        else if (++scrollHoldFrames > HOLD_DELAY && (scrollHoldFrames - HOLD_DELAY) % HOLD_STEP == 0) step = holdUp ? 1 : -1;
        scrollBackEntries = Math.max(0, Math.min(maxScroll(), scrollBackEntries + step));
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - MAX_EXPANDED_ENTRIES);
    }

    // increments the turn
    void nxtTurn() {
        turn++;
    }

    // begins the logging of the turn stats
    Entry beginBattleLog(Actor actor, String move, float trgtHp) {
        // create a new entry and add it to the array
        Entry entry = new Entry(turn, actor, move, trgtHp);
        entries.add(entry);
        // if the size exceeds MAX_ENTRIES, remove the earliest entry
        if (entries.size() > MAX_ENTRIES) entries.removeFirst();
        return entry;
    }

    private static int toHp(float hp) {
        return Math.max(0, Math.round(hp));
    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        if (isExpanded) drawExpanded(graphicsHandler);
        else drawCompact(graphicsHandler);
    }

    // the newest VISIBLE_ENTRIES moves in the top right corner
    private void drawCompact(GraphicsHandler graphicsHandler) {
        int h = PAD + HEADER_H + VISIBLE_ENTRIES * ROW_H + PAD;
        int x = BattleUI.W() - PANEL_W - MARGIN, y = MARGIN;
        BattleUI.box(graphicsHandler, x, y, PANEL_W, h);

        int left = x + PAD, top = y + PAD;
        drawHeader(graphicsHandler, left, top);

        if (entries.isEmpty()) {
            BattleUI.text(graphicsHandler, "NO MOVES YET", left, top + HEADER_H, 1, BattleUI.INK_SOFT);
        } else {
            int first = Math.max(0, entries.size() - VISIBLE_ENTRIES);
            for (int i = first; i < entries.size(); i++) {
                Color text = i == entries.size() - 1 ? BattleUI.INK : BattleUI.INK_SOFT;   // older rows are dimmed
                drawRow(graphicsHandler, entries.get(i), left, top + HEADER_H + (i - first) * ROW_H, text);
            }
        }
        if (canExpand) {   // a small reminder under the panel that the full log exists
            String hint = "T: FULL LOG";
            BattleUI.shadowText(graphicsHandler, hint, x + PANEL_W - 4 - BattleUI.textW(hint, 1), y + h + 4, 1, BattleUI.WHITE);
        }
    }

    // the whole log: dims the battle, then a taller panel with the newest entries at the bottom and a scrollbar
    private void drawExpanded(GraphicsHandler graphicsHandler) {
        graphicsHandler.drawFilledRectangle(0, 0, BattleUI.W(), BattleUI.H(), DIM);
        int h = PAD + HEADER_H + MAX_EXPANDED_ENTRIES * ROW_H + FOOTER_H + PAD;
        int x = BattleUI.W() - EXPANDED_W - MARGIN, y = MARGIN;
        BattleUI.box(graphicsHandler, x, y, EXPANDED_W, h);

        int left = x + PAD, top = y + PAD, rowsTop = top + HEADER_H;
        drawHeader(graphicsHandler, left, top);

        if (entries.isEmpty()) {
            BattleUI.text(graphicsHandler, "NO MOVES YET", left, rowsTop, 1, BattleUI.INK_SOFT);
        } else {
            int first = Math.max(0, entries.size() - MAX_EXPANDED_ENTRIES - scrollBackEntries);
            int last = Math.min(entries.size(), first + MAX_EXPANDED_ENTRIES);
            for (int i = first; i < last; i++) {
                drawRow(graphicsHandler, entries.get(i), left, rowsTop + (i - first) * ROW_H, BattleUI.INK);
            }
            drawScrollbar(graphicsHandler, left + COL_HP_RIGHT + 8, rowsTop, MAX_EXPANDED_ENTRIES * ROW_H, first);
        }
        int footerY = rowsTop + MAX_EXPANDED_ENTRIES * ROW_H + 4;
        BattleUI.text(graphicsHandler, "W/S SCROLL", left, footerY, 1, BattleUI.INK_SOFT);
        BattleUI.textRight(graphicsHandler, "T/B CLOSE", left + COL_HP_RIGHT, footerY, 1, BattleUI.INK_SOFT);
    }

    private void drawHeader(GraphicsHandler graphicsHandler, int left, int top) {
        BattleUI.text(graphicsHandler, "TURN", left, top, 1, BattleUI.INK_SOFT);
        BattleUI.text(graphicsHandler, "MOVE", left + COL_MOVE, top, 1, BattleUI.INK_SOFT);
        BattleUI.textRight(graphicsHandler, "DMG", left + COL_DMG_RIGHT, top, 1, BattleUI.INK_SOFT);
        BattleUI.textRight(graphicsHandler, "HP", left + COL_HP_RIGHT, top, 1, BattleUI.INK_SOFT);
    }

    // one entry: turn number, who (P / E), move, damage dealt, HP its target has left
    private void drawRow(GraphicsHandler graphicsHandler, Entry entry, int left, int ry, Color text) {
        int moveMaxW = COL_DMG_RIGHT - DMG_WIDTH - COL_MOVE;
        boolean player = entry.actor == Actor.PLAYER;
        BattleUI.text(graphicsHandler, String.valueOf(entry.turnNum), left, ry, 1, text);
        BattleUI.text(graphicsHandler, player ? "P" : "E", left + COL_WHO, ry, 1, player ? PLAYER_COLOR : ENEMY_COLOR);
        BattleUI.text(graphicsHandler, fit(entry.move, moveMaxW), left + COL_MOVE, ry, 1, text);
        BattleUI.textRight(graphicsHandler, String.valueOf(entry.dmgTaken), left + COL_DMG_RIGHT, ry, 1, text);
        BattleUI.textRight(graphicsHandler, entry.hpLeft < 0 ? "-" : String.valueOf(entry.hpLeft), left + COL_HP_RIGHT, ry, 1, text);
    }

    // a thin bar beside the rows; its thumb shows where the visible rows sit in the whole list. Only drawn when the list scrolls.
    private void drawScrollbar(GraphicsHandler graphicsHandler, int x, int y, int h, int first) {
        int total = entries.size();
        if (total <= MAX_EXPANDED_ENTRIES) return;
        graphicsHandler.drawFilledRectangle(x, y, SCROLLBAR_W, h, BattleUI.BOX_LINE);
        int thumbH = Math.max(12, h * MAX_EXPANDED_ENTRIES / total);
        int thumbY = y + (h - thumbH) * first / (total - MAX_EXPANDED_ENTRIES);
        graphicsHandler.drawFilledRectangle(x, thumbY, SCROLLBAR_W, thumbH, BattleUI.BOX_FRAME);
    }

    // cuts a long move name so it fits in maxW pixels
    private static String fit(String s, int maxW) {
        if (BattleUI.textW(s, 1) <= maxW) return s;
        while (s.length() > 1 && BattleUI.textW(s + ".", 1) > maxW) s = s.substring(0, s.length() - 1);
        return s + ".";
    }
}
