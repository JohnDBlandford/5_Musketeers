package NPCs.World1;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class HollowChorusZealot extends Enemy {

    private int enemyLevel;
    private static final Random random = new Random();

    // Baseline Stats (Level 5)
    private static final float BASE_HP = 220f;
    private static final float BASE_ATK = 38f;
    private static final float BASE_DEF = 24f;
    private static final float BASE_SPD = 26f;
    private static final int BASE_XP = 85;
    private static final int BASE_GOLD = 50;

    public HollowChorusZealot(int id, Point location) {
        this(id, location, 5);
    }

    public HollowChorusZealot(int id, Point location, int level) {
        super(id, location, "Hollow Chorus Zealot",
                BASE_HP,
                BASE_ATK,
                BASE_DEF,
                BASE_SPD,
                BASE_XP,
                BASE_GOLD,
                15.0f); // Spawn rate %

        this.enemyLevel = level;
    }

    // Hollow Chorus Zealot Moveset

    public void moveSiphonStrike(Player player) {
        float hpBeforeAttack = player != null ? player.getCurrentHP() : 0f;

        CombatResolver.resolveEnemyAttack(this, player, 1.20f);

        float hpAfterAttack = player != null ? player.getCurrentHP() : 0f;
        float damageDealt = Math.max(0f, hpBeforeAttack - hpAfterAttack);
        float healAmount = damageDealt * 0.20f;

        this.currentHP = Math.min(this.maxHP, this.currentHP + healAmount);

        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("Hollow Chorus Zealot drains health!");
            map.getTextbox().setIsActive(true);
        }
    }

    public void moveFanaticalSlash(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.35f);
    }

    public void moveDarkChant(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.15f);
            player.alterDEFModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.50f);
    }

    public void moveDoomsdayRite(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.60f);
    }

    public void executeTurn(Player player) {
        // AI: Uses Doomsday Rite if heavily wounded (<40% HP)
        if ((currentHP / maxHP) < 0.40f && random.nextBoolean()) {
            moveDoomsdayRite(player);
            return;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveSiphonStrike(player); break;
            case 1: moveFanaticalSlash(player); break;
            default: moveDarkChant(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }
}