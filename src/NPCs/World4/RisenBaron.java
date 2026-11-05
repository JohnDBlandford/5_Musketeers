package NPCs.World4;

import NPCs.World2.BaronOfTheClearedWood;
import Utils.Point;

// WORLD 4 (Dabbah's Hollow): World 2's mini-boss, back as a regular enemy.
// It IS a BaronOfTheClearedWood (same moves, passives and battle hooks, so any change to BaronOfTheClearedWood carries over); only the name and the
// spawn rate differ. Its level (50-70) and stats come from Balance, through EnemyFactory, like every other enemy.
public class RisenBaron extends BaronOfTheClearedWood {

    public RisenBaron(int id, Point location) {
        super(id, location);
        this.enemyName = "Risen Baron";
        this.spawnRate = 20f; // share of World 4's random encounters
    }
}