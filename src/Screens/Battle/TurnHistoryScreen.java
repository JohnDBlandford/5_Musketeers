package Screens.Battle;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
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

    // layout (pixels). Text is drawn at scale 1.
    private static final int PANEL_W = 290, MARGIN = 8, PAD = 12, HEADER_H = 14, ROW_H = 14;
    // column positions, measured from the left edge of the text area (PANEL_W - 2 * PAD = 266 wide)
    private static final int COL_WHO = 26, COL_MOVE = 40, COL_DMG_RIGHT = 216, COL_HP_RIGHT = 266;
    private static final int DMG_WIDTH = 24;              // room kept for a 4 digit damage number

    // colors using the battle UI
    private static final Color PLAYER_COLOR = BattleUI.BOX_FRAME;
    private static final Color ENEMY_COLOR = BattleUI.RED.darker();

    private final List<Entry> entries = new ArrayList<>();
    private int turn;

    protected TurnHistoryScreen() {}

    @Override
    public void initialize() {
        entries.removeAll(entries);
        turn = 0;
    }

    @Override
    public void update() {}

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
        int h = PAD + HEADER_H + VISIBLE_ENTRIES * ROW_H + PAD;
        int x = BattleUI.W() - PANEL_W - MARGIN, y = MARGIN;
        BattleUI.box(graphicsHandler, x, y, PANEL_W, h);

        int left = x + PAD, top = y + PAD;
        BattleUI.text(graphicsHandler, "TURN", left, top, 1, BattleUI.INK_SOFT);
        BattleUI.text(graphicsHandler, "MOVE", left + COL_MOVE, top, 1, BattleUI.INK_SOFT);
        BattleUI.textRight(graphicsHandler, "DMG", left + COL_DMG_RIGHT, top, 1, BattleUI.INK_SOFT);
        BattleUI.textRight(graphicsHandler, "HP", left + COL_HP_RIGHT, top, 1, BattleUI.INK_SOFT);

        if (entries.isEmpty()) {
            BattleUI.text(graphicsHandler, "NO MOVES YET", left, top + HEADER_H, 1, BattleUI.INK_SOFT);
            return;
        }
        int first = Math.max(0, entries.size() - VISIBLE_ENTRIES);
        int moveMaxW = COL_DMG_RIGHT - DMG_WIDTH - COL_MOVE;
        for (int i = first; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int ry = top + HEADER_H + (i - first) * ROW_H;
            Color text = i == entries.size() - 1 ? BattleUI.INK : BattleUI.INK_SOFT;   // older rows are dimmed
            boolean player = entry.actor == Actor.PLAYER;
            BattleUI.text(graphicsHandler, String.valueOf(entry.turnNum), left, ry, 1, text);
            BattleUI.text(graphicsHandler, player ? "P" : "E", left + COL_WHO, ry, 1, player ? PLAYER_COLOR : ENEMY_COLOR);
            BattleUI.text(graphicsHandler, fit(entry.move, moveMaxW), left + COL_MOVE, ry, 1, text);
            BattleUI.textRight(graphicsHandler, String.valueOf(entry.dmgTaken), left + COL_DMG_RIGHT, ry, 1, text);
            BattleUI.textRight(graphicsHandler, entry.hpLeft < 0 ? "-" : String.valueOf(entry.hpLeft), left + COL_HP_RIGHT, ry, 1, text);
        }
    }

    // cuts a long move name so it fits in maxW pixels
    private static String fit(String s, int maxW) {
        if (BattleUI.textW(s, 1) <= maxW) return s;
        while (s.length() > 1 && BattleUI.textW(s + ".", 1) > maxW) s = s.substring(0, s.length() - 1);
        return s + ".";
    }
}
