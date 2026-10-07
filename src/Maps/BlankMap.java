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
        // Add a portal to World 1 near the center of the blank map.
        // The portal is placed at the location of the tile at (48, 48) in the blank map
        // and teleports to (60, 90) in the world1 map
        Portal portal = new Portal(48, 48, "world1", 60 * 48, 90 * 48, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        return enhancedMapTiles;
    }

}
