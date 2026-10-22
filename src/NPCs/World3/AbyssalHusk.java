package NPCs.World3;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class AbyssalHusk extends Enemy implements BattleEnemy {

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
        switch (rng.nextInt(4)) {
            case 0:  return Move.attack("Frenzied Claws", 0.60f).hits(2);
            case 1:  return Move.attack("Abyssal Bite", 1.30f);
            case 2:  return Move.attack("Void Screech", 0.40f).lowersPlayer(Move.DEF, 0.15f);
            default: return Move.attack("Reckless Lunge", 1.50f).lowersSelf(Move.DEF, 0.10f);   // high risk
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}