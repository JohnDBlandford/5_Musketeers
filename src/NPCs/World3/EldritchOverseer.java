package NPCs.World3;

import Engine.CombatResolver;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class EldritchOverseer extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int turnCount = 0;
    private static final Random random = new Random();

    // Baseline Stats (Level 15 Baseline per Chapter 3)
    private static final float BASE_HP = 550f;
    private static final float BASE_ATK = 105f;
    private static final float BASE_DEF = 62f;
    private static final float BASE_SPD = 50f;
    private static final int BASE_XP = 380;
    private static final int BASE_GOLD = 260;

    public EldritchOverseer(int id, Point location) {
        this(id, location, 15 + (int)(Math.random() * 4));
    }

    public EldritchOverseer(int id, Point location, int level) {
        super(id, location, "Eldritch Overseer",
                BASE_HP + ((level - 15) * 45f),
                BASE_ATK + ((level - 15) * 9f),
                BASE_DEF + ((level - 15) * 5f),
                BASE_SPD + ((level - 15) * 3.5f),
                Math.round(BASE_XP * (1f + ((level - 15) * 0.20f))),
                Math.round(BASE_GOLD * (1f + ((level - 15) * 0.20f))),
                15.0f); // 15% Spawn Rate

        this.enemyLevel = Math.min(18, Math.max(15, level));
    }

    public void advanceTurn() {
        turnCount++;
    }

    // --- Moveset ---

    public void moveCosmicCrush(Player player) {
        if (player != null) {
            player.alterDEFModifier(-0.15f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.35f);
    }

    public void moveOverseerCommand(Player player) {
        this.alterATKModifier(0.15f);
        CombatResolver.resolveEnemyAttack(this, player, 0.90f);
    }

    public void moveGraspOfTheVoid(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.25f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 1.10f);
    }

    public void moveEldritchPulse(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 0.80f);
        if (player != null && player.getCurrentHP() > 0) {
            CombatResolver.resolveEnemyAttack(this, player, 0.80f);
        }
    }

    public void executeTurn(Player player) {
        advanceTurn();

        // Elite Passive Mechanic: Empowers standard attacks below 50% HP
        if ((this.currentHP / this.maxHP) < 0.50f && random.nextBoolean()) {
            moveEldritchPulse(player);
            return;
        }

        int choice = random.nextInt(3);
        switch (choice) {
            case 0: moveCosmicCrush(player); break;
            case 1: moveOverseerCommand(player); break;
            default: moveGraspOfTheVoid(player); break;
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
        // Elite passive: below 50% HP it may use Eldritch Pulse
        if ((currentHP / maxHP) < 0.50f && rng.nextBoolean()) return Move.attack("Eldritch Pulse", 0.80f).hits(2);
        switch (rng.nextInt(3)) {
            case 0:  return Move.attack("Cosmic Crush", 1.35f).lowersPlayer(Move.DEF, 0.15f);
            case 1:  return Move.attack("Overseer Command", 0.90f).raisesSelf(Move.ATK, 0.15f);
            default: return Move.attack("Grasp of the Void", 1.10f).lowersPlayer(Move.SPD, 0.25f);
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getTurnCount() {
        return turnCount;
    }
}