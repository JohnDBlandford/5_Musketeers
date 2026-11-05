package Story;

import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import Engine.ScreenManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// STORY: the quest log, opened with Q while walking. Three tabs (LEFT / RIGHT or A / D):
//   QUESTS    every quest you have started or finished, main first. UP / DOWN picks one; the right side shows who gave it,
//             where, its story, each objective (done ones ticked, the current one in gold) and the reward
//   STANDING  your standing with the Concord, the Hollow Chorus and the Kept Grove (-5 to +5), what each word means,
//             and every choice you have made so far
//   TABLETS   the hieroglyph tablets of Worlds 1-3 and how many pieces you have; ENTER reads a finished one
// Q or ESC closes it. Styled like the Codex of Beasts.
public final class QuestLogScreen {
    private boolean open, wasOpen;
    private int tab, selected, tabletSel, frame, choiceScroll;
    private final KeyLocker keys = new KeyLocker();
    private static final Key[] KEYS = { Key.Q, Key.ESC, Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT, Key.W, Key.S, Key.A, Key.D, Key.ENTER, Key.SPACE };
    private static final String[] TABS = { "QUESTS", "STANDING", "TABLETS" };

    private static final Color PANEL = new Color(14, 12, 14), FRAME = new Color(214, 178, 110), IRON = new Color(52, 46, 44);
    private static final Color BLOOD = new Color(110, 18, 18), HEAD = new Color(200, 80, 70), TEXT = new Color(230, 222, 205);
    private static final Color DIM = new Color(150, 140, 125), GOLD = new Color(230, 190, 60), DONE = new Color(120, 150, 110);
    private static final Color MAIN = StoryNPC.MAIN_COLOR, SIDE = StoryNPC.SIDE_COLOR;
    private static final Color[] FACTION = { new Color(220, 210, 180), new Color(220, 70, 70), new Color(130, 200, 120) };
    private static final String[] FACTION_TEXT = {
            "THE BEAST IS A CURSE TO BE ENDED BY FORCE. HOLY WAR, BEAST-SLAYER KNIGHTS, MARTYRS MADE SAINTS.",
            "THE HUNGER IS DESERVED. STORE THE SEED FOR THE LAST WINTER AND LET THE FAITHFUL EAT.",
            "GIVE BACK WHAT WAS TAKEN. LET THE FIELDS GROW WILD AND REMEMBER THE DAY THE SHEPHERD DIED." };

    public boolean isOpen() { return open; }

    // true once, right after it closes (so the Q that closed it can be locked)
    public boolean wasOpen() {
        boolean w = wasOpen;
        wasOpen = false;
        return w;
    }

    public void open() {
        open = true;
        frame = 0;
        for (Key k : KEYS) keys.lockKey(k);
        List<Quest> qs = shown();
        selected = Math.max(0, Math.min(selected, qs.size() - 1));
    }

    private boolean tap(Key k) {
        if (Keyboard.isKeyUp(k)) keys.unlockKey(k);
        if (Keyboard.isKeyDown(k) && !keys.isKeyLocked(k)) { keys.lockKey(k); return true; }
        return false;
    }

    // returns a world number when a tablet should be shown, else 0
    public int update() {
        frame++;
        if (tap(Key.Q) | tap(Key.ESC)) { open = false; wasOpen = true; return 0; }
        if (tap(Key.LEFT) | tap(Key.A)) tab = (tab + TABS.length - 1) % TABS.length;
        if (tap(Key.RIGHT) | tap(Key.D)) tab = (tab + 1) % TABS.length;
        boolean up = tap(Key.UP) | tap(Key.W), down = tap(Key.DOWN) | tap(Key.S), enter = tap(Key.ENTER) | tap(Key.SPACE);
        if (tab == 0) {
            int n = shown().size();
            if (n > 0) {
                if (up) selected = (selected + n - 1) % n;
                if (down) selected = (selected + 1) % n;
            }
        } else if (tab == 1) {
            if (up) choiceScroll = Math.max(0, choiceScroll - 1);
            if (down) choiceScroll = Math.min(Math.max(0, StoryState.choices().size() - 6), choiceScroll + 1);
        } else {
            if (up) tabletSel = (tabletSel + 2) % 3;
            if (down) tabletSel = (tabletSel + 1) % 3;
            if (enter && StoryState.pieceCount(tabletSel + 1) > 0) return tabletSel + 1;
        }
        return 0;
    }

    // the quests the knight knows about: started or finished, main quests first
    private static List<Quest> shown() {
        List<Quest> out = new ArrayList<>();
        for (Quest q : Quest.all()) if (q.main && StoryState.started(q.id)) out.add(q);
        for (Quest q : Quest.all()) if (!q.main && StoryState.started(q.id)) out.add(q);
        return out;
    }

    // ================================================================= drawing

    public void draw(GraphicsHandler gh) {
        int w = ScreenManager.getScreenWidth(), h = ScreenManager.getScreenHeight();
        gh.drawFilledRectangle(0, 0, w, h, new Color(0, 0, 0, 200));
        gh.drawFilledRectangleWithBorder(30, 30, w - 60, h - 60, PANEL, FRAME, 3);
        gh.drawRectangle(36, 36, w - 73, h - 73, BLOOD, 1);
        BattleUI.shadowText(gh, "JOURNAL", 50, 46, 3, TEXT);
        // tabs
        int tx = w - 50;
        for (int i = TABS.length - 1; i >= 0; i--) {
            int tw = BattleUI.textW(TABS[i], 2) + 20;
            tx -= tw;
            boolean on = i == tab;
            gh.drawFilledRectangle(tx, 44, tw, 26, on ? new Color(70, 52, 24) : new Color(28, 24, 24));
            gh.drawRectangle(tx, 44, tw, 26, on ? FRAME : IRON, 1);
            BattleUI.text(gh, TABS[i], tx + 10, 52, 2, on ? GOLD : DIM);
            tx -= 8;
        }
        if (tab == 0) drawQuests(gh, w, h);
        else if (tab == 1) drawStanding(gh, w, h);
        else drawTablets(gh, w, h);
        if (tab == 0) {   // the marker legend
            BattleUI.text(gh, "! ? RED = MAIN", 50, h - 74, 1, MAIN);
            BattleUI.text(gh, "! ? BLUE = SIDE", 50 + BattleUI.textW("! ? RED = MAIN", 1) + 24, h - 74, 1, SIDE);
        }
        BattleUI.textCenter(gh, "LEFT/RIGHT: TAB   UP/DOWN: SELECT   " + (tab == 2 ? "ENTER: READ   " : "") + "Q/ESC: CLOSE",
                w / 2, h - 58, 1, BattleUI.GREY);
    }

    private void drawQuests(GraphicsHandler gh, int w, int h) {
        List<Quest> qs = shown();
        int lx = 50, ly = 96, lw = 280;
        if (qs.isEmpty()) {
            BattleUI.text(gh, "NO QUESTS YET.", lx, ly, 2, DIM);
            BattleUI.text(gh, "TALK TO PEOPLE WITH A GOLD ! OVER THEIR HEADS.", lx, ly + 28, 1, DIM);
            return;
        }
        boolean mainHead = false, sideHead = false;
        int y = ly;
        for (int i = 0; i < qs.size(); i++) {
            Quest q = qs.get(i);
            if (q.main && !mainHead) { BattleUI.text(gh, "MAIN QUESTS - WORLD 1", lx, y, 1, MAIN); y += 16; mainHead = true; }
            if (!q.main && !sideHead) { y += 6; BattleUI.text(gh, "SIDE QUESTS - WORLD 1", lx, y, 1, SIDE); y += 16; sideHead = true; }
            boolean sel = i == selected, done = StoryState.done(q.id);
            if (sel) gh.drawFilledRectangleWithBorder(lx - 4, y - 4, lw + 8, 18, new Color(70, 52, 24), FRAME, 1);
            BattleUI.text(gh, done ? "+" : (q.main ? "!" : "?"), lx, y, 1, done ? DONE : (q.main ? MAIN : SIDE));
            String t = q.title.length() > 40 ? q.title.substring(0, 39) + "." : q.title;
            BattleUI.text(gh, t, lx + 14, y, 1, done ? DONE : TEXT);
            y += 20;
        }
        drawQuestDetail(gh, qs.get(Math.min(selected, qs.size() - 1)), w, h);
    }

    private void drawQuestDetail(GraphicsHandler gh, Quest q, int w, int h) {
        int x = 350, y = 92, dw = w - 50 - x, dh = h - 92 - 80;
        gh.drawFilledRectangleWithBorder(x, y, dw, dh, new Color(24, 20, 20), IRON, 2);
        int tx = x + 14, ty = y + 14, chars = (dw - 28) / 12;
        for (String line : BattleUI.wrap(q.title, chars)) { BattleUI.shadowText(gh, line, tx, ty, 2, q.main ? MAIN : SIDE); ty += 22; }
        BattleUI.text(gh, q.main ? "MAIN QUEST" : "SIDE QUEST", tx, ty, 1, q.main ? MAIN : SIDE);
        if (q.faction >= 0) BattleUI.text(gh, "  -  " + StoryState.FACTIONS[q.faction], tx + BattleUI.textW(q.main ? "MAIN QUEST" : "SIDE QUEST", 1), ty, 1, FACTION[q.faction]);
        ty += 14;
        BattleUI.text(gh, "FROM " + q.giver + "  -  " + q.place, tx, ty, 1, DIM);
        ty += 20;
        for (String line : BattleUI.wrap(q.summary, (dw - 28) / 7)) { BattleUI.text(gh, line, tx, ty, 1, TEXT); ty += 13; }
        ty += 10;
        BattleUI.text(gh, "OBJECTIVES", tx, ty, 1, HEAD);
        ty += 16;
        int stage = StoryState.stage(q.id);
        for (int i = 0; i < q.objectives.length; i++) {
            if (i > stage) break;   // later objectives stay hidden until you reach them
            boolean done = stage >= StoryState.DONE || i < stage;
            String text = Quest.progress(q, q.objectives[i]);
            List<String> lines = BattleUI.wrap(text, (dw - 50) / 7);
            BattleUI.text(gh, done ? "+" : ">", tx, ty, 1, done ? DONE : GOLD);
            for (String line : lines) { BattleUI.text(gh, line, tx + 14, ty, 1, done ? DONE : GOLD); ty += 13; }
            ty += 4;
        }
        if (StoryState.done(q.id)) {
            BattleUI.text(gh, "COMPLETE", tx, ty + 4, 2, DONE);
            ty += 28;
            String next = Quest.nextAfter(q.id);
            if (next != null) for (String line : BattleUI.wrap("NEXT: " + next, (dw - 28) / 7)) { BattleUI.text(gh, line, tx, ty, 1, MAIN); ty += 13; }
        } else {
            String where = Quest.whereNow(q);
            if (where != null) { ty += 4; BattleUI.text(gh, "WHERE: " + where, tx, ty, 1, MAIN); ty += 16; }
        }
        BattleUI.text(gh, "REWARD: " + q.reward, tx, y + dh - 22, 1, DIM);
    }

    private void drawStanding(GraphicsHandler gh, int w, int h) {
        int x = 50, y = 96, bw = w - 100;
        for (int f = 0; f < 3; f++) {
            int v = StoryState.standing(f), by = y + f * 74;
            gh.drawFilledRectangleWithBorder(x, by, bw, 66, new Color(24, 20, 20), IRON, 2);
            BattleUI.shadowText(gh, StoryState.FACTIONS[f], x + 14, by + 10, 2, FACTION[f]);
            BattleUI.textRight(gh, StoryState.standingWord(v) + "  " + (v > 0 ? "+" : "") + v, x + bw - 14, by + 10, 2, v > 0 ? GOLD : (v < 0 ? HEAD : DIM));
            // the bar: -5 .. +5, centre line at 0
            int barX = x + 14, barW = bw - 28, barY = by + 34, mid = barX + barW / 2;
            gh.drawFilledRectangle(barX, barY, barW, 10, new Color(36, 30, 30));
            int len = Math.round(barW / 2f * Math.abs(v) / 5f);
            if (v > 0) gh.drawFilledRectangle(mid, barY, len, 10, FACTION[f]);
            if (v < 0) gh.drawFilledRectangle(mid - len, barY, len, 10, new Color(150, 40, 36));
            gh.drawFilledRectangle(mid - 1, barY - 3, 2, 16, TEXT);
            gh.drawRectangle(barX, barY, barW, 10, IRON, 1);
            BattleUI.text(gh, FACTION_TEXT[f], x + 14, by + 50, 1, DIM);
        }
        int cy = y + 3 * 74 + 8;
        BattleUI.text(gh, "YOUR CHOICES", x, cy, 1, HEAD);
        cy += 16;
        List<String> cs = StoryState.choices();
        if (cs.isEmpty()) BattleUI.text(gh, "NONE YET. WHAT YOU DECIDE IS REMEMBERED, AND IT CHANGES WHO STANDS WITH YOU AT THE END.", x, cy, 1, DIM);
        for (int i = choiceScroll; i < cs.size() && cy < h - 84; i++) {
            BattleUI.text(gh, "- " + cs.get(i), x, cy, 1, TEXT);
            cy += 13;
        }
    }

    private void drawTablets(GraphicsHandler gh, int w, int h) {
        String[] names = { "WORLD 1 - THE SENTENCE", "WORLD 2 - ???", "WORLD 3 - ???" };
        int x = 50, y = 96, cw = w - 100;
        for (int i = 0; i < 3; i++) {
            int n = StoryState.pieceCount(i + 1), by = y + i * 104;
            boolean sel = i == tabletSel;
            gh.drawFilledRectangleWithBorder(x, by, cw, 94, sel ? new Color(46, 36, 22) : new Color(24, 20, 20), sel ? FRAME : IRON, 2);
            TabletScreen.drawThumbnail(gh, i + 1, x + 10, by + 8, 120, 78);
            String title = i == 0 ? names[0] : n > 0 ? (i == 1 ? "WORLD 2 - THE BREAKING" : "WORLD 3 - THE VERDICT") : names[i];
            BattleUI.shadowText(gh, title, x + 146, by + 14, 2, n == 6 ? GOLD : TEXT);
            BattleUI.text(gh, "PIECES " + n + "/6" + (n == 6 ? "  -  COMPLETE" : ""), x + 146, by + 42, 1, n == 6 ? GOLD : DIM);
            String hint = n == 6 ? "ENTER: READ THE CARVING" : n > 0 ? "ENTER: LOOK AT THE PIECES" :
                    i == 0 ? "PIECES ARE HIDDEN IN QUESTS AND SECRETS. THE KEYSTONE IS THE BOSS'S." : "NOT YET FOUND.";
            BattleUI.text(gh, hint, x + 146, by + 60, 1, DIM);
        }
    }
}