package Level;

public interface GameListener {
    void onWin();

    // Called when the player changes maps. The mapName is the name of the new map,
    // and playerX and playerY are the player's new coordinates in that map.

    default void onMapChange(String mapName, float playerX, float playerY) {
    }
}
