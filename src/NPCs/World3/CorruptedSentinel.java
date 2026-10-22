package NPCs.World3;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class CorruptedSentinel extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 10 Baseline per Chapter 3)
    private static final float BASE_HP = 320f;
    private static final float BASE_ATK = 65f;
    private static final float BASE_DEF = 45f;
    private static final float BASE_SPD = 32f;
    private static final int BASE_XP = 160;
    private static final int BASE_GOLD = 110;

    public CorruptedSentinel(int id, Point location) {
        this(id, location, 10 + (int)(Math.random() * 4));
    }

    public CorruptedSentinel(int id, Point location, int level) {
        super(id, location, "Corrupted Sentinel",
                BASE_HP + ((level - 10) * 28f),
                BASE_ATK + ((level - 10) * 6f),
                BASE_DEF + ((level - 10) * 4f),
                BASE_SPD + ((level - 10) * 2.5f),
                Math.round(BASE_XP * (1f + ((level - 10) * 0.15f))),
                Math.round(BASE_GOLD * (1f + ((level - 10) * 0.15f))),
                35.0f); // 35% Spawn Rate

        this.enemyLevel = Math.min(13, Math.max(10, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    // --- Moveset ---

    public void moveObsidianBlade(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 1.10f);
    }

    public void moveSentinelSlam(Player player) {
        if (player != null) {
            player.alterDEFModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.25f);
    }

    public void moveWatcherGaze(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.60f);
    }

    public void moveAegisPulse(Player player) {
        this.alterDEFModifier(0.15f);
        CombatResolver.resolveEnemyAttack(this, player, 0.80f);
    }

    public void executeTurn(Player player) {
        advanceTurn();

        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveObsidianBlade(player); break;
            case 1: moveSentinelSlam(player); break;
            case 2: moveWatcherGaze(player); break;
            default: moveAegisPulse(player); break;
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
            case 0:  return Move.attack("Obsidian Blade", 1.10f);
            case 1:  return Move.attack("Sentinel Slam", 1.25f).lowersPlayer(Move.DEF, 0.10f);
            case 2:  return Move.attack("Watcher Gaze", 0.60f).lowersPlayer(Move.SPD, 0.15f);
            default: return Move.attack("Aegis Pulse", 0.80f).raisesSelf(Move.DEF, 0.15f);
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}