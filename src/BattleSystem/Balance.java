package BattleSystem;

import Item.Item;
import NPCs.Enemy;

import java.util.Random;

// =====================================================================================================================
// BALANCE: every number that decides how strong things are, in ONE file. Search the project for "BALANCE:" to find every
// line of code that reads from this file.
//
// The math is Pokemon's own (Gen 3+ stat formula, the main-series damage formula, the Medium Fast XP curve and the Gen 5
// XP-gain formula). The only difference is the size of the numbers: HP and damage are x HP_SCALE (10) and the other stats
// x STAT_SCALE (2), so a level 70 Dabbah has ~2,700 HP per phase instead of ~270. Because damage is scaled by the same
// x10 as HP, every fight lasts exactly as many hits as the same fight in Pokemon would.
//
//   stat(L) = (floor((2 x base + IV) x L / 100) + 5) x STAT_SCALE                  (ATK, DEF, SPD)
//   HP(L)   = (floor((2 x base + IV) x L / 100) + L + 10) x HP_SCALE
//   damage  = (floor(floor((2L/5 + 2) x Power x A / D) / 50) + 2) x HP_SCALE x random(0.85-1.00) x crit
//             Power = the move's power (100% = BASE_POWER = 70, like a typical Pokemon move); A/D = attacker ATK / target DEF
//   XP to go from L to L+1 = (L+1)^3 - L^3                                           (Medium Fast: level 100 = 1,000,000 XP)
//   XP gained = floor((yield x Le / 5) x ((2Le + 10) / (Le + Lp + 10))^2.5 + 1) x 1.5   (Gen 5; Le = enemy level, Lp = yours)
//             so beating something below your level gives less, above your level more
//
// Level bands (agreed): World 1 opening 1-12, World 2 12-30, World 3 30-50, World 4 (Dabbah's Hollow, inside World 1) 50-70,
// Dabbah 70.
// The knight's cap is 100: levels above 70 are for over-levelling and post-game.
// =====================================================================================================================
public final class Balance {
    private Balance() {}

    // ------------------------------------------------------------------------------------------------- the Pokemon formulas

    public static final int HP_SCALE = 10;     // HP and damage are 10x Pokemon's
    public static final int STAT_SCALE = 2;    // ATK / DEF / SPD are 2x Pokemon's (only the ratio A/D matters for damage)
    public static final int IV = 15;           // every combatant uses an average IV (Pokemon: 0-31), so nothing is random
    public static final int BASE_POWER = 70;   // a 100% move has Pokemon power 70 (HEAVY CLEAVE 180% = 126, VOLLEY 55% = 38.5 x3)

    public static float stat(int base, int level) {
        return (float) ((Math.floor((2.0 * base + IV) * level / 100.0) + 5) * STAT_SCALE);
    }

    public static float hpStat(int base, int level) {
        return (float) ((Math.floor((2.0 * base + IV) * level / 100.0) + level + 10) * HP_SCALE);
    }

    // Pokemon's damage formula before the random roll and crit. power = the move's multiplier (1.0 = 100% = BASE_POWER).
    public static float damage(int attackerLevel, double power, float atk, float def) {
        int level = Math.max(1, attackerLevel);
        double movePower = power * BASE_POWER;
        double d = Math.floor(Math.floor((Math.floor(2.0 * level / 5) + 2) * movePower * Math.max(1f, atk) / Math.max(1f, def)) / 50) + 2;
        return (float) (d * HP_SCALE);
    }

    // Pokemon's random roll: 85-100%
    public static float roll(Random rng) { return (85 + rng.nextInt(16)) / 100f; }

    // ------------------------------------------------------------------------------------------------- levels and XP

    public static final int MAX_LEVEL = 100;   // the knight's cap
    public static final int DABBAH_LEVEL = 70;

    // BALANCE: Dabbah's three forms each have their own stats (design doc 3.6). Its HP and DEF fall while its ATK and SPD
    // rise: a reluctant witness becoming a fully committed judge, ending as a glass cannon. These multiply its level-70
    // FINAL_BOSS stats; the ratios are the doc's (Phase 2 = 2200/2600 HP, 245/210 ATK, ...). Index 0 = form 1.
    //   form                         HP     ATK   DEF   SPD    (at level 70)
    //   1  Beast of the Earth        2,690  450   422   282
    //   2  Scourge of the Ash        2,276  525   343   360
    //   3  The Unbound Ruin          1,862  621   277   454
    public static final float[] DABBAH_FORM_HP  = { 1.0f, 2200f / 2600f, 1800f / 2600f };
    public static final float[] DABBAH_FORM_ATK = { 1.0f, 245f / 210f, 290f / 210f };
    public static final float[] DABBAH_FORM_DEF = { 1.0f, 130f / 160f, 105f / 160f };
    public static final float[] DABBAH_FORM_SPD = { 1.0f, 115f / 90f, 145f / 90f };

    // Medium Fast: XP to go from L to L+1 = (L+1)^3 - L^3. Level 1 needs 7, level 10 needs 331, level 50 needs 7,651.
    public static long xpToNextLevel(int level) {
        if (level >= MAX_LEVEL) return 0;
        return (long) (level + 1) * (level + 1) * (level + 1) - (long) level * level * level;
    }

    // Gen 5 XP gain. yield = the enemy's base experience (see Archetype). x1.5 = Pokemon's trainer-battle bonus.
    public static final double BATTLE_XP_BONUS = 1.5;

    public static int expGain(int yield, int enemyLevel, int playerLevel) {
        double ratio = (2.0 * enemyLevel + 10) / (enemyLevel + playerLevel + 10.0);
        return (int) Math.round(Math.floor((yield * enemyLevel / 5.0) * Math.pow(ratio, 2.5) + 1) * BATTLE_XP_BONUS);
    }

    // ------------------------------------------------------------------------------------------------- the knight

    // The knight's base stats (like a Pokemon species). HP 80 / ATK 80 / DEF 75 / SPD 75.
    // Level 1: 120 HP, 12 ATK / DEF / SPD.  Level 70: ~2,080 HP.  Level 100: ~2,940 HP.
    public static final int KNIGHT_HP = 80, KNIGHT_ATK = 80, KNIGHT_DEF = 75, KNIGHT_SPD = 75;

    public static float playerMaxHp(int level) { return hpStat(KNIGHT_HP, level); }
    public static float playerAtk(int level) { return stat(KNIGHT_ATK, level); }
    public static float playerDef(int level) { return stat(KNIGHT_DEF, level); }
    public static float playerSpd(int level) { return stat(KNIGHT_SPD, level); }

    // ------------------------------------------------------------------------------------------------- world level bands

    // { lowest, highest } enemy level of each world. Index = world number (1-4).
    private static final int[][] BANDS = { null, { 1, 12 }, { 12, 30 }, { 30, 50 }, { 50, 70 } };   // World 4 was 50-68

    public static int[] band(int world) { return BANDS[Math.max(1, Math.min(4, world))].clone(); }

    // the world number of a map name ("world1", "world2_map", ...). World 4 is the final visit to World 1: give that map
    // a name containing "world4" (same rule as BattleWorld.fromMapName).
    public static int worldOf(String mapName) {
        if (mapName == null) return 1;
        String n = mapName.toLowerCase();
        if (n.contains("world4")) return 4;
        if (n.contains("world3")) return 3;
        if (n.contains("world2")) return 2;
        return 1;
    }

    public static int worldOf(BattleWorld w) {
        if (w == BattleWorld.WORLD_2) return 2;
        if (w == BattleWorld.WORLD_3) return 3;
        if (w == BattleWorld.WORLD_4) return 4;
        return 1;
    }

    // ------------------------------------------------------------------------------------------------- archetypes

    // Base stats per archetype (like a Pokemon species), its XP yield, and its spawn level offset from the knight's level.
    // Tuned so, at equal level with gear for that point of the game:
    //   skirmisher: you win in ~2.5 hits, it needs ~5.5     brute: ~5 / ~5.5     beast: ~2.3 / ~5     elite: ~4 / ~5
    //   mini-boss: ~8 / ~4.5 (you have 2 heals)              Dabbah: ~5.3 / ~4 per phase, 3 phases, +10% power each phase
    public enum Archetype {
        //          HP   ATK  DEF  SPD  yield  minOff maxOff
        SKIRMISHER(80, 85, 75, 95, 90, -1, 0),       // fast, aggressive, low DEF, the most common
        BRUTE(125, 90, 125, 45, 110, 0, 1),          // tanky melee, slow
        BEAST(85, 110, 65, 105, 110, 0, 1),          // corrupted wildlife: fastest, hits hardest, fragile
        ELITE(115, 110, 110, 85, 160, 1, 2),         // rare, balanced, strongest standard enemy
        MINI_BOSS(255, 115, 120, 80, 300, 0, 0),     // always at the TOP of its world's band
        REVENANT(160, 115, 115, 85, 220, -1, 1),
        GLITCH(200, 1, 150, 130, 450, 0, 0),         // World 4's watcher (NPCs.World4.GlitchEntity): bulky, fast, does no damage     // World 4: the earlier mini-bosses (NPCs.World4.Risen...), back as regular, runnable enemies
        FINAL_BOSS(128, 150, 140, 90, 600, 0, 0);    // Dabbah, level 70: form 1's stats (2,690 HP); forms 2-3 use DABBAH_FORM_*

        public final int hp, atk, def, spd, yield, minOff, maxOff;

        Archetype(int hp, int atk, int def, int spd, int yield, int minOff, int maxOff) {
            this.hp = hp;
            this.atk = atk;
            this.def = def;
            this.spd = spd;
            this.yield = yield;
            this.minOff = minOff;
            this.maxOff = maxOff;
        }

        public boolean isBoss() { return this == MINI_BOSS || this == FINAL_BOSS; }
    }

    // the level a random enemy of this archetype spawns at, for a knight of playerLevel in this world
    public static int spawnLevel(int world, Archetype a, int playerLevel, Random rng) {
        if (a == Archetype.FINAL_BOSS) return DABBAH_LEVEL;
        int[] b = band(world);
        if (a == Archetype.MINI_BOSS) return b[1];
        int off = a.minOff + rng.nextInt(a.maxOff - a.minOff + 1);
        return Math.max(b[0], Math.min(b[1], playerLevel + off));
    }

    // mini-bosses are 1% random encounters that can't be run from. They only appear once the knight is this close to the
    // top of the band, so a level 2 knight never meets a level 12 boss.
    public static final int BOSS_UNLOCK_GAP = 3;

    public static boolean bossCanSpawn(int world, int playerLevel) {
        return playerLevel >= band(world)[1] - BOSS_UNLOCK_GAP;
    }

    // ------------------------------------------------------------------------------------------------- enemy stats

    public static float enemyHp(int level, Archetype a) { return hpStat(a.hp, level); }
    public static float enemyAtk(int level, Archetype a) { return stat(a.atk, level); }
    public static float enemyDef(int level, Archetype a) { return stat(a.def, level); }
    public static float enemySpd(int level, Archetype a) { return stat(a.spd, level); }

    // XP for beating it when you are the SAME level (the Codex of Beasts shows this; the battle uses expGain with your real level)
    public static int enemyXp(int level, Archetype a) { return expGain(a.yield, level, level); }

    public static int enemyGold(int level, Archetype a) { return (int) Math.round((10 + 3.0 * level) * a.yield / 110.0); }

    // rewrites an enemy's stats for this level (its own class picked its moves; this picks its numbers).
    // The enemy's xpReward holds its XP YIELD; the battle turns it into XP with expGain (needs the knight's level).
    public static <E extends Enemy> E scale(E enemy, Archetype a, int level) {
        enemy.applyScaling(level, enemyHp(level, a), enemyAtk(level, a), enemyDef(level, a), enemySpd(level, a),
                a.yield, enemyGold(level, a));
        return enemy;
    }

    // ------------------------------------------------------------------------------------------------- PP

    // PP = how many times a move can be used in ONE battle (everything refills when the battle ends).
    // Stronger moves get fewer uses. -1 = unlimited (the bare-handed PUNCH / THROW ROCK / BRACE and STRUGGLE).
    public static int maxPP(Ability a) {
        if (a == null || a.getKey() == Item.ItemAbilities.NONE) return -1;
        if (a.getEffect() == Ability.Effect.GUARD || a.getEffect() == Ability.Effect.COUNTER) return 10;
        return ppForPower(a.getPower());
    }

    // enemies use the same tiers by move power (status moves 10); bosses get double so their signature move lasts
    public static int enemyMaxPP(EnemyMoves.Move m, boolean boss) {
        int pp = m.damages ? ppForPower(m.multiplier()) : 10;
        return boss ? pp * 2 : pp;
    }

    private static int ppForPower(double power) {
        if (power >= RECHARGE_POWER) return 5;
        if (power >= 1.2) return 10;
        if (power >= 0.8) return 15;
        return 20;
    }

    // ------------------------------------------------------------------------------------------------- recharge

    // A knight move this strong (HEAVY CLEAVE, 180%) makes him recharge: on his next turn MELEE and RANGED are greyed
    // out ("KNIGHT MUST RECHARGE!"); SHIELD moves, HEAL and RUN still work. Enemies don't recharge.
    public static final double RECHARGE_POWER = 1.6;

    public static boolean recharges(Ability a) {
        return a != null && a.getKey() != Item.ItemAbilities.NONE && a.getPower() >= RECHARGE_POWER;
    }

    // used when every move of a category is out of PP: weak, unlimited, and it hurts the knight for RECOIL of the damage
    public static final Ability STRUGGLE = new Ability(Item.ItemAbilities.NONE, "STRUGGLE", MoveCategory.MELEE, 0.5, 1.0,
            Ability.Effect.STRIKE, "Out of PP. A desperate swing that hurts you too.");
    public static final double STRUGGLE_RECOIL = 0.25;

    // an enemy whose move is out of PP uses this instead
    public static final String ENEMY_FALLBACK_NAME = "Desperate Strike";
    public static final float ENEMY_FALLBACK_POWER = 0.7f;

    // ------------------------------------------------------------------------------------------------- World 4

    // World 4 (Dabbah's Hollow) is a part of the World 1 map, not its own map. Random fights inside this tile rectangle of
    // "world1" use World 4's enemies, level band and loot. { x1, y1, x2, y2 } in tiles: the area around the Hollow's gate
    // (gate tiles at x 72-77, y 5-8). Change it to the real area once the Hollow is built.
    public static final int[] WORLD4_ZONE = { 58, 0, 92, 14 };

    // chance that the glitch entity joins a World 4 fight as a second enemy (a double battle)
    public static final double GLITCH_CHANCE = 1.0 / 8;

    // GRIEVANCE (a glitch move): every knight stat x GRIEF_FACTOR for GRIEF_TURNS rounds; the glitch can use it again
    // GRIEF_COOLDOWN of its turns after using it
    public static final float GRIEF_FACTOR = 0.5f;
    public static final int GRIEF_TURNS = 4, GRIEF_COOLDOWN = 4;

    public static boolean inWorld4(String mapName, float px, float py, int tilePx) {
        if (mapName == null || !mapName.toLowerCase().contains("world1")) return mapName != null && mapName.toLowerCase().contains("world4");
        int tx = (int) (px / tilePx), ty = (int) (py / tilePx);
        return tx >= WORLD4_ZONE[0] && tx <= WORLD4_ZONE[2] && ty >= WORLD4_ZONE[1] && ty <= WORLD4_ZONE[3];
    }

    // ------------------------------------------------------------------------------------------------- guard moves

    // BRACE (the shield move when no shield is equipped): cuts the damage of every enemy hit by BRACE_REDUCTION for the next
    // BRACE_TURNS enemy turns. BRACE and BULWARK STANCE always go first (priority) but can't be used twice in a row.
    public static final double BRACE_REDUCTION = 0.30;
    public static final int BRACE_TURNS = 2;

    // ------------------------------------------------------------------------------------------------- move unlocks

    // The knight learns the moves of each type (MELEE / RANGED / SHIELD) as he levels, slot by slot, in AbilityLibrary's
    // order: slot 1 at level 1 (GALE SLASH, WINDBORNE ARROW, BULWARK STANCE), slot 2 at 10, slot 3 at 15, slot 4 at 20.
    // A move still has to be on the equipped item to be used. Change these numbers to move the unlocks.
    public static final int[] MOVE_UNLOCK_LEVELS = { 1, 10, 15, 20 };

    public static int moveUnlockLevel(int slot) {
        return MOVE_UNLOCK_LEVELS[Math.max(0, Math.min(MOVE_UNLOCK_LEVELS.length - 1, slot))];
    }

    public static boolean moveUnlocked(int slot, int playerLevel) { return playerLevel >= moveUnlockLevel(slot); }

    // ------------------------------------------------------------------------------------------------- items

    // After a win, every EQUIPPED item gains this share of the battle's XP (items never gained XP in play before; the only
    // way was the inventory debug key).
    public static final double ITEM_XP_SHARE = 1.0;
}