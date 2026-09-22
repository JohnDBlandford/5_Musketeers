package Maps;

import java.util.ArrayList;

import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.PortalScript;
import Tilesets.CommonTileset;
import Utils.Point;

// Placeholder Map for testing purposes. This map is empty except for a single tile at the top left corner, which is where the player will spawn.

public class BlankMap extends Map {
    public BlankMap() {
        super("blank_map.txt", new CommonTileset());
        this.playerStartPosition = new Point(48, 48);
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        Portal portal = new Portal(20, 20, "test", 18 * 48, 20 * 48, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        return enhancedMapTiles;
    }

}
