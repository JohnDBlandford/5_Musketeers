package Maps;

import java.util.ArrayList;

import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
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
        Portal portal = new Portal(getMapTile(60, 90).getX(), getMapTile(60, 90).getY(),
                "world2_map", 3360, 2340, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        return enhancedMapTiles;
    }

}