package NPCs.World2;

import Level.Player;
import NPCs.Enemy;
import Utils.Point;
import Engine.CombatResolver;

import java.util.Random;

public class RaiderSkirmisher extends Enemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 5)
    private static final float BASE_HP = 140f;
    private static final float BASE_ATK = 42f;
    private static final float BASE_DEF = 22f;
    private static final float BASE_SPD = 38f;
    private static final int BASE_XP = 80;
    private static final int BASE_GOLD = 50;

    public RaiderSkirmisher(int id, Point location) {
        this(id, location, 3 + (int)(Math.random() * 3));
    }

    public RaiderSkirmisher(int id, Point location, int level){
        super(id, location, "RaiderSkirmisher",
                BASE_HP + ((level - 5) * 20f),
                BASE_ATK + ((level - 5) * 4f),
                BASE_DEF + ((level - 5) * 2.5f),
                BASE_SPD + ((level - 5) * 3f),
                Math.round(BASE_XP * (1f + ((level - 5) * 0.15f))),
                Math.round(BASE_GOLD * (1f + ((level - 5) * 0.15f))),
                35.0f);

        this.enemyLevel = Math.min(7, Math.max(5, level));
    }

    public void advanceTurn(){
        turnCount++;
    }

    private float getDamageMultiplier(Player player, float baseMultiplier) {
        if (player != null && (player.getCurrentHP() / player.getMaxHP()) < 0.35f) {
            return baseMultiplier + 0.10f;
        }
        return baseMultiplier;
    }

    // Moveset

    public void moveSlash(Player player) {
        float multiplier = getDamageMultiplier(player, 1.0f);
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
    }

    public void moveExecuteStrike(Player player) {
        float multiplier = getDamageMultiplier(player, 1.35f);
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
    }

    public void moveFeintThrust(Player player) {
        if (player != null) {
            player.alterDEFModifier(-0.10f);
        }
        float multiplier = getDamageMultiplier(player, 0.70f);
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
    }

    public void moveQuickDagger(Player player) {
        float multiplier = getDamageMultiplier(player, 0.60f);
        CombatResolver.resolveEnemyAttack(this, player, multiplier);
        if (player != null && player.getCurrentHP() > 0) {
            CombatResolver.resolveEnemyAttack(this, player, multiplier);
        }
    }

    public void executeTurn(Player player) {
        advanceTurn();

        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveSlash(player); break;
            case 1: moveExecuteStrike(player); break;
            case 2: moveFeintThrust(player); break;
            default: moveQuickDagger(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}

