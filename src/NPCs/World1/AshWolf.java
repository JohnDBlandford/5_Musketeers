package NPCs.World1;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class AshWolf extends Enemy {

    public int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Level 3 Baseline Stats
    private static final float BASE_HP = 110f;
    private static final float BASE_ATK = 30f;
    private static final float BASE_DEF = 9f;
    private static final float BASE_SPD = 30f;
    private static final int BASE_XP = 35;
    private static final int BASE_GOLD = 18;

    public AshWolf(int id, Point location) {
        this(id, location, 3 + (int)(Math.random() * 3));
    }

    public AshWolf(int id, Point location, int level) {
        super(id, location, "Ash Wolf",
                BASE_HP + ((level - 3) * 15f),
                BASE_ATK + ((level - 3) * 6f),
                BASE_DEF + ((level - 3) * 2f),
                BASE_SPD + ((level - 3) * 4f),
                Math.round(BASE_XP * (1f + ((level - 3) * 0.25f))),
                Math.round(BASE_GOLD * (1f + ((level - 3) * 0.25f))),
                20.0f); // 20% Spawn Rate

        this.enemyLevel = Math.min(3, Math.max(1, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    public void moveAshAmbush(Player player) {
        float multiplier = 1.10f;
        if (turnCount <= 1) {
            multiplier += 0.20f;
        }
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
    }

    public void movePredatoryStrike(Player player) {
        float damageMultiplier = 1.20f;
        if (player != null && (player.getCurrentHP() / player.getMaxHP() < 0.35f)) {
            damageMultiplier += 0.40f;
        }
        CombatResolver.resolveEnemyAttack(this, player, damageMultiplier);
    }

    public void moveEmberHowl(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.40f);
    }

    public void moveSmogClaws(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 0.55f);
        if (player != null && player.getCurrentHP() > 0) {
            CombatResolver.resolveEnemyAttack(this, player, 0.55f);
        }
    }

    public void executeTurn(Player player) {
        advanceTurn();
        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveAshAmbush(player); break;
            case 1: movePredatoryStrike(player); break;
            case 2: moveEmberHowl(player); break;
            default: moveSmogClaws(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}