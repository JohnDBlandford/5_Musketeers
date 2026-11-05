package NPCs.World4;

import NPCs.World2.MilitiaBrute;
import Utils.Point;

// WORLD 4 (Dabbah's Hollow): World 2's tank.
// It IS a MilitiaBrute (same moves, passives and battle hooks, so any change to MilitiaBrute carries over); only the name and the
// spawn rate differ. Its level (50-70) and stats come from Balance, through EnemyFactory, like every other enemy.
public class RisenMilitiaBrute extends MilitiaBrute {

    public RisenMilitiaBrute(int id, Point location) {
        super(id, location);
        this.enemyName = "Risen Militia Brute";
        this.spawnRate = 13f; // share of World 4's random encounters
    }
}