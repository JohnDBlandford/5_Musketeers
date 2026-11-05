package Screens;

import BattleSystem.BattleEnemy;
import BattleSystem.BattleUI;
import BattleSystem.EnemyMoves;
import Engine.*;
import NPCs.Enemy;
import BattleSystem.Balance;
import Encounters.EnemyFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

// The Codex of Beasts: an index of every enemy, like a Pokedex. Opened from the world map (B).
// An enemy stays "???" until the knight has fought it once (PlayLevelScreen calls markSeen when a battle starts).
//
// Almost everything shown is read LIVE from the enemy classes, so it stays right when an enemy file changes:
//   level range, HP / ATK / DEF / SPD, XP and gold   from sample instances of the class
//   spawn chance                                      from each class's own spawnRate, as a share of its world
//   moveset (power, hits, accuracy, stat effects)     by asking the class for its battle moves many times (chooseBattleMove),
//                                                     at full HP and again when badly hurt, so low-HP moves show up too
//   levels, stats and rewards                         from Balance (BALANCE: the world's level band, not the class's own numbers)
//   PP of each move                                   from Balance.enemyMaxPP
// Only the PASSIVE lines are written by hand below (passives live in code, not data): update them if a passive changes.
//
// FIXES: the list SCROLLS now (it ran off the bottom of the window once World 4 was added); the selected row is always
// kept in view, and the mouse wheel scrolls it. The tier line no longer says "REVENANT  REVENANT".
// GLITCH: everything written about the glitch entity is corrupted (letters eaten by noise, changing every few frames)
// or in binary, or both: name, world, stats, passive and moves. Its portrait is a box of static with skulls.
// CODEX CHANGES (search for "CODEX:"): DABBAH has a picture now (it had none: it has no 2D sprite, only the 3D model of the
// final fight). Its page shows its THREE FORMS side by side, rendered by FinalBossPresentation.formPortrait. Each form stays
// a black silhouette marked "?" until the knight has faced it (PlayBattleScreen calls markDabbahForm), win or lose: reach
// form 3 and die, and all three show. Its passive names only the forms you've seen.
public final class CodexOfBeastsScreen extends Screen {

    // ================================================================== data

    // passives, written from the enemy classes' battle hooks
    private static final java.util.Map<String, String> PASSIVES = new LinkedHashMap<>();
    static {
        PASSIVES.put("STARVING WRETCH", "COWARD: BELOW 25% HP IT MAY RUN AWAY, MORE LIKELY EVERY TURN.");
        PASSIVES.put("RECKONER BRUTE", "GUARD: +50% DEF FOR ITS FIRST 5 TURNS, THEN THE GUARD WEARS OFF.");
        PASSIVES.put("ASH WOLF", "AMBUSHER: ASH AMBUSH HITS HARDER ON TURN 1. PREDATORY STRIKE GAINS +40% WHEN YOU ARE BELOW 35% HP.");
        PASSIVES.put("HOLLOW CHORUS ZEALOT", "SIPHON: SIPHON STRIKE HEALS IT FOR 20% OF THE DAMAGE DEALT. BELOW 40% HP IT MAY USE DOOMSDAY RITE.");
        PASSIVES.put("THE FAMINE WARDEN", "EVERY 3RD TURN IT UNLEASHES FAMINE CLEAVE.");
        PASSIVES.put("RAIDER SKIRMISHER", "OPPORTUNIST: EVERY MOVE HITS 10% HARDER WHEN YOU ARE BELOW 35% HP.");
        PASSIVES.put("MILITIA BRUTE", "GUARD STANCE: +30% DEF UNTIL ITS NEXT ATTACK.");
        PASSIVES.put("BLIGHTED BOAR", "NONE.");
        PASSIVES.put("BARON'S VANGUARD", "BELOW 50% HP IT ALWAYS USES DUAL FLURRY.");
        PASSIVES.put("BARON OF THE CLEARED WOOD", "EVERY 3RD TURN IT UNLEASHES CLEARED WOOD CLEAVE.");
        PASSIVES.put("CORRUPTED SENTINEL", "NONE.");
        PASSIVES.put("GAZER BEAST", "NONE.");
        PASSIVES.put("ABYSSAL HUSK", "RECKLESS: RECKLESS LUNGE LOWERS ITS OWN DEF.");
        PASSIVES.put("ELDRITCH OVERSEER", "BELOW 50% HP IT MAY USE ELDRITCH PULSE.");
        PASSIVES.put("THE WATCHER IN THE ASH", "EVERY 3RD TURN IT UNLEASHES ASHEN OBLIVION.");
        // World 4 (same classes underneath, so the same passives)
        PASSIVES.put("RISEN FAMINE WARDEN", "EVERY 3RD TURN IT UNLEASHES FAMINE CLEAVE.");
        PASSIVES.put("RISEN BARON", "EVERY 3RD TURN IT UNLEASHES CLEARED WOOD CLEAVE.");
        PASSIVES.put("RISEN WATCHER", "EVERY 3RD TURN IT UNLEASHES ASHEN OBLIVION.");
        PASSIVES.put("RISEN RECKONER BRUTE", "GUARD: +50% DEF FOR ITS FIRST 5 TURNS, THEN THE GUARD WEARS OFF.");
        PASSIVES.put("RISEN MILITIA BRUTE", "GUARD STANCE: +30% DEF UNTIL ITS NEXT ATTACK.");
        PASSIVES.put("RISEN SENTINEL", "NONE.");
        PASSIVES.put(NPCs.World4.GlitchEntity.ID, "IT DOES NO DAMAGE. IT JOINS 1 WORLD 4 FIGHT IN 8. ITS MOVES HIDE YOUR HEALTH, "
                + "HALVE YOUR STATS, TEAR OUT YOUR MOVES, AND BLEED ITS ALLY INTO A ONE-HIT KILLER.");   // GLITCH
        PASSIVES.put("DABBAH, BEAST OF THE EARTH", "THREE PHASES: BEAST OF THE EARTH, SCOURGE OF THE ASH, THE UNBOUND RUIN. "
                + "EACH PHASE RESTORES ITS HP AND ADDS +10% POWER. EVERY 3RD TURN: JUDGEMENT OF DABBAH.");
    }

    private static final String[] WORLD_NAMES = { "", "WORLD 1 - THE RECKONING", "WORLD 2 - THE LONG EROSION",
            "WORLD 3 - THE AGE OF WATCHING", "WORLD 4 - DABBAH'S HOLLOW" };

    // BALANCE: the roster now comes from EnemyFactory.roster() (same enemies, now with their world and archetype), and the
    // levels / stats / rewards from Balance (the level band of the enemy's world). Moves are still read from each class.

    public static final class MoveInfo {
        public String name;
        public float minPower = Float.MAX_VALUE, maxPower = 0;
        public int minHits = Integer.MAX_VALUE, maxHits = 0;
        public float accuracy = 1f;
        public boolean status, flee;
        public int pp;   // BALANCE: uses per battle (Balance.enemyMaxPP)
        public final List<String> effects = new ArrayList<>();
    }

    public static final class Entry {
        public String name;
        public int number, world;
        public boolean boss;
        public Balance.Archetype archetype;   // BALANCE
        public int minLv = Integer.MAX_VALUE, maxLv = 0;
        public float spawnRate, spawnShare; // spawnShare = percent of that world's random encounters
        public final int[] hp = range(), atk = range(), def = range(), spd = range(), xp = range(), gold = range();
        public String passive = "";
        public final List<MoveInfo> moves = new ArrayList<>();
        public BufferedImage portrait, silhouette;
        public BufferedImage[] forms, formShadows;   // CODEX: Dabbah's three forms (null for every other enemy)

        private static int[] range() { return new int[] { Integer.MAX_VALUE, 0 }; }
    }

    private static List<Entry> entries;
    private static final Set<String> SEEN = new HashSet<>();

    public static void markSeen(String enemyName) { if (enemyName != null) SEEN.add(enemyName.toUpperCase()); }
    public static boolean isSeen(Entry e) { return SEEN.contains(e.name); }
    public static void resetSeen() { SEEN.clear(); dabbahForms = 0; }

    // CODEX: how many of Dabbah's forms the knight has faced (0-3). It only ever goes up.
    private static int dabbahForms;

    public static void markDabbahForm(int form) { dabbahForms = Math.max(dabbahForms, Math.min(3, form)); }

    public static int dabbahFormsSeen() { return dabbahForms; }

    private static final String[] DABBAH_FORMS = { "BEAST OF THE EARTH", "SCOURGE OF THE ASH", "THE UNBOUND RUIN" };

    public static int seenCount() {
        int n = 0;
        for (Entry e : entries()) if (isSeen(e)) n++;
        return n;
    }

    // the enemies that can appear in random encounters on this map (empty for a map with none)
    public static List<Entry> pool(String mapName) {
        int w = mapName.contains("2") ? 2 : (mapName.contains("3") ? 3 : (mapName.contains("4") ? 4 : 1));
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries()) if (e.world == w && w != 4) out.add(e);
        return out;
    }

    // built once, the first time the Codex of Beasts or the map's enemy pool is shown
    public static List<Entry> entries() {
        if (entries != null) return entries;
        entries = new ArrayList<>();
        float[] worldTotal = new float[5];
        List<EnemyFactory.Spec> roster = EnemyFactory.roster();
        for (int i = 0; i < roster.size(); i++) {
            EnemyFactory.Spec s = roster.get(i);
            Entry e = new Entry();
            e.number = i + 1;
            e.world = s.world;
            e.boss = s.boss();
            e.archetype = s.archetype;
            try {
                study(s, e);
            } catch (RuntimeException ex) {
                System.out.println("Codex of Beasts: could not read enemy #" + e.number + " (" + ex + ")");
                if (e.name == null) e.name = "UNKNOWN";
            }
            if (!e.boss) worldTotal[s.world] += e.spawnRate;   // bosses aren't part of the normal spawn share
            entries.add(e);
        }
        for (Entry e : entries) e.spawnShare = e.boss || worldTotal[e.world] <= 0 ? 0 : e.spawnRate * 100f / worldTotal[e.world];
        return entries;
    }

    private static void study(EnemyFactory.Spec s, Entry e) {
        // BALANCE: levels from the world's band (bosses at its top, Dabbah 70); stats at the lowest and highest level
        Enemy first = s.make.get();
        int[] band = Balance.band(s.world);
        Random r = new Random(1);
        e.minLv = s.archetype.isBoss() ? Balance.spawnLevel(s.world, s.archetype, 1, r) : band[0];
        e.maxLv = s.archetype.isBoss() ? e.minLv : band[1];
        for (int lv : new int[] { e.minLv, e.maxLv }) {
            grow(e.hp, Balance.enemyHp(lv, s.archetype)); grow(e.atk, Balance.enemyAtk(lv, s.archetype));
            grow(e.def, Balance.enemyDef(lv, s.archetype)); grow(e.spd, Balance.enemySpd(lv, s.archetype));
            grow(e.xp, Balance.enemyXp(lv, s.archetype)); grow(e.gold, Balance.enemyGold(lv, s.archetype));
        }
        e.name = first.getEnemyName().toUpperCase();
        e.spawnRate = Math.max(0f, first.getSpawnRate());
        e.passive = PASSIVES.getOrDefault(e.name, "");
        e.portrait = portraitOf(first);
        e.silhouette = silhouette(e.portrait);
        if (first instanceof NPCs.World1.Dabbah) {   // CODEX: its three forms, drawn by the final fight's 3D renderer
            e.forms = new BufferedImage[3];
            e.formShadows = new BufferedImage[3];
            for (int f = 0; f < 3; f++) {
                e.forms[f] = Screens.Battle.FinalBossPresentation.formPortrait(f + 1);
                e.formShadows[f] = silhouette(e.forms[f]);
            }
            if (e.forms[0] != null) { e.portrait = e.forms[0]; e.silhouette = e.formShadows[0]; }
        }

        // the moveset: ask a fresh instance for its moves many times, healthy and then badly hurt, then through each boss phase
        if (!(first instanceof BattleEnemy)) return;
        Enemy en = s.make.get();
        BattleEnemy be = (BattleEnemy) en;
        java.util.Map<String, MoveInfo> found = new LinkedHashMap<>();
        Random rng = new Random(7);
        for (int phase = 0; phase < 4; phase++) {
            if (phase == 0) be.onBattleStart();
            for (int turn = 0; turn < 60; turn++) record(found, be.chooseBattleMove(rng, null), s.boss());
            if (en.getCurrentHP() > en.getMaxHP() * 0.3f) {
                en.applyDamage(en.getCurrentHP() - en.getMaxHP() * 0.2f, null); // badly hurt: low-HP moves appear
                for (int turn = 0; turn < 60; turn++) record(found, be.chooseBattleMove(rng, null), s.boss());
            }
            String[] next = be.onBattleDefeat(); // a boss with phases revives here; others return null
            if (next == null) break;
        }
        e.moves.addAll(found.values());
    }

    private static void record(java.util.Map<String, MoveInfo> found, EnemyMoves.Move m, boolean boss) {
        if (m == null) return;
        String key = m.flees ? "RUNS AWAY" : m.name.toUpperCase();
        MoveInfo mi = found.get(key);
        if (mi == null) {
            mi = new MoveInfo();
            mi.name = key;
            mi.flee = m.flees;
            mi.status = !m.damages && !m.flees;
            mi.accuracy = m.accuracy();
            mi.pp = m.flees ? -1 : Balance.enemyMaxPP(m, boss);   // BALANCE
            for (EnemyMoves.Fx fx : m.effects()) {
                String stat = fx.stat == 0 ? "ATK" : (fx.stat == 1 ? "DEF" : "SPD");
                mi.effects.add((fx.amount > 0 ? "+" : "-") + Math.round(Math.abs(fx.amount) * 100) + "% " + (fx.onPlayer ? "YOUR " : "OWN ") + stat);
            }
            found.put(key, mi);
        }
        if (m.damages) {
            mi.minPower = Math.min(mi.minPower, m.multiplier());
            mi.maxPower = Math.max(mi.maxPower, m.multiplier());
            mi.minHits = Math.min(mi.minHits, m.hits());
            mi.maxHits = Math.max(mi.maxHits, m.hits());
        }
    }

    private static void grow(int[] r, float v) {
        int i = Math.round(v);
        r[0] = Math.min(r[0], i);
        r[1] = Math.max(r[1], i);
    }

    private static BufferedImage portraitOf(Enemy en) {
        try {
            GameObject.Frame[] anim = en.getCurrentAnimation();
            return anim == null || anim.length == 0 ? null : anim[0].getImage();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // the same picture, solid black: what an unseen enemy looks like
    private static BufferedImage silhouette(BufferedImage img) {
        if (img == null) return null;
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) > 100 && (p & 0xffffff) != 0xff00ff) out.setRGB(x, y, 0xff0a0608);
            }
        return out;
    }

    public static String range(int[] r) { return r[0] == r[1] ? String.valueOf(r[0]) : r[0] + "-" + r[1]; }

    public static String levels(Entry e) {
        if (isGlitch(e)) return "LV ???";   // GLITCH: its level is never shown
        return e.minLv == e.maxLv ? "LV " + e.minLv : "LV " + e.minLv + "-" + e.maxLv;
    }

    // GLITCH: the glitch entity's name is never shown, only animated noise
    public static boolean isGlitch(Entry e) { return NPCs.World4.GlitchEntity.ID.equals(e.name); }

    private String shownName(Entry e, boolean seen) {
        if (isGlitch(e)) return NPCs.World4.GlitchEntity.glitchName(frame, 9);
        return seen ? e.name : "???";
    }

    public static String tier(Entry e) {
        if (e.archetype == Balance.Archetype.FINAL_BOSS) return "FINAL BOSS";
        if (e.boss) return "BOSS";
        if (e.archetype == Balance.Archetype.REVENANT) return "REVENANT";   // BALANCE: World 4's returning mini-bosses
        if (e.spawnShare >= 30) return "COMMON";
        if (e.spawnShare >= 20) return "UNCOMMON";
        return "RARE";
    }

    // ================================================================== screen

    private final Runnable onClose;
    private final KeyLocker keyLocker = new KeyLocker();
    private static final Key[] KEYS = { Key.B, Key.ESC, Key.UP, Key.DOWN, Key.W, Key.S, Key.LEFT, Key.RIGHT, Key.A, Key.D };
    private int selected;
    private int frame;

    private static final Color PANEL = new Color(14, 12, 14);
    private static final Color FRAME = new Color(214, 178, 110);
    private static final Color IRON = new Color(52, 46, 44);
    private static final Color BLOOD = new Color(110, 18, 18);
    private static final Color HEAD = new Color(200, 80, 70);
    private static final Color TEXT = new Color(230, 222, 205);
    private static final Color DIM = new Color(150, 140, 125);
    private static final Color GOLD = new Color(230, 190, 60);

    public CodexOfBeastsScreen(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public void initialize() {
        frame = 0;
        entries();
        for (Key k : KEYS) keyLocker.lockKey(k);
    }

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
        update(null);
    }

    public void update(java.awt.Point click) {
        frame++;
        List<Entry> es = entries();
        if (tap(Key.B) | tap(Key.ESC)) { onClose.run(); return; }
        if (tap(Key.UP) | tap(Key.W)) { selected = (selected + es.size() - 1) % es.size(); follow = true; }
        if (tap(Key.DOWN) | tap(Key.S)) { selected = (selected + 1) % es.size(); follow = true; }
        if (tap(Key.LEFT) | tap(Key.A)) { jumpWorld(-1); follow = true; }
        if (tap(Key.RIGHT) | tap(Key.D)) { jumpWorld(1); follow = true; }
        int wheel = MouseInput.pollWheel();   // FIX: the wheel scrolls the list
        if (wheel != 0) { listScroll += wheel * ROW_H * 2; follow = false; }
        int mx = MouseInput.getX(), my = MouseInput.getY();
        if (click != null) { int hit = rowAt(click.x, click.y); if (hit >= 0) selected = hit; }
        else if (mx >= 0 && Math.abs(mx - lastMx) + Math.abs(my - lastMy) > 0) { int hit = rowAt(mx, my); if (hit >= 0) selected = hit; }
        lastMx = mx;
        lastMy = my;
    }

    private int lastMx = -1, lastMy = -1;

    // LEFT / RIGHT jump to the first enemy of the previous / next world
    private void jumpWorld(int dir) {
        List<Entry> es = entries();
        int w = es.get(selected).world + dir;
        if (w < 1) w = 4;
        if (w > 4) w = 1;
        for (int i = 0; i < es.size(); i++) if (es.get(i).world == w) { selected = i; return; }
    }

    // ---- layout of the list on the left
    private static final int LIST_X = 50, LIST_Y = 96, LIST_W = 256, ROW_H = 19, HEAD_H = 22;
    // FIX: the list scrolls inside [LIST_Y, listBottom()); rowRects() are in list space, minus listScroll on screen
    private int listScroll;
    private boolean follow = true;   // keep the selected row in view (off while the wheel is scrolling)

    private static int listBottom() { return ScreenManager.getScreenHeight() - 80; }

    private int contentHeight() {
        java.awt.Rectangle[] rs = rowRects();
        return rs.length == 0 ? 0 : rs[rs.length - 1].y + ROW_H - LIST_Y;
    }

    // clamps the scroll and, after a key press, moves it so the selected row (and its world heading) is visible
    private void fixScroll() {
        int view = listBottom() - LIST_Y;
        java.awt.Rectangle[] rs = rowRects();
        if (follow && selected < rs.length) {
            int top = rs[selected].y - HEAD_H - LIST_Y, bottom = rs[selected].y + ROW_H - LIST_Y;
            if (top < listScroll) listScroll = top;
            if (bottom > listScroll + view) listScroll = bottom - view;
        }
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, contentHeight() - view)));
    }

    private java.awt.Rectangle[] rowRects() {
        List<Entry> es = entries();
        java.awt.Rectangle[] out = new java.awt.Rectangle[es.size()];
        int y = LIST_Y, lastWorld = 0;
        for (int i = 0; i < es.size(); i++) {
            if (es.get(i).world != lastWorld) { y += HEAD_H; lastWorld = es.get(i).world; }
            out[i] = new java.awt.Rectangle(LIST_X, y, LIST_W, ROW_H - 2);
            y += ROW_H;
        }
        return out;
    }

    private int rowAt(int x, int y) {
        java.awt.Rectangle[] rs = rowRects();
        if (y < LIST_Y || y >= listBottom()) return -1;   // FIX: only the visible part of the list
        for (int i = 0; i < rs.length; i++) if (rs[i].contains(x, y + listScroll)) return i;
        return -1;
    }

    @Override
    public void draw(GraphicsHandler gh) {
        int w = ScreenManager.getScreenWidth(), h = ScreenManager.getScreenHeight();
        List<Entry> es = entries();
        gh.drawFilledRectangle(0, 0, w, h, new Color(0, 0, 0, 200));
        gh.drawFilledRectangleWithBorder(30, 30, w - 60, h - 60, PANEL, FRAME, 3);
        gh.drawRectangle(36, 36, w - 73, h - 73, BLOOD, 1);
        BattleUI.shadowText(gh, "CODEX OF BEASTS", 50, 46, 3, TEXT);
        BattleUI.textRight(gh, "SEEN " + seenCount() + "/" + es.size(), w - 50, 50, 2, BattleUI.GREY);

        // the list, grouped by world. FIX: clipped to its area and scrolled
        fixScroll();
        java.awt.Rectangle[] rs = rowRects();
        Graphics2D lg = gh.getGraphics();
        Shape oldClip = lg.getClip();
        lg.clipRect(LIST_X - 8, LIST_Y - 4, LIST_W + 16, listBottom() - LIST_Y + 4);
        int lastWorld = 0;
        for (int i = 0; i < es.size(); i++) {
            Entry e = es.get(i);
            java.awt.Rectangle r = new java.awt.Rectangle(rs[i]);
            r.y -= listScroll;
            if (e.world != lastWorld) {
                lastWorld = e.world;
                BattleUI.text(gh, WORLD_NAMES[e.world], LIST_X, r.y - 15, 1, HEAD);
            }
            boolean seen = isSeen(e), sel = i == selected;
            if (sel) gh.drawFilledRectangleWithBorder(r.x - 4, r.y - 2, r.width + 8, r.height + 2, new Color(70, 52, 24), FRAME, 1);
            String num = (e.number < 10 ? "#0" : "#") + e.number;
            BattleUI.font().draw(gh, num, r.x, r.y + 4, 1, 1, DIM);
            String nm = shownName(e, seen);   // GLITCH
            if (nm.length() > 28) nm = nm.substring(0, 27) + ".";
            BattleUI.font().draw(gh, nm, r.x + 34, r.y + 4, 1, 1, isGlitch(e) ? new Color(170, 120, 220)
                    : seen ? (e.boss ? GOLD : TEXT) : new Color(110, 104, 96));
        }
        lg.setClip(oldClip);
        drawScrollBar(gh);

        drawDetail(gh, es.get(selected));
        BattleUI.textCenter(gh, "UP/DOWN: BROWSE   LEFT/RIGHT: WORLD   B/ESC: BACK TO MAP", w / 2, h - 58, 1, BattleUI.GREY);
    }

    // FIX: a thin bar on the list's right edge showing where in the list you are
    private void drawScrollBar(GraphicsHandler gh) {
        int view = listBottom() - LIST_Y, total = contentHeight();
        if (total <= view) return;
        int bx = LIST_X + LIST_W + 10, bh = Math.max(16, view * view / total);
        int by = LIST_Y + (view - bh) * listScroll / Math.max(1, total - view);
        gh.drawFilledRectangle(bx, LIST_Y, 3, view, IRON);
        gh.drawFilledRectangle(bx, by, 3, bh, FRAME);
    }

    private void drawDetail(GraphicsHandler gh, Entry e) {
        int x = 322, y = 92, dw = ScreenManager.getScreenWidth() - 50 - x, dh = 404;
        gh.drawFilledRectangleWithBorder(x, y, dw, dh, new Color(24, 20, 20), IRON, 2);
        boolean seen = isSeen(e);
        if (isGlitch(e)) { drawGlitchDetail(gh, e, x, y, dw, dh, seen); return; }   // GLITCH

        // portrait (a black silhouette until seen), on a dark plinth with a faint red glow
        int px = x + 12, py = y + 12, ps = 104;
        Graphics2D g = gh.getGraphics();
        int pw = e.forms != null ? drawForms(gh, e, px, py, ps, seen) : ps;   // CODEX: Dabbah's three forms instead
        if (e.forms == null) {
            gh.drawFilledRectangleWithBorder(px, py, ps, ps, new Color(12, 10, 12), IRON, 2);
            int glow = 30 + (int) (20 * Math.sin(frame * 0.05));
            g.setColor(new Color(140, 20, 20, glow));
            g.fillOval(px + 12, py + 12, ps - 24, ps - 24);
            BufferedImage pic = seen ? e.portrait : e.silhouette;
            if (pic != null) {
                Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int sz = ps - 8;
                g.drawImage(pic, px + 4, py + 4, sz, sz, null);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp == null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : interp);
            }
            if (!seen) BattleUI.textCenter(gh, "?", px + ps / 2, py + ps / 2 - 14, 4, new Color(90, 80, 75));
        }

        // name, world, tier, levels
        int tx = px + pw + 14, tw = x + dw - 12 - tx;   // CODEX: pw (Dabbah's strip of forms is wider)
        int ty = py + 2;
        for (String line : BattleUI.wrap(shownName(e, seen), Math.max(1, tw / 12))) {   // GLITCH
            BattleUI.shadowText(gh, line, tx, ty, 2, seen && e.boss ? GOLD : TEXT);
            ty += 22;
        }
        BattleUI.text(gh, WORLD_NAMES[e.world], tx, ty + 2, 1, HEAD);
        ty += 18;
        String tier = isGlitch(e) ? "???  JOINS 1 IN 8 FIGHTS"   // GLITCH
                : tier(e) + (e.boss ? "" : (tier(e).equals(e.archetype.name()) ? "" : "  " + e.archetype.name())   // FIX: no "REVENANT  REVENANT"
                + "  " + Math.round(e.spawnShare) + "% OF FIGHTS");
        BattleUI.text(gh, tier, tx, ty, 1, e.boss ? GOLD : DIM);
        ty += 16;
        BattleUI.text(gh, levels(e), tx, ty, 2, TEXT);

        int cy = py + ps + 16;
        if (!seen) {
            BattleUI.text(gh, "NOT ENCOUNTERED YET.", x + 12, cy, 2, DIM);
            BattleUI.text(gh, "FIGHT IT ONCE TO LEARN ITS STATS, PASSIVE AND MOVES.", x + 12, cy + 26, 1, DIM);
            return;
        }

        // stats
        String[][] stats = { { "HP", range(e.hp) }, { "ATK", range(e.atk) }, { "DEF", range(e.def) },
                { "SPD", range(e.spd) }, { "XP", range(e.xp) }, { "GOLD", range(e.gold) } };
        int colW = (dw - 24) / 3;
        for (int i = 0; i < stats.length; i++) {
            int sx = x + 12 + (i % 3) * colW, sy = cy + (i / 3) * 20;
            BattleUI.text(gh, stats[i][0], sx, sy, 1, DIM);
            BattleUI.text(gh, stats[i][1], sx + 36, sy, 1, TEXT);
        }
        cy += 46;

        // passive
        BattleUI.text(gh, "PASSIVE", x + 12, cy, 1, HEAD);
        cy += 14;
        String passive = e.forms != null ? dabbahPassive() : e.passive;   // CODEX
        for (String line : BattleUI.wrap(passive.isEmpty() ? "NONE." : passive, (dw - 24) / 7)) {
            BattleUI.text(gh, line, x + 12, cy, 1, TEXT);
            cy += 12;
        }
        cy += 8;

        // moves
        BattleUI.text(gh, "MOVES", x + 12, cy, 1, HEAD);
        BattleUI.text(gh, "POWER", x + 12 + 160, cy, 1, DIM);
        BattleUI.text(gh, "PP", x + 12 + 222, cy, 1, DIM);   // BALANCE
        BattleUI.text(gh, "EFFECT", x + 12 + 254, cy, 1, DIM);
        cy += 14;
        int effChars = Math.max(8, (dw - 24 - 254) / 7);
        for (MoveInfo m : e.moves) {
            if (cy > y + dh - 14) break;
            BattleUI.text(gh, m.name.length() > 22 ? m.name.substring(0, 21) + "." : m.name, x + 12, cy, 1, TEXT);
            String power = m.flee || m.status ? "--" : pct(m.minPower) + (m.maxPower > m.minPower + 0.001f ? "-" + pct(m.maxPower) : "");
            BattleUI.text(gh, power, x + 12 + 160, cy, 1, TEXT);
            BattleUI.text(gh, m.pp < 0 ? "--" : String.valueOf(m.pp), x + 12 + 222, cy, 1, TEXT);   // BALANCE
            List<String> bits = new ArrayList<>();
            if (m.flee) bits.add("ENDS THE BATTLE");
            if (m.status) bits.add("STATUS");
            if (m.maxHits > 1) bits.add(m.minHits == m.maxHits ? m.maxHits + " HITS" : m.minHits + "-" + m.maxHits + " HITS");
            if (m.accuracy < 0.999f) bits.add(Math.round(m.accuracy * 100) + "% ACC");
            bits.addAll(m.effects);
            String eff = bits.isEmpty() ? "-" : String.join(", ", bits);
            for (String line : BattleUI.wrap(eff, effChars)) {
                BattleUI.text(gh, line, x + 12 + 254, cy, 1, eff.contains("YOUR") ? new Color(220, 120, 100) : DIM);
                cy += 12;
            }
            cy += 2;
        }
    }

    // CODEX: Dabbah's three forms in a row: each a panel with the form (kept in proportion), or its black silhouette and a
    // "?" until the knight has faced it; the one it was last seen in glows. Returns the strip's width.
    private int drawForms(GraphicsHandler gh, Entry e, int px, int py, int ps, boolean seen) {
        Graphics2D g = gh.getGraphics();
        int known = seen ? Math.max(1, dabbahForms) : 0, cw = 74, gap = 4;
        Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int f = 0; f < 3; f++) {
            int cx = px + f * (cw + gap);
            boolean have = f < known;
            gh.drawFilledRectangleWithBorder(cx, py, cw, ps, new Color(12, 10, 12), have && f == known - 1 ? FRAME : IRON, 2);
            int glow = 30 + (int) (20 * Math.sin(frame * 0.05 + f));
            g.setColor(f == 1 && have ? new Color(200, 90, 20, glow) : (f == 2 && have ? new Color(110, 30, 150, glow) : new Color(140, 20, 20, glow)));
            g.fillOval(cx + 8, py + 18, cw - 16, ps - 40);
            BufferedImage pic = have ? e.forms[f] : e.formShadows[f];
            if (pic != null) {   // fit inside the panel, above the numeral, keeping its proportions
                int bw = cw - 8, bh = ps - 22;
                float k = Math.min(bw / (float) pic.getWidth(), bh / (float) pic.getHeight());
                int iw = Math.max(1, Math.round(pic.getWidth() * k)), ih = Math.max(1, Math.round(pic.getHeight() * k));
                g.drawImage(pic, cx + 4 + (bw - iw) / 2, py + 4 + (bh - ih), iw, ih, null);
            }
            if (!have) BattleUI.textCenter(gh, "?", cx + cw / 2, py + ps / 2 - 22, 3, new Color(90, 80, 75));
            BattleUI.textCenter(gh, f == 0 ? "I" : (f == 1 ? "II" : "III"), cx + cw / 2, py + ps - 15, 1, have ? GOLD : DIM);
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp == null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : interp);
        return 3 * cw + 2 * gap;
    }

    // CODEX: Dabbah's passive, naming only the forms the knight has faced
    private static String dabbahPassive() {
        StringBuilder b = new StringBuilder("THREE FORMS: ");
        for (int f = 0; f < 3; f++) {
            b.append(f == 0 ? "I " : (f == 1 ? ", II " : ", III ")).append(f < Math.max(1, dabbahForms) ? DABBAH_FORMS[f] : "???");
        }
        return b.append(". EACH FORM RISES AT FULL HP WITH ITS OWN STATS (LESS HP AND DEF, MORE ATK AND SPD) AND +10% POWER. "
                + "EVERY 3RD TURN: JUDGEMENT OF DABBAH. ITS BLOWS CAN LAND CRITICAL HITS.").toString();
    }

    private static String pct(float f) { return Math.round(f * 100) + "%"; }

    // ================================================================== GLITCH: the glitch entity's page
    // Nothing on it reads cleanly: every line is corrupted (letters eaten by noise, a little different every few frames),
    // binary, or both. The real words are still there underneath, so a patient player can piece them together.

    private static final Color GLITCH_TEXT = new Color(200, 180, 230);
    private static final Color GLITCH_DIM = new Color(130, 100, 160);
    private static final Color GLITCH_RED = new Color(220, 60, 60);

    private static final java.util.Map<NPCs.World4.GlitchEntity.GlitchMove, String> GLITCH_EFFECTS = new java.util.EnumMap<>(NPCs.World4.GlitchEntity.GlitchMove.class);
    static {
        GLITCH_EFFECTS.put(NPCs.World4.GlitchEntity.GlitchMove.VEIL_OF_STATIC, "HIDES YOUR HP BAR UNDER STATIC");
        GLITCH_EFFECTS.put(NPCs.World4.GlitchEntity.GlitchMove.GRIEVANCE, "ALL YOUR STATS -50% FOR 4 TURNS. SHOWS YOU WHAT HAPPENED");
        GLITCH_EFFECTS.put(NPCs.World4.GlitchEntity.GlitchMove.SEVERANCE, "TEARS ONE OF YOUR MOVES OUT FOR THE FIGHT");
        GLITCH_EFFECTS.put(NPCs.World4.GlitchEntity.GlitchMove.BLOOD_TITHE, "ITS ALLY: ATK +999% BUT HP DROPS TO 1. STRIKE FIRST");
    }

    private String rot(String text, double amount) { return NPCs.World4.GlitchEntity.corrupt(text, frame, amount); }

    private String bin(String text) { return NPCs.World4.GlitchEntity.toBinary(text); }

    // a long string shown through a window of n characters that slowly scrolls along it
    private String marquee(String text, int n, int offset) {
        if (text.length() <= n) return text;
        String loop = text + "   " + text;
        int start = (frame / 5 + offset) % (text.length() + 3);
        return loop.substring(start, start + n);
    }

    private void drawGlitchDetail(GraphicsHandler gh, Entry e, int x, int y, int dw, int dh, boolean seen) {
        // portrait: a box of static with a skull surfacing now and then
        int px = x + 12, py = y + 12, ps = 104;
        gh.drawFilledRectangleWithBorder(px, py, ps, ps, new Color(12, 10, 12), new Color(90, 30, 138), 2);
        Random r = new Random(frame / 4 * 53L);
        Color[] noise = { new Color(10, 6, 16), new Color(42, 15, 61), new Color(90, 30, 138), new Color(138, 16, 32), new Color(20, 12, 28) };
        for (int yy = 4; yy < ps - 4; yy += 4)
            for (int xx = 4; xx < ps - 4; xx += 4) gh.drawFilledRectangle(px + xx, py + yy, 4, 4, noise[r.nextInt(noise.length)]);
        if (r.nextInt(3) > 0) {
            int sx = px + 20 + r.nextInt(ps - 50), sy = py + 20 + r.nextInt(ps - 50);
            String[] skull = { ".#####.", "#######", "#..#..#", "#######", ".##.##.", "..#.#.." };
            for (int row = 0; row < skull.length; row++)
                for (int col = 0; col < 7; col++)
                    if (skull[row].charAt(col) == '#') gh.drawFilledRectangle(sx + col * 3, sy + row * 3, 3, 3, new Color(230, 220, 236));
        }

        // name, world, tier, level
        int tx = px + ps + 14, ty = py + 2;
        BattleUI.shadowText(gh, NPCs.World4.GlitchEntity.glitchName(frame, 9), tx, ty, 2, GLITCH_TEXT);
        ty += 24;
        BattleUI.text(gh, rot(WORLD_NAMES[4], 0.3), tx, ty, 1, GLITCH_RED);
        ty += 16;
        BattleUI.text(gh, "???  " + rot("JOINS 1 IN 8 FIGHTS", 0.35), tx, ty, 1, GLITCH_DIM);
        ty += 16;
        BattleUI.text(gh, rot("LV ???", 0.2), tx, ty, 2, GLITCH_TEXT);

        int cy = py + ps + 16;
        if (!seen) {
            BattleUI.text(gh, rot("NOT ENCOUNTERED YET.", 0.4), x + 12, cy, 2, GLITCH_DIM);
            BattleUI.text(gh, marquee(bin("IT HAS SEEN YOU"), (dw - 24) / 7, 0), x + 12, cy + 26, 1, GLITCH_DIM);
            return;
        }

        // stats: labels half-eaten, numbers replaced by noise of the same length
        String[][] stats = { { "HP", range(e.hp) }, { "ATK", range(e.atk) }, { "DEF", range(e.def) },
                { "SPD", range(e.spd) }, { "XP", range(e.xp) }, { "GOLD", range(e.gold) } };
        int colW = (dw - 24) / 3;
        for (int i = 0; i < stats.length; i++) {
            int sx = x + 12 + (i % 3) * colW, sy = cy + (i / 3) * 20;
            BattleUI.text(gh, rot(stats[i][0], 0.3), sx, sy, 1, GLITCH_DIM);
            BattleUI.text(gh, NPCs.World4.GlitchEntity.glitchName(frame + i * 9, stats[i][1].length()), sx + 36, sy, 1, GLITCH_TEXT);
        }
        cy += 46;

        // passive: a line of binary (what it is), then the real passive with letters eaten out of it
        int chars = (dw - 24) / 7;
        BattleUI.text(gh, rot("PASSIVE", 0.3), x + 12, cy, 1, GLITCH_RED);
        cy += 14;
        BattleUI.text(gh, marquee(bin("IT WATCHES"), chars, 0), x + 12, cy, 1, GLITCH_DIM);
        cy += 12;
        for (String line : BattleUI.wrap(e.passive, chars)) {
            BattleUI.text(gh, rot(line, 0.3), x + 12, cy, 1, GLITCH_TEXT);
            cy += 12;
        }
        cy += 8;

        // moves: the binary name (it spells the real name), then what the move does, half-eaten
        BattleUI.text(gh, rot("MOVES", 0.3), x + 12, cy, 1, GLITCH_RED);
        BattleUI.text(gh, rot("EFFECT", 0.3), x + 12 + 200, cy, 1, GLITCH_DIM);
        cy += 14;
        int effChars = Math.max(8, (dw - 24 - 200) / 7);
        int k = 0;
        for (NPCs.World4.GlitchEntity.GlitchMove m : NPCs.World4.GlitchEntity.GlitchMove.values()) {
            if (cy > y + dh - 26) break;
            BattleUI.text(gh, marquee(m.binary(), 26, k * 11), x + 12, cy, 1, GLITCH_TEXT);
            // underneath the binary: its translation, mostly eaten
            BattleUI.text(gh, "= " + rot(m.realName, 0.45), x + 12, cy + 12, 1, GLITCH_DIM);
            int ey = cy;
            for (String line : BattleUI.wrap(GLITCH_EFFECTS.get(m), effChars)) {
                BattleUI.text(gh, rot(line, 0.3), x + 12 + 200, ey, 1, line.contains("YOUR") ? new Color(220, 120, 100) : GLITCH_DIM);
                ey += 12;
            }
            cy = Math.max(cy + 26, ey) + 4;
            k++;
        }
    }
}