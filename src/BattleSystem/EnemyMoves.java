package BattleSystem;

import java.util.*;

// The moves each enemy can use, picked at random every turn (never the same move twice in a row).
// Every move is one of three kinds:
//   plain attack            normal damage (the enemy's ATK against your DEF, same formula as before)
//   attack with a side effect   the same damage, and a 35% chance of also changing a stat
//   status move             no damage, it always changes a stat (like Growl in Pokemon)
//
// Moves are written as short text so they are easy to edit:
//   "Ragged Claw"          plain attack
//   "Famished Bite*-def"   attack that may lower YOUR DEF      (the part after * is the side effect)
//   "Pitiful Wail|-atk"    status move that lowers YOUR ATK    (the part after | is the effect)
//   "Menacing Howl|+atk"   status move that raises the ENEMY's own ATK
// An effect is  + or -  followed by  atk, def or spd.   "-" hits the player, "+" buffs the enemy itself.
//
// An enemy is found by its name (any part of it, ignoring capitals and apostrophes). Unknown enemies fall back to a list for their
// type (Skirmisher / Brute / Beast / Elite) and then a generic one, so a new enemy never breaks the battle.
public final class EnemyMoves {
    private EnemyMoves() {}

    // one stat change a move can cause. stat: 0 ATK, 1 DEF, 2 SPD. onPlayer = hits the knight, otherwise it changes the enemy itself.
    // amount is the exact change to the stat modifier (+0.10 = +10%, -0.15 = -15%).
    public static final class Fx {
        public final int stat;
        public final boolean onPlayer;
        public final float amount;
        Fx(int stat, boolean onPlayer, float amount) { this.stat = stat; this.onPlayer = onPlayer; this.amount = amount; }
    }

    // One enemy move. Enemy classes build theirs with the helpers below, for example
    //     Move.attack("Gore Charge", 1.30f).lowersPlayer(Move.DEF, 0.10f)
    //     Move.attack("Frenzied Trample", 0.45f).hits(2)
    //     Move.status("Iron Rally").raisesSelf(Move.DEF, 0.20f)
    // damage = the enemy's ATK x multiplier against the knight's DEF, once per hit.
    // Effects on the knight only happen if the move lands; effects on the enemy itself always happen.
    public static final class Move {
        public static final int ATK = 0, DEF = 1, SPD = 2;

        public final String name;
        public final boolean damages;       // false = status move (no damage)
        public final boolean flees;         // the enemy runs away and the battle ends
        float multiplier = 1f;              // scales the enemy's ATK for this move
        int hits = 1;
        float effectChance = 1f;            // chance the knight-effects happen after a damaging hit
        float accuracy = 1f;                // chance the move hits at all (1 = never misses)
        final List<Fx> effects = new ArrayList<>();

        private Move(String name, boolean damages, boolean flees) { this.name = name; this.damages = damages; this.flees = flees; }

        public static Move attack(String name, float multiplier) { Move m = new Move(name, true, false); m.multiplier = multiplier; return m; }
        public static Move status(String name) { return new Move(name, false, false); }
        public static Move flee(String name) { return new Move(name, false, true); }

        public Move hits(int n) { this.hits = Math.max(1, n); return this; }
        public Move accuracy(float a) { this.accuracy = a; return this; }
        public Move chance(float c) { this.effectChance = c; return this; }
        public Move lowersPlayer(int stat, float amount) { effects.add(new Fx(stat, true, -Math.abs(amount))); return this; }
        public Move raisesSelf(int stat, float amount) { effects.add(new Fx(stat, false, Math.abs(amount))); return this; }
        public Move lowersSelf(int stat, float amount) { effects.add(new Fx(stat, false, -Math.abs(amount))); return this; }

        public float multiplier() { return multiplier; }
        public int hits() { return hits; }
        public float accuracy() { return accuracy; }
        public float effectChance() { return effectChance; }
        public List<Fx> effects() { return effects; }
        public boolean hasEffect() { return !effects.isEmpty(); }
    }

    // the short-text moves below: "*" = attack with a 35% side effect, "|" = status move, "-" hits the player, "+" buffs the enemy
    private static Move parse(String s) {
        int bar = s.indexOf('|'), star = s.indexOf('*');
        int cut = bar >= 0 ? bar : star;
        if (cut < 0) return Move.attack(s, 1f);
        String name = s.substring(0, cut), fx = s.substring(cut + 1).trim().toLowerCase();
        boolean up = fx.startsWith("+");
        int stat = fx.endsWith("atk") ? 0 : (fx.endsWith("def") ? 1 : 2);
        Move m = bar < 0 ? Move.attack(name, 1f).chance(0.35f) : Move.status(name);
        return up ? m.raisesSelf(stat, 0.15f) : m.lowersPlayer(stat, 0.15f);
    }

    private static final Map<String, Move[]> MOVES = new LinkedHashMap<>();

    private static void add(String enemy, String... moves) {
        Move[] list = new Move[moves.length];
        for (int i = 0; i < moves.length; i++) list[i] = parse(moves[i]);
        MOVES.put(enemy, list);
    }

    static {
        // World 1 (opening)
        add("starving wretch", "Desperate Lunge", "Ragged Claw", "Famished Bite*-def", "Pitiful Wail|-atk");
        add("reckoner brute", "Shield Slam", "Brutal Cudgel*-def", "Hardened Stance|+def", "Taunting Roar|-atk");
        add("ash wolf", "Ember Bite", "Ash Pounce*-spd", "Savage Snap", "Menacing Howl|+atk");
        add("hollow chorus zealot", "Zealots Smite", "Hymn Strike*-atk", "Fervent Slash", "Rite of Fervor|+atk");
        add("famine warden", "Hunger Sweep", "Starving Slam*-def", "Famines Grasp*-spd", "Wardens Decree|-atk");
        // World 2
        add("raider skirmisher", "Raiders Stab", "Dirty Slash*-def", "Ambush Cut", "Dirt Kick|-spd");
        add("militia brute", "Pike Thrust", "Shield Bash*-spd", "Militia Hammer", "Shield Wall|+def");
        add("blighted boar", "Blighted Charge", "Gore*-def", "Trample", "Frenzied Snort|+atk");
        add("baron's vanguard", "Vanguard Strike", "Twin Blades", "Barons Edict|-atk", "Rallying Cry|+atk");
        add("baron of the cleared wood", "Timber Fall", "Axe Cleave*-def", "Clearing Sweep", "Barons Wrath|+atk");
        // World 3
        add("witch-hunter", "Hunters Brand|-def", "Silver Stake", "Pyre Torch*-atk", "Witch Bolt");
        add("inquisitor brute", "Interrogation Hammer", "Silencing Blow*-atk", "Iron Verdict", "Righteous Guard|+def");
        add("corrupted stag", "Antler Gore", "Withering Charge*-def", "Thorned Rush*-spd", "Wild Bellow|+spd");
        add("hunt-captain's retinue", "Retinue Volley", "Captains Order|+atk", "Spear Wall|+def", "Horn Charge*-spd");
        add("the hunt-captain", "Hunt Captains Spear", "Mob Rally|+atk", "Cruel Cleave*-def", "Final Hunt");
        // World 1 (final)
        add("crusader skirmisher", "Crusader Slash", "Holy Lunge*-def", "Squad Strike", "Battle Hymn|+atk");
        add("beast-slayer brute", "Slayers Maul", "Beast Breaker*-spd", "Iron Crush*-def", "Unyielding Guard|+def");
        add("reckoning direwolf", "Direwolf Maul", "Rending Fangs*-def", "Savage Leap", "Bloodthirsty Howl|+atk");
        add("marked reckoner", "Marked Strike", "Reckoners Brand|-def", "Judgement Cut*-atk", "Branded Blow");
        add("last crusader", "Last Crusade", "Crusaders Wrath*-def", "Sunder", "Martyrs Oath|+atk");
        // Dabbah, one list per phase
        add("the witness", "Famine Gaze|-atk", "Withering Wail*-spd", "Barren Touch");
        add("the judge", "Mark of Judgment|-def", "Verdict", "Condemn*-atk");
        add("the reckoning", "Unmaking*-def", "Ruin", "Final Reckoning", "Hollow Maw*-spd");
        add("dabbah", "Famine Gaze|-atk", "Mark of Judgment|-def", "Ruin");
        // fallbacks by type
        add("skirmisher", "Quick Slash", "Sneak Stab*-def", "Reckless Lunge", "Dirt Kick|-spd");
        add("brute", "Heavy Smash", "Crushing Blow*-def", "Bull Rush", "Hardened Stance|+def");
        add("beast", "Savage Bite", "Feral Pounce*-spd", "Rending Claw", "Menacing Howl|+atk");
        add("elite", "Elite Strike", "Merciless Cut*-def", "Brutal Onslaught", "Battle Cry|+atk");
    }
    private static final Move[] GENERIC = {parse("Strike"), parse("Heavy Blow"), parse("Wild Swing")};

    private static Move[] listFor(String enemyName) {
        String n = enemyName.toLowerCase().replace("’", "'");
        String noApos = n.replace("'", "");
        for (Map.Entry<String, Move[]> e : MOVES.entrySet()) {
            String k = e.getKey();
            if (n.contains(k) || noApos.contains(k.replace("'", ""))) return e.getValue();
        }
        return GENERIC;
    }

    // a random move for this enemy, never the same one twice in a row (pass the last move used, or null)
    public static Move pick(String enemyName, Random rng, Move last) {
        Move[] list = listFor(enemyName);
        if (list.length == 1) return list[0];
        Move m;
        do { m = list[rng.nextInt(list.length)]; } while (m == last);
        return m;
    }
}