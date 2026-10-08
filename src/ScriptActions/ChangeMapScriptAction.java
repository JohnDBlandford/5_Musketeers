package ScriptActions;

import Level.ScriptState;

// A script action that changes the current map to a new map, and moves the player to a new position in that map

public class ChangeMapScriptAction extends ScriptAction {
    private final String mapName;
    private final float playerX;
    private final float playerY;

    public ChangeMapScriptAction(String mapName, float playerX, float playerY) {
        this.mapName = mapName;
        this.playerX = playerX;
        this.playerY = playerY;
    }

    @Override
    public ScriptState execute() {
        map.requestMapChange(mapName, playerX, playerY);
        return ScriptState.COMPLETED;
    }
}
