package NPCs.World3;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class TheWatcherInTheAsh extends Enemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 18 Boss per Chapter 3)
    private static final float BASE_HP = 950f;
    private static final float BASE_ATK = 125f;
    private static final float BASE_DEF = 75f;
    private static final float BASE_SPD = 55f;
    private static final int BASE_XP = 750;
    private static final int BASE_GOLD = 500;

    public TheWatcherInTheAsh(int id, Point location) {
        this(id, location, 18);
    }

    public TheWatcherInTheAsh(int id, Point location, int level) {
        super(id, location, "The Watcher in the Ash",
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

    public void moveAshenOblivion(Player player) {
        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("The Watcher in the Ash unleashes Ashen Oblivion!");
            map.getTextbox().setIsActive(true);
        }
        if (player != null) {
            player.alterDEFModifier(-0.20f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.70f);
    }

    public void moveGazeOfDesolation(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.25f);
    }

    public void moveAshenShield() {
        this.alterDEFModifier(0.25f);
        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("The Watcher fortifies itself with dense ash!");
            map.getTextbox().setIsActive(true);
        }
    }

    public void moveCataclysmicRay(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.35f);
    }

    public void executeTurn(Player player) {
        advanceTurn();

        // Scripted pattern: Every 3rd turn unleashes Ashen Oblivion
        if (turnCount > 0 && turnCount % 3 == 0) {
            moveAshenOblivion(player);
            return;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveGazeOfDesolation(player); break;
            case 1: moveAshenShield(); break;
            default: moveCataclysmicRay(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}