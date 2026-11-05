package NPCs.World1;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.Balance;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

// BALANCE CHANGES (search for "BALANCE:"):
//   - FIX: Dabbah now always fights with its real stats. Before, a Dabbah built with "new Dabbah(...)" (the P test key does
//     this) kept the old placeholder numbers below (level 20, 1,500 HP, 160 ATK, 95 DEF, 65 SPD) because only
//     EnemyFactory.boss("dabbah") ran it through Balance. The constructor now scales it itself: level 70, Balance's
//     FINAL_BOSS stats, so every Dabbah is the same however it is made.
//   - each FORM has its own stats (design doc 3.6): HP and DEF fall, ATK and SPD rise (Balance.DABBAH_FORM_*):
//         form 1  Beast of the Earth   2,690 HP  450 ATK  422 DEF  282 SPD
//         form 2  Scourge of the Ash   2,276 HP  525 ATK  343 DEF  360 SPD
//         form 3  The Unbound Ruin     1,862 HP  621 ATK  277 DEF  454 SPD
//     Before, every form came back with the same stats and full HP. The +10% move power per form is unchanged.
//   - NEW: Dabbah can land CRITICAL HITS: x1.5 damage, 15% / 20% / 25% chance in forms 1 / 2 / 3 (CRIT_CHANCE). Each one
//     plays an impact frame in the battle (CINEMATIC). Before, enemies never landed critical hits.
// The moves, the every-3rd-turn Judgement, the form names and the lines are unchanged.
public class Dabbah extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int turnCount = 0;
    private int phase = 1; // Phases 1, 2, and 3
    private String formTitle = "Dabbah, Beast of the Earth";
    private static final Random random = new Random();

    // Baseline Stats (Final Boss). BALANCE: only placeholders now: the constructor replaces them with Balance's level 70 stats.
    private static final float BASE_HP = 1500f;
    private static final float BASE_ATK = 160f;
    private static final float BASE_DEF = 95f;
    private static final float BASE_SPD = 65f;
    private static final int BASE_XP = 1500;
    private static final int BASE_GOLD = 1000;

    // BALANCE: NEW. Dabbah's critical hits (PlayBattleScreen rolls them on every hit Dabbah lands)
    public static final float CRIT_MULTIPLIER = 1.5f;
    private static final float[] CRIT_CHANCE = {0.15f, 0.20f, 0.25f};   // forms 1, 2, 3

    public float critChance() {
        return CRIT_CHANCE[Math.max(0, Math.min(2, phase - 1))];
    }

    // BALANCE: form 1's stats (as scaled); forms 2 and 3 are worked out from these with Balance.DABBAH_FORM_*
    private float form1Hp, form1Atk, form1Def, form1Spd;

    public Dabbah(int id, Point location) {
        this(id, location, Balance.DABBAH_LEVEL);   // BALANCE: was level 20
    }

    public Dabbah(int id, Point location, int level) {
        super(id, location, "Dabbah, Beast of the Earth",
                BASE_HP,
                BASE_ATK,
                BASE_DEF,
                BASE_SPD,
                BASE_XP,
                BASE_GOLD,
                1.0f); // 1.0% encounter rate

        this.enemyLevel = level;
        // BALANCE: FIX. Scale it right away (level 70, FINAL_BOSS stats), so "new Dabbah(...)" is never the placeholder numbers
        Balance.scale(this, Balance.Archetype.FINAL_BOSS, level);
    }

    // BALANCE: whenever it is (re)scaled, remember those numbers as form 1's
    @Override
    public void applyScaling(int level, float hp, float atk, float def, float spd, int xp, int gold) {
        super.applyScaling(level, hp, atk, def, spd, xp, gold);
        this.enemyLevel = level;
        form1Hp = hp;
        form1Atk = atk;
        form1Def = def;
        form1Spd = spd;
    }

    // BALANCE: NEW. Sets HP / ATK / DEF / SPD for the current form (and fills HP). Stat drops from the fight stay as they are.
    private void applyForm() {
        if (form1Hp <= 0) return;   // never scaled (can't happen: the constructor scales it)
        int i = Math.max(0, Math.min(2, phase - 1));
        this.maxHP = Math.round(form1Hp * Balance.DABBAH_FORM_HP[i]);
        this.currentHP = this.maxHP;
        this.baseATK = Math.round(form1Atk * Balance.DABBAH_FORM_ATK[i]);
        this.baseDEF = Math.round(form1Def * Balance.DABBAH_FORM_DEF[i]);
        this.baseSPD = Math.max(1f, Math.round(form1Spd * Balance.DABBAH_FORM_SPD[i]));
    }

    public void advanceTurn() {
        turnCount++;
    }

    /**
     * Calculates move multiplier with a +10% increase per phase progression.
     * Phase 1: Base multiplier
     * Phase 2: +0.10f multiplier
     * Phase 3: +0.20f multiplier
     */
    private float getPhaseMultiplier(float baseMultiplier) {
        return baseMultiplier + ((phase - 1) * 0.10f);
    }

    /**
     * Triggered automatically whenever damage is received.
     * Checks if boss can revive into Phase 2 or Phase 3.
     */
    @Override
    protected void onTakeDamage(float damageAmount) {
        if (this.currentHP <= 0 && phase < 3) {
            phase++;
            applyForm();                 // BALANCE: the next form's own stats, at full HP (was: same stats, full HP)
            this.turnCount = 0;          // Reset turn counter for ultimate timing

            if (map != null && map.getTextbox() != null) {
                map.getTextbox().setIsActive(true);
                if (phase == 2) {
                    this.formTitle = "Dabbah, Scourge of the Ash";
                    map.getTextbox().addText("Dabbah's carcass splits open, bathed in black embers!");
                    map.getTextbox().addText("\"Fools... You think this dirt can bind what was promised to the stars?\"");
                } else if (phase == 3) {
                    this.formTitle = "Dabbah, The Unbound Ruin";
                    map.getTextbox().addText("The very earth beneath you shatters as Dabbah unearths its true form!");
                    map.getTextbox().addText("\"Behold the hour of harvest... I am the end written before time!\"");
                }
            }
        }
    }

    // --- Moveset ---

    public void moveJudgementOfDabbah(Player player) {
        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText(formTitle + " calls down Judgement of Dabbah!");
            map.getTextbox().setIsActive(true);
        }
        if (player != null) {
            player.alterDEFModifier(-0.25f);
        }
        float multiplier = getPhaseMultiplier(1.80f);
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
    }

    public void moveEarthshakingSlam(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.20f);
        }
        float multiplier = getPhaseMultiplier(1.40f);
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
    }

    public void moveWrathOfTheBeast(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.15f);
        }
        float multiplier = getPhaseMultiplier(1.20f);
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
    }

    public void moveInfernalRupture(Player player) {
        float multiplier = getPhaseMultiplier(0.75f);
        int hits = 2;
        for (int i = 0; i < hits; i++) {
            if (player != null && player.getCurrentHP() > 0) {
                CombatResolver.resolveEnemyAttack(this, player, multiplier);
            }
        }
    }

    public void executeTurn(Player player) {
        advanceTurn();

        // Scripted pattern: Every 3rd turn unleashes Judgement of Dabbah
        if (turnCount > 0 && turnCount % 3 == 0) {
            moveJudgementOfDabbah(player);
            return;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveEarthshakingSlam(player); break;
            case 1: moveWrathOfTheBeast(player); break;
            default: moveInfernalRupture(player); break;
        }
    }

    // ---------------------------------------------------------------- BATTLE hooks (used by Screens.Battle.PlayBattleScreen)
    // Same moves, multipliers and passives as executeTurn(), but the battle does the damage and shows the text.
    // Stat changes use the exact amounts above; "-" effects on the knight happen only if the move lands.

    @Override
    public void onBattleStart() {
        turnCount = 0;
        phase = 1;
        formTitle = "Dabbah, Beast of the Earth";
        applyForm();   // BALANCE: form 1's stats at full HP (was: currentHP = maxHP)
    }

    @Override
    public int battleLevel() {
        return enemyLevel;
    }

    @Override
    public Move chooseBattleMove(Random rng, Player player) {
        advanceTurn();
        if (turnCount % 3 == 0) {   // scripted: every 3rd turn
            return Move.attack("Judgement of Dabbah", getPhaseMultiplier(1.80f)).lowersPlayer(Move.DEF, 0.25f);
        }
        switch (rng.nextInt(3)) {
            case 0:  return Move.attack("Earthshaking Slam", getPhaseMultiplier(1.40f)).lowersPlayer(Move.SPD, 0.20f);
            case 1:  return Move.attack("Wrath of the Beast", getPhaseMultiplier(1.20f)).lowersPlayer(Move.ATK, 0.15f);
            default: return Move.attack("Infernal Rupture", getPhaseMultiplier(0.75f)).hits(2);
        }
    }

    // reaching 0 HP in phase 1 or 2 revives Dabbah in the next phase (the revive in onTakeDamage can never run, because the
    // damage code only calls it while HP is still above 0)
    @Override
    public String[] onBattleDefeat() {
        if (phase >= 3) return null;
        phase++;
        applyForm();   // BALANCE: the next form's own stats, at full HP (was: currentHP = maxHP with the same stats)
        turnCount = 0;
        if (phase == 2) {
            formTitle = "Dabbah, Scourge of the Ash";
            return new String[] {"Dabbah's carcass splits open, bathed in black embers!",
                    "\"Fools... You think this dirt can bind what was promised to the stars?\""};
        }
        formTitle = "Dabbah, The Unbound Ruin";
        return new String[] {"The very earth beneath you shatters as Dabbah unearths its true form!",
                "\"Behold the hour of harvest... I am the end written before time!\""};
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public int getPhase() {
        return phase;
    }

    public String getFormTitle() {
        return formTitle;
    }
}