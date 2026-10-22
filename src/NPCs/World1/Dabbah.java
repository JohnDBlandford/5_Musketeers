package NPCs.World1;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class Dabbah extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int turnCount = 0;
    private int phase = 1; // Phases 1, 2, and 3
    private String formTitle = "Dabbah, Beast of the Earth";
    private static final Random random = new Random();

    // Baseline Stats (Final Boss)
    private static final float BASE_HP = 1500f;
    private static final float BASE_ATK = 160f;
    private static final float BASE_DEF = 95f;
    private static final float BASE_SPD = 65f;
    private static final int BASE_XP = 1500;
    private static final int BASE_GOLD = 1000;

    public Dabbah(int id, Point location) {
        this(id, location, 20);
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
            this.currentHP = this.maxHP; // Fully restore HP for next phase
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
        currentHP = maxHP;
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
        currentHP = maxHP;
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