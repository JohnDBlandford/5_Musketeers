package NPCs.World2;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class BaronsVanguard extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 10 Baseline per Chapter 3)
    private static final float BASE_HP = 420f;
    private static final float BASE_ATK = 72f;
    private static final float BASE_DEF = 50f;
    private static final float BASE_SPD = 44f;
    private static final int BASE_XP = 220;
    private static final int BASE_GOLD = 150;

    public BaronsVanguard(int id, Point location) {
        this(id, location, 10 + (int)(Math.random() * 3));
    }

    public BaronsVanguard(int id, Point location, int level) {
        super(id, location, "Baron's Vanguard",
                BASE_HP + ((level - 10) * 35f),
                BASE_ATK + ((level - 10) * 7f),
                BASE_DEF + ((level - 10) * 4.5f),
                BASE_SPD + ((level - 10) * 3f),
                Math.round(BASE_XP * (1f + ((level - 10) * 0.20f))),
                Math.round(BASE_GOLD * (1f + ((level - 10) * 0.20f))),
                15.0f); // 15% Spawn Rate

        this.enemyLevel = Math.min(12, Math.max(10, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    // --- Moveset ---

    public void moveVanguardStrike(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.25f);
    }

    public void moveDualFlurry(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 0.90f);
        if (player != null && player.getCurrentHP() > 0) {
            CombatResolver.resolveEnemyAttack(this, player, 0.90f);
        }
    }

    public void moveVanguardShieldBracing(Player player) {
        this.alterDEFModifier(0.20f);
        CombatResolver.resolveEnemyAttack(this, player, 0.70f);
    }

    public void moveDisarmingLunge(Player player) {
        if (player != null) {
            player.alterATKModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.0f);
    }

    public void executeTurn(Player player) {
        advanceTurn();

        // Elite Passive Mechanic: Always uses Dual Flurry when below 50% HP
        if ((this.currentHP / this.maxHP) < 0.50f) {
            moveDualFlurry(player);
            return;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveVanguardStrike(player); break;
            case 1: moveVanguardShieldBracing(player); break;
            default: moveDisarmingLunge(player); break;
        }
    }

    // ---------------------------------------------------------------- BATTLE hooks (used by Screens.Battle.PlayBattleScreen)
    // Same moves, multipliers and passives as executeTurn(), but the battle does the damage and shows the text.
    // Stat changes use the exact amounts above; "-" effects on the knight happen only if the move lands.

    @Override
    public void onBattleStart() {
        turnCount = 0;
    }

    @Override
    public int battleLevel() {
        return enemyLevel;
    }

    @Override
    public Move chooseBattleMove(Random rng, Player player) {
        advanceTurn();
        // Elite passive: always uses Dual Flurry below 50% HP
        if ((currentHP / maxHP) < 0.50f) return Move.attack("Dual Flurry", 0.90f).hits(2);
        switch (rng.nextInt(3)) {
            case 0:  return Move.attack("Vanguard Strike", 1.25f);
            case 1:  return Move.attack("Shield Bracing", 0.70f).raisesSelf(Move.DEF, 0.20f);
            default: return Move.attack("Disarming Lunge", 1.0f).lowersPlayer(Move.ATK, 0.15f);
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}