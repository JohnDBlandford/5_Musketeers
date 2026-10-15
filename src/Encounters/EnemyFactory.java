package Encounters;

import NPCs.Enemy;
import NPCs.World1.*;
import NPCs.World2.*;
import NPCs.World3.*;
import BattleSystem.BattleWorld;
import Utils.Point;

import java.util.Random;
import java.util.function.Supplier;

// Builds your real enemy classes for the battle. Random encounters pick by each enemy's spawn rate in the current world
// (the spawnRate numbers from the enemy classes: World 1 = Wretch 35, Brute 25, Ash Wolf 20, Zealot 15, and so on).
// Each class picks its own random level in its constructor, exactly as you wrote it.
//
//   EnemyFactory.randomEncounter("world1", rng)   a random enemy (and whether it is a boss) for that map
//   EnemyFactory.boss("famine warden")     a boss, for scripts:   startBattle(EnemyFactory.boss("dabbah"), 1, true)
public final class EnemyFactory {
    private EnemyFactory() { }

    // One enemy that can appear in a world. Its weight is the enemy class's OWN spawnRate (read from a sample of the class the
    // first time it is needed), so changing a spawn rate in an enemy file changes the encounters automatically.
    private static final class Entry {
        final Supplier<Enemy> make;
        final boolean boss;
        float weight = -1;
        Entry(boolean boss, Supplier<Enemy> make) { this.boss = boss; this.make = make; }
        float weight() {
            if (weight < 0) weight = Math.max(0f, make.get().getSpawnRate());
            return weight;
        }
    }

    // what a random encounter turns out to be
    public static final class Encounter {
        public final Enemy enemy;
        public final boolean boss;   // bosses can't be run away from
        Encounter(Enemy enemy, boolean boss) { this.enemy = enemy; this.boss = boss; }
    }

    private static Point origin() { return new Point(0, 0); }

    // World 1: Starving Wretch, Reckoner Brute, Ash Wolf, Hollow Chorus Zealot and (rarely) The Famine Warden.
    // Dabbah is not here: he belongs to World 4, which will have its own enemies.
    private static final Entry[] WORLD1 = {
            new Entry(false, () -> new StarvingWretch(0, origin())),
            new Entry(false, () -> new ReckonerBrute(0, origin())),
            new Entry(false, () -> new AshWolf(0, origin())),
            new Entry(false, () -> new HollowChorusZealot(0, origin())),
            new Entry(true,  () -> new TheFamineWarden(0, origin())),
    };
    // World 2: Raider Skirmisher, Militia Brute, Blighted Boar, Baron's Vanguard and (rarely) Baron of the Cleared Wood
    private static final Entry[] WORLD2 = {
            new Entry(false, () -> new RaiderSkirmisher(0, origin())),
            new Entry(false, () -> new MilitiaBrute(0, origin())),
            new Entry(false, () -> new BlightedBoar(0, origin())),
            new Entry(false, () -> new BaronsVanguard(0, origin())),
            new Entry(true,  () -> new BaronOfTheClearedWood(0, origin())),
    };
    // World 3: Corrupted Sentinel, Gazer Beast, Abyssal Husk, Eldritch Overseer and (rarely) The Watcher in the Ash
    private static final Entry[] WORLD3 = {
            new Entry(false, () -> new CorruptedSentinel(0, origin())),
            new Entry(false, () -> new GazerBeast(0, origin())),
            new Entry(false, () -> new AbyssalHusk(0, origin())),
            new Entry(false, () -> new EldritchOverseer(0, origin())),
            new Entry(true,  () -> new TheWatcherInTheAsh(0, origin())),
    };

    // a random encounter for this world, picked by spawn rate (null = this world has no random encounters yet, e.g. World 4)
    public static Encounter randomEncounter(BattleWorld world, Random rng) {
        Entry[] table = world == BattleWorld.WORLD_1 ? WORLD1 : (world == BattleWorld.WORLD_2 ? WORLD2 : (world == BattleWorld.WORLD_3 ? WORLD3 : null));
        if (table == null) return null;
        float total = 0;
        for (Entry e : table) total += e.weight();
        float roll = rng.nextFloat() * total;
        for (Entry e : table) {
            roll -= e.weight();
            if (roll < 0) return new Encounter(e.make.get(), e.boss);
        }
        return new Encounter(table[0].make.get(), table[0].boss);
    }

    public static Encounter randomEncounter(String mapName, Random rng) {
        return randomEncounter(BattleWorld.fromMapName(mapName), rng);
    }

    // bosses (name matched loosely: "famine warden", "baron", "watcher", "dabbah"); null if unknown
    public static Enemy boss(String name) {
        String n = name.toLowerCase();
        if (n.contains("famine")) return new TheFamineWarden(0, origin());
        if (n.contains("baron")) return new BaronOfTheClearedWood(0, origin());
        if (n.contains("watcher")) return new TheWatcherInTheAsh(0, origin());
        if (n.contains("dabbah")) return new Dabbah(0, origin());
        return null;
    }
}