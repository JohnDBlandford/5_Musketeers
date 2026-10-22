package Maps;

import java.util.ArrayList;

import GameObject.Door;
import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.DoorScript;
import Scripts.PortalScript;
import Tilesets.World1Tileset;

public class World1Map extends Map {
    public World1Map() {
        super("world1_map.txt", new World1Tileset());
        this.playerStartPosition = getMapTile(81, 90).getLocation(); // Brackenford plaza

    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        // The portal is placed at the location of the tile at (60, 90) in the world1
        // map
        // and teleports to (48, 48) in the map
        Portal world2portal = new Portal(getMapTile(60, 90).getX(), getMapTile(60, 90).getY(),
                "world2_map", 3360, 2340, 48, 48);
        world2portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(world2portal);

        Door hallDoor = new Door(getMapTile(82, 82).getX(), getMapTile(82, 82).getY(),
                "guild_hall_map", 408, 624, 48, 48);
        hallDoor.setInteractScript(new DoorScript());
        enhancedMapTiles.add(hallDoor);

        return enhancedMapTiles;

    }

}