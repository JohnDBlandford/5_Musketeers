package NPCs.World1;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class TheFamineWarden extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 5 Boss)
    private static final float BASE_HP = 380f;
    private static final float BASE_ATK = 48f;
    private static final float BASE_DEF = 32f;
    private static final float BASE_SPD = 28f;
    private static final int BASE_XP = 150;
    private static final int BASE_GOLD = 100;

    public TheFamineWarden(int id, Point location) {
        this(id, location, 5);
    }

    public TheFamineWarden(int id, Point location, int level) {
        super(id, location, "The Famine Warden",
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

    // Famine Warden Moveset integrated with CombatResolver

    public void moveFamineCleave(Player player) {
        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("The Famine Warden unleashes Famine Cleave!");
            map.getTextbox().setIsActive(true);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.50f);
    }

    public void moveHeavyGreatsword(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.25f);
    }

    public void moveDesolationWave(Player player) {
        if (player != null) {
            player.alterDEFModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.90f);
    }

    public void moveWardensGrasp(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.20f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.60f);
    }

    public void executeTurn(Player player) {
        advanceTurn();

        // Scripted pattern: Every 3rd turn unleashes Famine Cleave
        if (turnCount > 0 && turnCount % 3 == 0) {
            moveFamineCleave(player);
            return;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveHeavyGreatsword(player); break;
            case 1: moveDesolationWave(player); break;
            default: moveWardensGrasp(player); break;
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
        if (turnCount % 3 == 0) return Move.attack("Famine Cleave", 1.50f);   // scripted: every 3rd turn
        switch (rng.nextInt(3)) {
            case 0:  return Move.attack("Heavy Greatsword", 1.25f);
            case 1:  return Move.attack("Desolation Wave", 0.90f).lowersPlayer(Move.DEF, 0.15f);
            default: return Move.attack("Warden's Grasp", 0.60f).lowersPlayer(Move.SPD, 0.20f);
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}