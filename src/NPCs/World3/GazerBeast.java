package NPCs.World3;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class GazerBeast extends Enemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 12 Baseline per Chapter 3)
    private static final float BASE_HP = 260f;
    private static final float BASE_ATK = 80f;
    private static final float BASE_DEF = 28f;
    private static final float BASE_SPD = 52f;
    private static final int BASE_XP = 190;
    private static final int BASE_GOLD = 130;

    public GazerBeast(int id, Point location) {
        this(id, location, 12 + (int)(Math.random() * 4));
    }

    public GazerBeast(int id, Point location, int level) {
        super(id, location, "Gazer Beast",
                BASE_HP + ((level - 12) * 24f),
                BASE_ATK + ((level - 12) * 7f),
                BASE_DEF + ((level - 12) * 2.5f),
                BASE_SPD + ((level - 12) * 4f),
                Math.round(BASE_XP * (1f + ((level - 12) * 0.15f))),
                Math.round(BASE_GOLD * (1f + ((level - 12) * 0.15f))),
                30.0f); // 30% Spawn Rate

        this.enemyLevel = Math.min(15, Math.max(12, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    // --- Moveset ---

    public void moveEyestalkBeam(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.15f);
    }

    public void moveMindPiercingGlare(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.85f);
    }

    public void moveVoidBlink(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.20f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.50f);
    }

    public void moveMultiBeamBarrage(Player player) {
        int hits = 3;
        for (int i = 0; i < hits; i++) {
            if (player != null && player.getCurrentHP() > 0) {
                CombatResolver.resolveEnemyAttack(this, player, 0.45f);
            }
        }
    }

    public void executeTurn(Player player) {
        advanceTurn();

        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveEyestalkBeam(player); break;
            case 1: moveMindPiercingGlare(player); break;
            case 2: moveVoidBlink(player); break;
            default: moveMultiBeamBarrage(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}