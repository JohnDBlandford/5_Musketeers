package NPCs.World4;

import NPCs.World1.TheFamineWarden;
import Utils.Point;

// WORLD 4 (Dabbah's Hollow): World 1's mini-boss, back as a regular enemy.
// It IS a TheFamineWarden (same moves, passives and battle hooks, so any change to TheFamineWarden carries over); only the name and the
// spawn rate differ. Its level (50-70) and stats come from Balance, through EnemyFactory, like every other enemy.
public class RisenFamineWarden extends TheFamineWarden {

    public RisenFamineWarden(int id, Point location) {
        super(id, location);
        this.enemyName = "Risen Famine Warden";
        this.spawnRate = 20f; // share of World 4's random encounters
    }
}