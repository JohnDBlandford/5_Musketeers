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

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();
        Door hallDoor = new Door(getMapTile(9, 14).getX(), getMapTile(9, 14).getY(),
                "world1", 3888, 3936, 96, 48);
        hallDoor.setInteractScript(new DoorScript());
        enhancedMapTiles.add(hallDoor);
        return enhancedMapTiles;
    }
}
