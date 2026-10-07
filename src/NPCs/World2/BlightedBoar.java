package NPCs.World2;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class BlightedBoar extends Enemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 7 Baseline per Chapter 3)
    private static final float BASE_HP = 220f;
    private static final float BASE_ATK = 58f;
    private static final float BASE_DEF = 20f;
    private static final float BASE_SPD = 48f;
    private static final int BASE_XP = 100;
    private static final int BASE_GOLD = 60;

    public BlightedBoar(int id, Point location) {
        this(id, location, 7 + (int)(Math.random() * 3));
    }

    public BlightedBoar(int id, Point location, int level) {
        super(id, location, "Blighted Boar",
                BASE_HP + ((level - 7) * 22f),
                BASE_ATK + ((level - 7) * 6f),
                BASE_DEF + ((level - 7) * 2f),
                BASE_SPD + ((level - 7) * 4f),
                Math.round(BASE_XP * (1f + ((level - 7) * 0.15f))),
                Math.round(BASE_GOLD * (1f + ((level - 7) * 0.15f))),
                20.0f); // 20% Spawn Rate

        this.enemyLevel = Math.min(9, Math.max(7, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    // --- Moveset ---

    public void moveWildTusk(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.10f);
    }

    public void moveGoreCharge(Player player) {
        if (player != null) {
            player.alterDEFModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.30f);
    }

    public void moveBlightSpit(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.50f);
    }

    public void moveFrenziedTrample(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 0.45f);
        if (player != null && player.getCurrentHP() > 0) {
            CombatResolver.resolveEnemyAttack(this, player, 0.45f);
        }
    }

    public void executeTurn(Player player) {
        advanceTurn();

        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveWildTusk(player); break;
            case 1: moveGoreCharge(player); break;
            case 2: moveBlightSpit(player); break;
            default: moveFrenziedTrample(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}