package Maps;

import java.util.ArrayList;

import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.PortalScript;
import Tilesets.World1Tileset;
import Utils.Point;

public class World1Map extends Map {
    public World1Map() {
        super("world1_map.txt", new World1Tileset());
        this.playerStartPosition = getMapTile(36, 45).getLocation();
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();
        // Add a portal to the blank map at (18, 20) that teleports the player to (48,
        // 48) in the blank map
        Portal portal = new Portal(getMapTile(35, 40).getX(), getMapTile(35, 40).getY(),
                "blank", 48, 48, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        return enhancedMapTiles;
    }

}