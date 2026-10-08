package NPCs.World3;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class AbyssalHusk extends Enemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 11 Baseline per Chapter 3)
    private static final float BASE_HP = 230f;
    private static final float BASE_ATK = 75f;
    private static final float BASE_DEF = 22f;
    private static final float BASE_SPD = 60f;
    private static final int BASE_XP = 170;
    private static final int BASE_GOLD = 90;

    public AbyssalHusk(int id, Point location) {
        this(id, location, 11 + (int)(Math.random() * 4));
    }

    public AbyssalHusk(int id, Point location, int level) {
        super(id, location, "Abyssal Husk",
                BASE_HP + ((level - 11) * 20f),
                BASE_ATK + ((level - 11) * 7.5f),
                BASE_DEF + ((level - 11) * 2f),
                BASE_SPD + ((level - 11) * 4.5f),
                Math.round(BASE_XP * (1f + ((level - 11) * 0.15f))),
                Math.round(BASE_GOLD * (1f + ((level - 11) * 0.15f))),
                20.0f); // 20% Spawn Rate

        this.enemyLevel = Math.min(14, Math.max(11, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    // --- Moveset ---

    public void moveFrenziedClaws(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 0.60f);
        if (player != null && player.getCurrentHP() > 0) {
            CombatResolver.resolveEnemyAttack(this, player, 0.60f);
        }
    }

    public void moveAbyssalBite(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.30f);
    }

    public void moveVoidScreech(Player player) {
        if (player != null) {
            player.alterDEFModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.40f);
    }

    public void moveRecklessLunge(Player player) {
        // High risk move: Deals massive damage but lowers own defense
        this.alterDEFModifier(-0.10f);
        CombatResolver.resolveEnemyAttack(this, player, 1.50f);
    }

    public void executeTurn(Player player) {
        advanceTurn();

        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveFrenziedClaws(player); break;
            case 1: moveAbyssalBite(player); break;
            case 2: moveVoidScreech(player); break;
            default: moveRecklessLunge(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}