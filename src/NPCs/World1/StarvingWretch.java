package NPCs.World1;

import Engine.CombatResolver;
import Level.MapEntityStatus;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Utils.Point;

import java.util.Random;

public class StarvingWretch extends Enemy implements BattleEnemy {

    private int enemyLevel;
    private int fleeTurns = 0;
    private boolean hasFled = false;
    private boolean inBattle = false;   // in the battle screen, fleeing is decided on the enemy's turn only
    private final Random random = new Random();

    // Starter Baseline Stats (Level 1)
    private static final float BASE_HP = 70f;
    private static final float BASE_ATK = 18f;
    private static final float BASE_DEF = 10f;
    private static final float BASE_SPD = 22f;
    private static final int BASE_XP = 25;
    private static final int BASE_GOLD = 12;

    public StarvingWretch(int id, Point location) {
        this(id, location, 1 + (int)(Math.random() * 3));
    }

    public StarvingWretch(int id, Point location, int level) {
        super(id, location, "Starving Wretch",
                BASE_HP + ((level - 1) * 10f),
                BASE_ATK + ((level - 1) * 3f),
                BASE_DEF + ((level - 1) * 2f),
                BASE_SPD + ((level - 1) * 2f),
                Math.round(BASE_XP * (1f + ((level - 1) * 0.2f))),
                Math.round(BASE_GOLD * (1f + ((level - 1) * 0.2f))),
                35.0f);

        this.enemyLevel = Math.min(3, Math.max(1, level));
    }

    // Starving Wretch Moveset integrated with CombatResolver

    public void moveDesperationScratch(Player player) {
        CombatResolver.resolveEnemyAttack(this, player, 0.85f);
    }

    public void moveBiteOfEnvy(Player player) {
        // 80% accuracy/hit chance
        if (random.nextFloat() < 0.80f) {
            CombatResolver.resolveEnemyAttack(this, player, 1.30f);
        } else {
            if (map != null && map.getTextbox() != null) {
                map.getTextbox().addText("Starving Wretch's Bite of Envy missed!");
                map.getTextbox().setIsActive(true);
            }
        }
    }

    public void moveMudFling(Player player) {
        if (player != null) {
            player.alterSPDModifier(-0.05f);
            player.alterDEFModifier(-0.10f);
        }
        CombatResolver.resolveEnemyAttack(this, player, 0.50f);
    }

    public void moveScrappyFlail(Player player) {
        int hits = 2 + random.nextInt(2); // 2 to 3 hits
        for (int i = 0; i < hits; i++) {
            if (player != null && player.getCurrentHP() > 0) {
                CombatResolver.resolveEnemyAttack(this, player, 0.35f);
            }
        }
    }

    public void executeTurn(Player player) {
        // Evaluate flee condition if low health (< 25% HP)
        if (currentHP < (maxHP * 0.25f) && !hasFled) {
            if (evaluateFlee()) {
                return; // Turn ends if successfully fled
            }
        }

        int choice = random.nextInt(4);
        switch (choice) {
            case 0: moveDesperationScratch(player); break;
            case 1: moveBiteOfEnvy(player); break;
            case 2: moveMudFling(player); break;
            default: moveScrappyFlail(player); break;
        }
    }

    @Override
    protected void onTakeDamage(float damageAmount) {
        if (inBattle) return;   // in battle the flee check happens on its turn (chooseBattleMove)
        if (currentHP < (maxHP * 0.25f) && !hasFled) {
            evaluateFlee();
        }
    }

    private boolean evaluateFlee() {
        fleeTurns++;
        float fleeChance = Math.min(100.0f, fleeTurns * 20.0f);
        float roll = random.nextFloat() * 100.0f;

        if (roll < fleeChance) {
            hasFled = true;

            if (map != null && map.getTextbox() != null) {
                map.getTextbox().addText("Starving Wretch has run away!");
                map.getTextbox().setIsActive(true);
            }

            this.setMapEntityStatus(MapEntityStatus.INACTIVE);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- BATTLE hooks (used by Screens.Battle.PlayBattleScreen)
    // Same moves, multipliers and passives as executeTurn(), but the battle does the damage and shows the text.
    // Stat changes use the exact amounts above; "-" effects on the knight happen only if the move lands.

    @Override
    public void onBattleStart() {
        fleeTurns = 0;
        hasFled = false;
        inBattle = true;
    }

    @Override
    public int battleLevel() {
        return enemyLevel;
    }

    @Override
    public Move chooseBattleMove(Random rng, Player player) {
        // when badly hurt (< 25% HP) it may run away, more likely every turn
        if (currentHP < (maxHP * 0.25f) && !hasFled) {
            if (evaluateFlee()) return Move.flee("Starving Wretch");
        }
        switch (rng.nextInt(4)) {
            case 0:  return Move.attack("Desperation Scratch", 0.85f);
            case 1:  return Move.attack("Bite of Envy", 1.30f).accuracy(0.80f);
            case 2:  return Move.attack("Mud Fling", 0.50f).lowersPlayer(Move.SPD, 0.05f).lowersPlayer(Move.DEF, 0.10f);
            default: return Move.attack("Scrappy Flail", 0.35f).hits(2 + rng.nextInt(2));
        }
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }
}