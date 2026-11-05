package NPCs.World4;

import NPCs.World1.ReckonerBrute;
import Utils.Point;

// WORLD 4 (Dabbah's Hollow): World 1's tank.
// It IS a ReckonerBrute (same moves, passives and battle hooks, so any change to ReckonerBrute carries over); only the name and the
// spawn rate differ. Its level (50-70) and stats come from Balance, through EnemyFactory, like every other enemy.
public class RisenReckonerBrute extends ReckonerBrute {

    public RisenReckonerBrute(int id, Point location) {
        super(id, location);
        this.enemyName = "Risen Reckoner Brute";
        this.spawnRate = 14f; // share of World 4's random encounters
    }
}