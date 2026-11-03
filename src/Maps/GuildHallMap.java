package Maps;

import java.util.ArrayList;

import GameObject.Door;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.DoorScript;

import Tilesets.*;
import Utils.Point;

public class GuildHallMap extends Map {

    public GuildHallMap() {
        super("guild_hall_map.txt", new TavernTileset());
        this.playerStartPosition = new Point(0, 0); //

    }

    // where the front door drops the player. These are methods (not fields) because
    // loadEnhancedMapTiles runs inside the Map constructor, before fields are set.
    protected String exitMap() {
        return "world1";
    }

    protected float exitX() {
        return 76 * 48 + 24;
    }

    protected float exitY() {
        return 80 * 48;
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();
        Door hallDoor = new Door(getMapTile(9, 14).getX(), getMapTile(9, 14).getY(),
                exitMap(), exitX(), exitY(), 96, 48);
        hallDoor.setInteractScript(new DoorScript());
        enhancedMapTiles.add(hallDoor);
        return enhancedMapTiles;
    }
}
