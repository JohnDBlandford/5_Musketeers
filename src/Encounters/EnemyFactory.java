package Encounters;

import BattleSystem.Balance;
import BattleSystem.Balance.Archetype;
import NPCs.Enemy;
import NPCs.World1.*;
import NPCs.World2.*;
import NPCs.World3.*;
import NPCs.World4.*;
import BattleSystem.BattleWorld;
import Utils.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

// Builds your real enemy classes for the battle. Random encounters pick by each enemy's spawn rate in the current world
// (the spawnRate numbers from the enemy classes: World 1 = Wretch 35, Brute 25, Ash Wolf 20, Zealot 15, and so on).
//
//   EnemyFactory.randomEncounter("world1", rng, playerLevel)   a random enemy (and whether it is a boss) for that map
//   EnemyFactory.boss("famine warden")     a boss, for scripts:   startBattle(EnemyFactory.boss("dabbah"), 1, true)
//
// BALANCE CHANGES (search for "BALANCE:"):
//   - every enemy now has an ARCHETYPE (skirmisher / brute / beast / elite / mini-boss / final boss) and a WORLD
//   - after it is built, Balance.scale() sets its level and numbers: random enemies spawn near the knight's level,
//     clamped to the world's band (World 1: 1-12, World 2: 12-30, World 3: 30-50); mini-bosses at the top of the band;
//     Dabbah at 70. The class still decides its moves, passives and phases.
//   - mini-bosses only appear at random once the knight is within 3 levels of the band's top (Balance.bossCanSpawn)
//   - roster() lists every enemy with its world and archetype (the bestiary reads it)
//   - WORLD 4: the earlier mini-bosses and tanks (NPCs.World4) are World 4's random enemies, levels 50-70
//   - GLITCH: 1 World 4 fight in 8 adds the glitch entity as a second enemy (Encounter.companion); glitch(level) builds it
public final class EnemyFactory {
    private EnemyFactory() { }

    // One enemy that can appear. Its weight is the enemy class's OWN spawnRate (read from a sample of the class the
    // first time it is needed), so changing a spawn rate in an enemy file changes the encounters automatically.
    public static final class Spec {
        public final int world;                 // 1-3, 4 = Dabbah's Hollow (the final visit to World 1)
        public final Archetype archetype;       // BALANCE: decides its numbers
        public final Supplier<Enemy> make;
        private float weight = -1;

        Spec(int world, Archetype archetype, Supplier<Enemy> make) {
            this.world = world;
            this.archetype = archetype;
            this.make = make;
        }

        public boolean boss() { return archetype.isBoss(); }

        float weight() {
            if (weight < 0) weight = Math.max(0f, make.get().getSpawnRate());
            return weight;
        }
    }

    // what a random encounter turns out to be
    public static final class Encounter {
        public final Enemy enemy;
        public final boolean boss;   // bosses can't be run away from
        public final Enemy companion;   // GLITCH: the second enemy of a double battle, or null
        Encounter(Enemy enemy, boolean boss) { this(enemy, boss, null); }
        Encounter(Enemy enemy, boolean boss, Enemy companion) { this.enemy = enemy; this.boss = boss; this.companion = companion; }
    }

    // GLITCH: the glitch entity at this level (the level of the enemy it joins)
    public static GlitchEntity glitch(int level) {
        return Balance.scale(new GlitchEntity(0, origin()), Archetype.GLITCH, level);
    }

    // GLITCH: TEST ONLY (the T key in PlayLevelScreen). A World 4 enemy at the knight's level, always with the glitch.
    public static Encounter testDoubleBattle(int playerLevel, Random rng) {
        List<Spec> pool = table(4);
        Spec pick = pool.get(rng.nextInt(pool.size()));
        int level = Math.max(1, Math.min(Balance.MAX_LEVEL, playerLevel));
        return new Encounter(Balance.scale(pick.make.get(), pick.archetype, level), pick.boss(), glitch(level));
    }

    private static Point origin() { return new Point(0, 0); }

    // BALANCE: archetypes follow the design doc (chapter 3). World 3's classes don't match the doc's names, so they are
    // matched by their stat shape: Sentinel is the tank (brute), Husk the fast low-DEF one (skirmisher), Gazer the glass cannon (beast).
    private static final List<Spec> ROSTER = List.of(
            new Spec(1, Archetype.SKIRMISHER, () -> new StarvingWretch(0, origin())),
            new Spec(1, Archetype.BRUTE, () -> new ReckonerBrute(0, origin())),
            new Spec(1, Archetype.BEAST, () -> new AshWolf(0, origin())),
            new Spec(1, Archetype.ELITE, () -> new HollowChorusZealot(0, origin())),
            new Spec(1, Archetype.MINI_BOSS, () -> new TheFamineWarden(0, origin())),
            new Spec(2, Archetype.SKIRMISHER, () -> new RaiderSkirmisher(0, origin())),
            new Spec(2, Archetype.BRUTE, () -> new MilitiaBrute(0, origin())),
            new Spec(2, Archetype.BEAST, () -> new BlightedBoar(0, origin())),
            new Spec(2, Archetype.ELITE, () -> new BaronsVanguard(0, origin())),
            new Spec(2, Archetype.MINI_BOSS, () -> new BaronOfTheClearedWood(0, origin())),
            new Spec(3, Archetype.SKIRMISHER, () -> new AbyssalHusk(0, origin())),
            new Spec(3, Archetype.BRUTE, () -> new CorruptedSentinel(0, origin())),
            new Spec(3, Archetype.BEAST, () -> new GazerBeast(0, origin())),
            new Spec(3, Archetype.ELITE, () -> new EldritchOverseer(0, origin())),
            new Spec(3, Archetype.MINI_BOSS, () -> new TheWatcherInTheAsh(0, origin())),
            // World 4 (Dabbah's Hollow), levels 50-70: the three earlier mini-bosses as regular, runnable enemies (REVENANT)
            // and the three earlier tanks (BRUTE). Their classes are in NPCs.World4; spawn shares 20/20/20 and 14/13/13.
            new Spec(4, Archetype.REVENANT, () -> new RisenFamineWarden(0, origin())),
            new Spec(4, Archetype.REVENANT, () -> new RisenBaron(0, origin())),
            new Spec(4, Archetype.REVENANT, () -> new RisenWatcher(0, origin())),
            new Spec(4, Archetype.BRUTE, () -> new RisenReckonerBrute(0, origin())),
            new Spec(4, Archetype.BRUTE, () -> new RisenMilitiaBrute(0, origin())),
            new Spec(4, Archetype.BRUTE, () -> new RisenSentinel(0, origin())),
            // the glitch entity: never a normal spawn; joins 1 World 4 fight in 8 as a second enemy (see randomEncounter)
            new Spec(4, Archetype.GLITCH, () -> new GlitchEntity(0, origin())),
            // Dabbah is never a random encounter: only EnemyFactory.boss("dabbah").
            new Spec(4, Archetype.FINAL_BOSS, () -> new Dabbah(0, origin())));

    public static List<Spec> roster() { return ROSTER; }

    private static List<Spec> table(int world) {
        List<Spec> out = new ArrayList<>();
        for (Spec s : ROSTER)   // Dabbah and the glitch entity never spawn on their own
            if (s.world == world && s.archetype != Archetype.FINAL_BOSS && s.archetype != Archetype.GLITCH) out.add(s);
        return out;
    }

    // a random encounter for this world, picked by spawn rate, at a level near the knight's (null = no random encounters here)
    public static Encounter randomEncounter(BattleWorld world, Random rng, int playerLevel) {
        int w = Balance.worldOf(world);
        List<Spec> pool = new ArrayList<>();
        for (Spec s : table(w)) if (!s.boss() || Balance.bossCanSpawn(w, playerLevel)) pool.add(s);   // BALANCE
        if (pool.isEmpty()) return null;
        float total = 0;
        for (Spec s : pool) total += s.weight();
        float roll = rng.nextFloat() * total;
        Spec pick = pool.get(0);
        for (Spec s : pool) {
            roll -= s.weight();
            if (roll < 0) { pick = s; break; }
        }
        int level = Balance.spawnLevel(w, pick.archetype, playerLevel, rng);   // BALANCE
        // GLITCH: 1 World 4 fight in 8 gets the glitch entity as a second enemy, at the same level
        Enemy companion = w == 4 && !pick.boss() && rng.nextDouble() < Balance.GLITCH_CHANCE ? glitch(level) : null;
        return new Encounter(Balance.scale(pick.make.get(), pick.archetype, level), pick.boss(), companion);
    }

    public static Encounter randomEncounter(String mapName, Random rng, int playerLevel) {
        return randomEncounter(BattleWorld.fromMapName(mapName), rng, playerLevel);
    }

    // old signatures (no knight level given): enemies spawn at the bottom of the world's band
    public static Encounter randomEncounter(BattleWorld world, Random rng) {
        return randomEncounter(world, rng, Balance.band(Balance.worldOf(world))[0]);
    }

    public static Encounter randomEncounter(String mapName, Random rng) {
        return randomEncounter(BattleWorld.fromMapName(mapName), rng);
    }

    // bosses (name matched loosely: "famine warden", "baron", "watcher", "dabbah"); null if unknown.
    // BALANCE: each comes back scaled to its level (top of its band; Dabbah 70).
    public static Enemy boss(String name) {
        String n = name.toLowerCase();
        int world = n.contains("famine") ? 1 : n.contains("baron") ? 2 : n.contains("watcher") ? 3 : n.contains("dabbah") ? 4 : 0;
        if (world == 0) return null;
        for (Spec s : ROSTER) {
            if (s.world == world && s.boss()) {   // (World 4's revenants aren't bosses, so "dabbah" finds Dabbah)
                return Balance.scale(s.make.get(), s.archetype, Balance.spawnLevel(world, s.archetype, 1, new Random()));
            }
        }
        return null;
    }
}