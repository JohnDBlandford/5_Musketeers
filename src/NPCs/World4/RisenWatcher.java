package NPCs.World4;

import NPCs.World3.TheWatcherInTheAsh;
import Utils.Point;

// WORLD 4 (Dabbah's Hollow): World 3's mini-boss, back as a regular enemy.
// It IS a TheWatcherInTheAsh (same moves, passives and battle hooks, so any change to TheWatcherInTheAsh carries over); only the name and the
// spawn rate differ. Its level (50-70) and stats come from Balance, through EnemyFactory, like every other enemy.
public class RisenWatcher extends TheWatcherInTheAsh {

    public RisenWatcher(int id, Point location) {
        super(id, location);
        this.enemyName = "Risen Watcher";
        this.spawnRate = 20f; // share of World 4's random encounters
    }
}