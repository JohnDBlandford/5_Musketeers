package NPCs.World2;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class BaronOfTheClearedWood extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 12 Boss per Chapter 3)
    private static final float BASE_HP = 700f;
    private static final float BASE_ATK = 85f;
    private static final float BASE_DEF = 58f;
    private static final float BASE_SPD = 40f;
    private static final int BASE_XP = 450;
    private static final int BASE_GOLD = 350;

    public BaronOfTheClearedWood(int id, Point location) {
        this(id, location, 12);
    }

    public BaronOfTheClearedWood(int id, Point location, int level) {
        super(id, location, "Baron of the Cleared Wood",
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

    // --- Moveset ---

    public void moveClearedWoodCleave(Player player) {
        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("The Baron unleashes Cleared Wood Cleave!");
            map.getTextbox().setIsActive(true);
        }
        if (player != null) {
            player.alterDEFModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.60f);
    }

    public void moveCommandingSmite(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.30f);
    }

    public void moveIronRally() {
        this.alterDEFModifier(0.20f);
        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("The Baron rallies his defense!");
            map.getTextbox().setIsActive(true);
        }
    }

    public void moveDecapitatingSweep(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.10f);
    }

    public void executeTurn(Player player) {
        advanceTurn();

        // Scripted pattern: Every 3rd turn unleashes Cleared Wood Cleave
        if (turnCount > 0 && turnCount % 3 == 0) {
            moveClearedWoodCleave(player);
            return;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveCommandingSmite(player); break;
            case 1: moveIronRally(); break;
            default: moveDecapitatingSweep(player); break;
        }
    }

    // ---------------------------------------------------------------- BATTLE hooks (used by Screens.Battle.PlayBattleScreen)
    // Same moves, multipliers and passives as executeTurn(), but the battle does the damage and shows the text.
    // Stat changes use the exact amounts above; "-" effects on the knight happen only if the move lands.

    @Override
    public void onBattleStart() {
        turnCount = 0;
    }

    @Override
    public int battleLevel() {
        return enemyLevel;
    }

    @Override
    public Move chooseBattleMove(Random rng, Player player) {
        advanceTurn();
        if (turnCount % 3 == 0) return Move.attack("Cleared Wood Cleave", 1.60f).lowersPlayer(Move.DEF, 0.15f);   // every 3rd turn
        switch (rng.nextInt(3)) {
            case 0:  return Move.attack("Commanding Smite", 1.30f);
            case 1:  return Move.status("Iron Rally").raisesSelf(Move.DEF, 0.20f);
            default: return Move.attack("Decapitating Sweep", 1.10f).lowersPlayer(Move.SPD, 0.10f);
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}