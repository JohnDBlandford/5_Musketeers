package Maps;

import java.util.ArrayList;

import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.PortalScript;
import Tilesets.World3Tileset;

// World 3: The Age of Watching (884-908 CE). You arrive in the middle of the Harvest Fair.
public class World3Map extends Map {
    public World3Map() {
        super("world3_map.txt", new World3Tileset());
        this.playerStartPosition = getMapTile(66, 66).getLocation(); // Harvest Fair
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        // back to The Fracture in World 2 (lands just north of the ember grate)
        Portal portal = new Portal(getMapTile(78, 70).getX(), getMapTile(78, 70).getY(),
                "world2_map", 68 * 48, 106 * 48, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        return enhancedMapTiles;
    }

}
