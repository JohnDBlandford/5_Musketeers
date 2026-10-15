package BattleSystem;

import Engine.Key;
import Engine.Keyboard;

import java.util.EnumSet;

// One place that turns "key is held down" into "key was tapped this frame", so holding ENTER
// never skips through several menus. PlayBattleScreen calls update() once per frame; every other battle screen just asks tap().
public final class BattleInput {
    private BattleInput() {}

    private static final EnumSet<Key> held = EnumSet.noneOf(Key.class);
    private static final EnumSet<Key> tapped = EnumSet.noneOf(Key.class);

    // called when a battle starts: anything already held down (like the ENTER that started the fight) does not count as a tap
    public static void reset() {
        tapped.clear();
        held.clear();
        for (Key k : Key.values()) if (Keyboard.isKeyDown(k)) held.add(k);
    }

    public static void update() {
        tapped.clear();
        for (Key k : Key.values()) {
            boolean down = Keyboard.isKeyDown(k);
            if (down && !held.contains(k)) tapped.add(k);
            if (down) held.add(k); else held.remove(k);
        }
    }

    public static boolean tap(Key k) { return tapped.contains(k); }

    public static boolean confirm() { return tap(Key.ENTER) || tap(Key.SPACE); }
    public static boolean back() { return tap(Key.B) || tap(Key.X); }
    public static boolean info() { return tap(Key.Y); }
    public static boolean up() { return tap(Key.UP) || tap(Key.W); }
    public static boolean down() { return tap(Key.DOWN) || tap(Key.S); }
    public static boolean left() { return tap(Key.LEFT) || tap(Key.A); }
    public static boolean right() { return tap(Key.RIGHT) || tap(Key.D); }

    // keys 1-4 jump straight to that ability slot; returns 0-3, or -1 if none was tapped
    public static int number() {
        if (tap(Key.ONE)) return 0;
        if (tap(Key.TWO)) return 1;
        if (tap(Key.THREE)) return 2;
        if (tap(Key.FOUR)) return 3;
        return -1;
    }
}