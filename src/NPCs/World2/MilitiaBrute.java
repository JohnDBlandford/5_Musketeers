package NPCs.World2;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class MilitiaBrute extends Enemy {

    private int enemyLevel;
    private int turnCount = 0;
    private boolean isGuarding = false;
    private static final Random random = new Random();

    // Baseline Stats (Level 6 Baseline)
    private static final float BASE_HP = 280f;
    private static final float BASE_ATK = 50f;
    private static final float BASE_DEF = 48f;
    private static final float BASE_SPD = 20f;
    private static final int BASE_XP = 110;
    private static final int BASE_GOLD = 75;

    public MilitiaBrute(int id, Point location) {
        this(id, location, 6 + (int)(Math.random() * 3));
    }

    public MilitiaBrute(int id, Point location, int level) {
        super(id, location, "Militia Brute",
                BASE_HP + ((level - 6) * 30f),
                BASE_ATK + ((level - 6) * 5f),
                BASE_DEF + ((level - 6) * 5f),
                BASE_SPD + ((level - 6) * 1.5f),
                Math.round(BASE_XP * (1f + ((level - 6) * 0.15f))),
                Math.round(BASE_GOLD * (1f + ((level - 6) * 0.15f))),
                30.0f); 

        this.enemyLevel = Math.min(8, Math.max(6, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    // Moveset

    public void moveGuardStance() {
        this.alterDEFModifier(0.30f);
        this.isGuarding = true;

        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("Militia Brute assumes a defensive stance!");
            map.getTextbox().setIsActive(true);
        }
    }

    public void moveHeavySlam(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.20f);
    }

    public void moveShieldBash(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.85f);
    }

    public void moveWarCry(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.40f);
    }

    public void executeTurn(Player player) {
        advanceTurn();

        // AI: Alternates between guard stance and attacking
        if (!isGuarding && random.nextDouble() < 0.35) {
            moveGuardStance();
            return;
        }

        if (isGuarding) {
            this.alterDEFModifier(-0.30f); // Reset temporary guard bonus on attack
            this.isGuarding = false;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveHeavySlam(player); break;
            case 1: moveShieldBash(player); break;
            default: moveWarCry(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public boolean isGuarding() {
        return isGuarding;
    }
}