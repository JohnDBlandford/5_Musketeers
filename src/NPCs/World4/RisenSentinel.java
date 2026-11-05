package NPCs.World4;

import NPCs.World3.CorruptedSentinel;
import Utils.Point;

// WORLD 4 (Dabbah's Hollow): World 3's tank.
// It IS a CorruptedSentinel (same moves, passives and battle hooks, so any change to CorruptedSentinel carries over); only the name and the
// spawn rate differ. Its level (50-70) and stats come from Balance, through EnemyFactory, like every other enemy.
public class RisenSentinel extends CorruptedSentinel {

    public RisenSentinel(int id, Point location) {
        super(id, location);
        this.enemyName = "Risen Sentinel";
        this.spawnRate = 13f; // share of World 4's random encounters
    }
}