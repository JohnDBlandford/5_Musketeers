package NPCs.World1;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

public class ReckonerBrute extends Enemy {

    private int enemyLevel;
    private int turnCount = 0;
    private boolean guardActive = true;
    private static final Random random = new Random();

    // Baseline Stats (Level 2 Baseline)
    private static final float BASE_HP = 120f;
    private static final float BASE_ATK = 25f;
    private static final float BASE_DEF = 18f;
    private static final float BASE_SPD = 15f;
    private static final int BASE_XP = 45;
    private static final int BASE_GOLD = 25;

    public ReckonerBrute(int id, Point location) {
        this(id, location, 2 + (int)(Math.random() * 3));
    }

    public ReckonerBrute(int id, Point location, int level) {
        super(id, location, "Reckoner Brute",
                BASE_HP + ((level - 2) * 20f),
                BASE_ATK + ((level - 2) * 5f),
                BASE_DEF + ((level - 2) * 4f),
                BASE_SPD + ((level - 2) * 1.5f),
                Math.round(BASE_XP * (1f + ((level - 2) * 0.25f))),
                Math.round(BASE_GOLD * (1f + ((level - 2) * 0.25f))),
                25.0f);

        this.enemyLevel = Math.min(4, Math.max(2, level));

        // Guard Passive: Increases DEF by 50% (+0.50f)
        alterDEFModifier(0.50f);
    }

    /**
     * Call this dedicated function once per turn in your combat manager/loop.
     * Tracks Guard duration and automatically removes it after turn 5.
     */
    public void advanceTurn() {
        turnCount++;
        if (turnCount > 5 && guardActive) {
            removeGuard();
        }
    }

    private void removeGuard() {
        guardActive = false;
        alterDEFModifier(-0.50f); // Remove 50% DEF boost

        if (map != null && map.getTextbox() != null) {
            map.getTextbox().addText("Reckoner Brute's Guard has worn off!");
            map.getTextbox().setIsActive(true);
        }
    }

    // Move Set integrated with CombatResolver

    public void moveReckoningStrike(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.35f);
    }

    public void moveCrushingSlam(Player player) {
        if (player != null) {
            player.alterDEFModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.10f);
    }

    public void moveAlphaRoar(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.40f);
    }

    public void moveTremorOfReckoning(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.70f);
    }

    public void executeTurn(Player player) {
        advanceTurn();
        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveReckoningStrike(player); break;
            case 1: moveCrushingSlam(player); break;
            case 2: moveAlphaRoar(player); break;
            default: moveTremorOfReckoning(player); break;
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public boolean isGuardActive() {
        return guardActive;
    }
}