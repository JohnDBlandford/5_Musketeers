package NPCs.World1;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class TheFamineWarden extends Enemy {

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

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}